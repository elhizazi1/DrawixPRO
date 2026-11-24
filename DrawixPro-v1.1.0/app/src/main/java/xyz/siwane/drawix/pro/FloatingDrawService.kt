package xyz.siwane.drawix.pro

import xyz.siwane.drawix.pro.overlay.FloatingUIWrapper
import xyz.siwane.drawix.pro.capture.CaptureCoordinator
import xyz.siwane.drawix.pro.engine.ServiceController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Outline
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Choreographer
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import xyz.siwane.drawix.pro.databinding.LayoutCloseTargetBinding
import xyz.siwane.drawix.pro.databinding.LayoutFloatingButtonBinding
import xyz.siwane.drawix.pro.databinding.LayoutTextInputBinding
import xyz.siwane.drawix.pro.databinding.LayoutToolsPanelBinding
import kotlin.math.abs
import kotlin.math.hypot
import java.util.Locale

// ===================================================================
// FloatingUIWrapper
// ===================================================================
// ===================================================================
// FloatingDrawService
// ===================================================================
class FloatingDrawService : Service() {

    companion object {
        @Volatile var isRunning = false
        var instance: FloatingDrawService? = null
        private const val CHANNEL_ID = "DrawingServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var windowManager: WindowManager
    private lateinit var drawingSurface: DrawView

    private lateinit var buttonBinding: LayoutFloatingButtonBinding
    private lateinit var panelBinding: LayoutToolsPanelBinding
    private lateinit var closeTargetBinding: LayoutCloseTargetBinding
    private var textInputBinding: LayoutTextInputBinding? = null

    private lateinit var buttonWrapper: FloatingUIWrapper
    private lateinit var panelWrapper: FloatingUIWrapper
    private var buttonPositioner: xyz.siwane.drawix.pro.overlay.FloatingButtonPositioner? = null
    
    private lateinit var colorPopupWrapper: FrameLayout
    private lateinit var colorPopupParams: WindowManager.LayoutParams

    private lateinit var drawParams: WindowManager.LayoutParams
    private lateinit var buttonParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams
    private lateinit var closeTargetParams: WindowManager.LayoutParams

    private var screenWidth = 0
    private var screenHeight = 0
    private var layoutFlag = 0
    
    private var isCometMode = true

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var destroyed = false
    private var isCapturing = false
    private lateinit var locContext: Context
    private val appearancePreferences by lazy { xyz.siwane.drawix.pro.data.DrawPreferences(this) }
    private val overlayStyler by lazy { xyz.siwane.drawix.pro.overlay.OverlayStyler(this, appearancePreferences) }
    private var refreshToolColors: ((Int) -> Unit)? = null
    private var systemDark = false
    private var layoutDensity = 0f
    private var layoutFontScale = 0f

    private fun refreshOverlayAppearance() {
        if (destroyed || !::buttonBinding.isInitialized || !::panelBinding.isInitialized) return
        val foreground = overlayStyler.apply(buttonBinding, panelBinding, textInputBinding, systemDark)
        refreshToolColors?.invoke(foreground)
    }

    @Suppress("DEPRECATION")
    private val baseWindowFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN or
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION

    private val pendingUpdates = mutableSetOf<View>()
    private var isUpdatePending = false
    private val updateFrameCallback = Choreographer.FrameCallback {
        isUpdatePending = false
        pendingUpdates.forEach { view ->
            try {
                if (view.isAttachedToWindow) {
                    val params = when (view) {
                        buttonWrapper -> buttonParams
                        panelWrapper -> panelParams
                        drawingSurface -> drawParams
                        colorPopupWrapper -> colorPopupParams
                        else -> null
                    }
                    if (params != null) windowManager.updateViewLayout(view, params)
                }
            } catch (e: Exception) {}
        }
        pendingUpdates.clear()
    }

    private fun requestSmoothUpdate(view: View) {
        pendingUpdates.add(view)
        if (!isUpdatePending) {
            isUpdatePending = true
            Choreographer.getInstance().postFrameCallback(updateFrameCallback)
        }
    }

    private fun getLocalizedContext(): Context = xyz.siwane.drawix.pro.data.AppLocale.wrap(this)

    private fun setCutoutMode(params: WindowManager.LayoutParams) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        return START_NOT_STICKY
    }

    private fun getNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, locContext.getString(R.string.service_title), NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(locContext.getString(R.string.notification_title))
            .setContentText(locContext.getString(R.string.notification_desc))
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, locContext.getString(R.string.tile_active_label),
                PendingIntent.getService(this, 1, Intent(this, FloatingDrawService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE))
            .setSmallIcon(R.drawable.ic_brush_2)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    fun syncLaserModeUI() {
        if (!::panelBinding.isInitialized) return
        val prefs = getSharedPreferences("DrawSettings", Context.MODE_PRIVATE)
        isCometMode = prefs.getBoolean("laser_comet_mode", true)
        val activeSystemColor = Color.parseColor("#00B693")
        if (isCometMode) {
            panelBinding.toolLaserMode.setColorFilter(activeSystemColor, PorterDuff.Mode.SRC_IN)
        } else {
            panelBinding.toolLaserMode.setColorFilter(Color.parseColor("#999999"), PorterDuff.Mode.SRC_IN)
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()
        instance = this
        locContext = getLocalizedContext()
        layoutDensity = resources.displayMetrics.density
        layoutFontScale = resources.configuration.fontScale
        systemDark = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES

        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, getNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(NOTIFICATION_ID, getNotification())
        } catch (e: Exception) {
            android.util.Log.e("DrawixOverlay", "Foreground start failed", e)
            stopSelf()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, locContext.getString(R.string.toast_overlay_permission), Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }


        try {
        val accessibilityContext = ScreenshotAccessibilityService.instance
        val wmContext = accessibilityContext ?: this
        windowManager = wmContext.getSystemService(WINDOW_SERVICE) as WindowManager
        val themeContext = xyz.siwane.drawix.pro.overlay.OverlayAppearance.context(this)
        val inflater = LayoutInflater.from(themeContext)

        updateScreenDimensions()

        layoutFlag = if (accessibilityContext != null) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        drawingSurface = DrawView(themeContext)

        drawParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            baseWindowFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSPARENT 
        )
        setCutoutMode(drawParams)
        windowManager.addView(drawingSurface, drawParams)

        drawingSurface.onTextInputRequested = { x, y -> showTextInputDialog(x, y) }
        drawingSurface.onCanvasTouched = {
            if (textInputBinding != null) { cancelTextInput(); true } else false
        }

        panelBinding = LayoutToolsPanelBinding.inflate(inflater)
        panelBinding.root.layoutDirection = View.LAYOUT_DIRECTION_LTR

        (panelBinding.colorsContainer.parent as? android.view.ViewGroup)?.apply {
            removeView(panelBinding.colorsContainer)
            removeView(panelBinding.customPickerContainer)
        }

        colorPopupWrapper = FrameLayout(themeContext).apply {
            clipChildren = false
            clipToPadding = false
            addView(panelBinding.colorsContainer)
            addView(panelBinding.customPickerContainer)
        }

        colorPopupParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag, baseWindowFlags, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
        }
        setCutoutMode(colorPopupParams)
        colorPopupWrapper.visibility = View.GONE
        windowManager.addView(colorPopupWrapper, colorPopupParams)

        panelWrapper = FloatingUIWrapper(themeContext, panelBinding.root, isCenterPivot = true)
        
        panelWrapper.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val actualPanelWidth = panelWrapper.measuredWidth
        val actualPanelHeight = panelWrapper.measuredHeight

        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag, baseWindowFlags, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT 
            x = maxOf(0, (screenWidth - actualPanelWidth) / 2)
            y = maxOf(0, screenHeight - actualPanelHeight - 120) 
        }
        setCutoutMode(panelParams)
        panelWrapper.visibility = View.GONE
        windowManager.addView(panelWrapper, panelParams)

        closeTargetBinding = LayoutCloseTargetBinding.inflate(inflater)
        closeTargetParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag, baseWindowFlags, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (64 * resources.displayMetrics.density).toInt()
        }
        setCutoutMode(closeTargetParams)
        closeTargetBinding.root.visibility = View.GONE
        windowManager.addView(closeTargetBinding.root, closeTargetParams)

        buttonBinding = LayoutFloatingButtonBinding.inflate(inflater)
        buttonBinding.root.elevation = 0f
        buttonBinding.btnMainFab.elevation = 0f
        buttonBinding.btnMainFab.translationZ = 0f
        buttonBinding.btnMainFab.stateListAnimator = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            buttonBinding.btnMainFab.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            buttonBinding.btnMainFab.clipToOutline = true
        }

        buttonWrapper = FloatingUIWrapper(themeContext, buttonBinding.root).apply { minimumTouchSizePx = (48 * resources.displayMetrics.density).toInt() }
        buttonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag, baseWindowFlags, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.RIGHT
            x = (6 * resources.displayMetrics.density).toInt()
            y = (screenHeight * .35f).toInt()
        }
        setCutoutMode(buttonParams)
        windowManager.addView(buttonWrapper, buttonParams)
        buttonPositioner = xyz.siwane.drawix.pro.overlay.FloatingButtonPositioner(this, buttonWrapper, buttonParams,
            { screenWidth to screenHeight }, { requestSmoothUpdate(buttonWrapper) })
        buttonWrapper.post { if (!destroyed) buttonPositioner?.restore() }

        val prefs = getSharedPreferences("DrawSettings", Context.MODE_PRIVATE)
        applyButtonScale(prefs.getFloat("button_scale", 1.0f))
        applyPanelScale(prefs.getFloat("panel_scale", 1.0f))
        applyButtonOpacity(prefs.getFloat("btn_alpha_value", 1.0f))

        setupActions(themeContext)
        serviceScope.launch {
            appearancePreferences.changes.conflate().collect { refreshOverlayAppearance() }
        }
        isRunning = true
        ServiceController.publish(true)
        SmartNotificationHelper.cancelRetentionReminder(this)
        } catch (e: Exception) {
            android.util.Log.e("DrawixOverlay", "Overlay initialization failed", e)
            Toast.makeText(this, locContext.getString(R.string.toast_overlay_permission), Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun showColorPopup(viewToShow: View, viewToHide: View) {
        viewToHide.visibility = View.GONE
        viewToShow.visibility = View.VISIBLE
        colorPopupWrapper.visibility = View.VISIBLE
        
        colorPopupWrapper.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val w = colorPopupWrapper.measuredWidth
        val h = colorPopupWrapper.measuredHeight
        
        colorPopupParams.x = panelParams.x + (panelWrapper.width / 2) - (w / 2)
        colorPopupParams.y = panelParams.y - h - -35
        
        if (colorPopupParams.y < 0) {
            colorPopupParams.y = panelParams.y + panelWrapper.height + -35
        }
        windowManager.updateViewLayout(colorPopupWrapper, colorPopupParams)

        viewToShow.pivotX = w / 2f
        viewToShow.pivotY = h / 2f
        viewToShow.scaleX = 0f
        viewToShow.scaleY = 0f
        viewToShow.alpha = 0f
        
        viewToShow.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(250)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
    }

    private fun hideColorPopup() {
        if (!::colorPopupWrapper.isInitialized || colorPopupWrapper.visibility == View.GONE) return
        val activeView = if (panelBinding.colorsContainer.visibility == View.VISIBLE) panelBinding.colorsContainer else panelBinding.customPickerContainer
        
        activeView.animate()
            .scaleX(0f).scaleY(0f).alpha(0f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                activeView.visibility = View.GONE
                colorPopupWrapper.visibility = View.GONE
            }
            .start()
    }

    fun applyButtonScale(scale: Float) {
        if (!::buttonWrapper.isInitialized) return
        buttonWrapper.post {
            if (destroyed || !buttonWrapper.isAttachedToWindow) return@post
            updateScreenDimensions()
            if (buttonWrapper.baseWidth <= 0 || buttonWrapper.baseHeight <= 0) {
                applyButtonScale(scale)
                return@post
            }
            val clampedScale = scale.coerceIn(0.5f, 1.5f)
            val baseW = buttonWrapper.baseWidth.toFloat()
            val baseH = buttonWrapper.baseHeight.toFloat()
            buttonParams.x = buttonParams.x.coerceIn(0, maxOf(0, screenWidth - baseW.toInt()))
            buttonParams.y = buttonParams.y.coerceIn(0, maxOf(0, screenHeight - baseH.toInt()))
            val maxScaleX = (screenWidth - buttonParams.x) / baseW
            val maxScaleY = (screenHeight - buttonParams.y) / baseH
            val safeScale = clampedScale.coerceAtMost(maxScaleX).coerceAtMost(maxScaleY)
            buttonWrapper.targetScale = safeScale
            requestSmoothUpdate(buttonWrapper)
        }
    }

    fun applyPanelScale(scale: Float) {
        if (!::panelWrapper.isInitialized) return
        panelWrapper.post {
            if (destroyed || !panelWrapper.isAttachedToWindow) return@post
            updateScreenDimensions()
            if (panelWrapper.baseWidth <= 0 || panelWrapper.baseHeight <= 0) {
                applyPanelScale(scale)
                return@post
            }
            val clampedScale = scale.coerceIn(0.5f, 1.5f)
            val baseW = panelWrapper.baseWidth.toFloat()
            val baseH = panelWrapper.baseHeight.toFloat()
            val maxScaleX = screenWidth / baseW
            val maxScaleY = screenHeight / baseH
            val safeScale = clampedScale.coerceAtMost(maxScaleX).coerceAtMost(maxScaleY)
            
            val oldW = panelWrapper.measuredWidth
            val oldH = panelWrapper.measuredHeight
            
            panelWrapper.targetScale = safeScale
            panelWrapper.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val newW = panelWrapper.measuredWidth
            val newH = panelWrapper.measuredHeight
            
            if (oldW > 0 && oldH > 0) {
                panelParams.x = (panelParams.x - (newW - oldW) / 2).coerceIn(0, maxOf(0, screenWidth - newW))
                panelParams.y = (panelParams.y - (newH - oldH)).coerceIn(0, maxOf(0, screenHeight - newH))
            }
            requestSmoothUpdate(panelWrapper)
        }
    }

    fun applyButtonOpacity(alpha: Float) {
        if (::buttonWrapper.isInitialized) buttonWrapper.alpha = alpha.coerceIn(.2f, 1f)
    }

    @Suppress("DEPRECATION")
    private fun updateScreenDimensions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
        } else {
            val display = windowManager.defaultDisplay
            val metrics = android.util.DisplayMetrics()
            display.getRealMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
        }
    }

    @Suppress("DEPRECATION")
    private fun triggerHapticFeedback(duration: Long = 40) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(duration)
        }
    }

    private fun cancelTextInput() {
        textInputBinding?.let {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.etInput.windowToken, 0)
            try { windowManager.removeView(it.root) } catch (e: Exception) {}
            textInputBinding = null
        }
    }

    @Suppress("DEPRECATION")
    private fun showTextInputDialog(x: Float, y: Float) {
        if (textInputBinding != null) return
        val accessibilityContext = ScreenshotAccessibilityService.instance
        val uiContext = accessibilityContext ?: this
        val themeContext = xyz.siwane.drawix.pro.overlay.OverlayAppearance.context(this)

        textInputBinding = LayoutTextInputBinding.inflate(LayoutInflater.from(themeContext))
        
        val textParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            this.x = x.toInt() - 50
            this.y = y.toInt() - 150
        }
        setCutoutMode(textParams)
        windowManager.addView(textInputBinding!!.root, textParams)
        textInputBinding!!.etInput.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(textInputBinding!!.etInput, InputMethodManager.SHOW_IMPLICIT)

        textInputBinding!!.btnCancelText.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        textInputBinding!!.btnConfirmText.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)

        textInputBinding!!.etInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                cancelTextInput(); true
            } else false
        }
        textInputBinding!!.btnCancelText.setOnClickListener { cancelTextInput() }
        refreshOverlayAppearance()
        textInputBinding!!.btnConfirmText.setOnClickListener {
            val text = textInputBinding!!.etInput.text.toString()
            if (text.isNotEmpty()) drawingSurface.addTextShape(text, x, y)
            cancelTextInput()
        }
    }

    private fun openPanelFromBottomEdge() {
        if (panelWrapper.visibility == View.VISIBLE) return

        updateScreenDimensions()
        val currentPanelScale = panelWrapper.targetScale
        val actualPanelWidth = panelWrapper.width 
        val actualPanelHeight = panelWrapper.height

        val destY = screenHeight - actualPanelHeight - 120
        val startY = screenHeight

        panelParams.x = maxOf(0, (screenWidth - actualPanelWidth) / 2) 
        panelParams.y = startY
        requestSmoothUpdate(panelWrapper) 
        
        panelWrapper.visibility = View.VISIBLE
        panelWrapper.childView.scaleX = currentPanelScale * 0.4f
        panelWrapper.childView.scaleY = currentPanelScale * 0.4f
        panelWrapper.childView.alpha = 0f

        val yAnimator = android.animation.ValueAnimator.ofInt(startY, destY)
        yAnimator.duration = 450
        yAnimator.interpolator = OvershootInterpolator(1.0f) 
        yAnimator.addUpdateListener {
            panelParams.y = it.animatedValue as Int
            requestSmoothUpdate(panelWrapper)
        }
        yAnimator.start()

        panelWrapper.childView.animate()
            .scaleX(currentPanelScale).scaleY(currentPanelScale).alpha(1f)
            .setDuration(400)
            .setInterpolator(OvershootInterpolator(1.0f))
            .start()

        drawParams.flags = if (drawingSurface.currentMode != DrawMode.NONE) {
            baseWindowFlags
        } else {
            baseWindowFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        requestSmoothUpdate(drawingSurface)
    }

    private fun updateButtonAnimationAnchor() {
        val location = IntArray(2)
        buttonWrapper.getLocationOnScreen(location)
        val (x, y) = xyz.siwane.drawix.pro.overlay.FloatingButtonPlacement.animationAnchor(
            location[0], location[1], buttonWrapper.width, buttonWrapper.height, screenWidth, screenHeight)
        buttonWrapper.setAnimationAnchor(x, y)
    }

    private val closePanelAction = {
        cancelTextInput()
        hideColorPopup()
        updateScreenDimensions()
        
        val currentPanelScale = panelWrapper.targetScale
        val endY = screenHeight

        val yAnimator = android.animation.ValueAnimator.ofInt(panelParams.y, endY)
        yAnimator.duration = 350
        yAnimator.interpolator = AccelerateDecelerateInterpolator()
        yAnimator.addUpdateListener {
            panelParams.y = it.animatedValue as Int
            requestSmoothUpdate(panelWrapper)
        }
        yAnimator.start()

        panelWrapper.childView.animate()
            .scaleX(currentPanelScale * 0.5f).scaleY(currentPanelScale * 0.5f).alpha(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                panelWrapper.visibility = View.GONE
                
                buttonWrapper.visibility = View.VISIBLE
                updateButtonAnimationAnchor()
                val currentBtnScale = buttonWrapper.targetScale
                buttonWrapper.childView.scaleX = 0f
                buttonWrapper.childView.scaleY = 0f
                buttonWrapper.childView.animate()
                    .scaleX(currentBtnScale).scaleY(currentBtnScale).alpha(1f)
                    .setDuration(350)
                    .setInterpolator(OvershootInterpolator(1.2f))
                    .start()
            }.start()

        drawParams.flags = baseWindowFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        requestSmoothUpdate(drawingSurface)
    }

    private fun setupActions(themeContext: Context) {
        var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f
        var isDragging = false; var isHoveringClose = false

        buttonWrapper.setOnClickListener {
            updateScreenDimensions()
            updateButtonAnimationAnchor()
            buttonWrapper.childView.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(150).withEndAction {
                buttonWrapper.visibility = View.GONE
                openPanelFromBottomEdge()
            }.start()
        }
        buttonWrapper.contentDescription = locContext.getString(R.string.service_title)
        val dragSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop
        buttonWrapper.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    updateScreenDimensions()
                    buttonPositioner?.cancel()
                    initialX = buttonParams.x; initialY = buttonParams.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    isDragging = false; isHoveringClose = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = abs(event.rawX - initialTouchX)
                    val dy = abs(event.rawY - initialTouchY)
                    if (dx > dragSlop || dy > dragSlop) {
                        isDragging = true
                        val maxX = maxOf(0, screenWidth - buttonWrapper.width)
                        val maxY = maxOf(0, screenHeight - buttonWrapper.height)
                        val newX = (initialX - (event.rawX - initialTouchX).toInt()).coerceIn(0, maxX)
                        val newY = (initialY + (event.rawY - initialTouchY).toInt()).coerceIn(0, maxY)

                        if (event.rawY > screenHeight * 0.6) {
                            closeTargetBinding.root.visibility = View.VISIBLE
                            val targetCenterY = screenHeight - closeTargetParams.y -
                                (if (closeTargetBinding.root.height > 0) closeTargetBinding.root.height / 2 else (32.5f * resources.displayMetrics.density).toInt())
                            val distance = hypot((event.rawX - screenWidth / 2f).toDouble(), (event.rawY - targetCenterY).toDouble())
                            if (distance < 56 * resources.displayMetrics.density) {
                                if (!isHoveringClose) triggerHapticFeedback(50)
                                isHoveringClose = true
                                closeTargetBinding.root.scaleX = 1.3f; closeTargetBinding.root.scaleY = 1.3f
                                buttonParams.x = maxOf(0, (screenWidth / 2) - (buttonWrapper.width / 2))
                                buttonParams.y = (targetCenterY - buttonWrapper.height / 2).coerceIn(0, maxOf(0, screenHeight - buttonWrapper.height))
                            } else {
                                isHoveringClose = false
                                closeTargetBinding.root.scaleX = 1.0f; closeTargetBinding.root.scaleY = 1.0f
                                buttonParams.x = newX; buttonParams.y = newY
                            }
                        } else {
                            isHoveringClose = false
                            closeTargetBinding.root.scaleX = 1f; closeTargetBinding.root.scaleY = 1f
                            closeTargetBinding.root.visibility = View.GONE
                            buttonParams.x = newX; buttonParams.y = newY
                        }
                        requestSmoothUpdate(buttonWrapper)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    closeTargetBinding.root.visibility = View.GONE
                    if (isDragging) {
                        if (isHoveringClose) stopSelf() else buttonPositioner?.dock()
                    } else buttonWrapper.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    isDragging = false; isHoveringClose = false
                    closeTargetBinding.root.visibility = View.GONE
                    closeTargetBinding.root.scaleX = 1f; closeTargetBinding.root.scaleY = 1f
                    true
                }
                else -> false
            }
        }

        var pInitialX = 0; var pInitialY = 0; var pInitialTouchX = 0f; var pInitialTouchY = 0f
        
        panelBinding.root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    updateScreenDimensions()
                    pInitialX = panelParams.x; pInitialY = panelParams.y
                    pInitialTouchX = event.rawX; pInitialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(event.rawX - pInitialTouchX) > 10 || abs(event.rawY - pInitialTouchY) > 10) {
                        hideColorPopup()
                        
                        val dx = (event.rawX - pInitialTouchX).toInt()
                        val dy = (event.rawY - pInitialTouchY).toInt()
                        val maxX = maxOf(0, screenWidth - panelWrapper.width)
                        val maxY = maxOf(0, screenHeight - panelWrapper.height)

                        panelParams.x = (pInitialX + dx).coerceIn(0, maxX)
                        panelParams.y = (pInitialY + dy).coerceIn(0, maxY)
                        requestSmoothUpdate(panelWrapper)
                    }
                    true
                }
                else -> false
            }
        }

        fun animateClick(view: View) {
            view.scaleX = 0.8f; view.scaleY = 0.8f
            view.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }

        val allIcons = listOf(
            panelBinding.toolBrush, panelBinding.toolLaser, panelBinding.toolRect, panelBinding.toolCircle,
            panelBinding.toolArrow, panelBinding.toolText, panelBinding.toolMove,
            panelBinding.toolColorPicker, panelBinding.toolEraser, panelBinding.toolUndo, panelBinding.toolClear,
            panelBinding.toolScreenshot
        )

        var baseIconColor = com.google.android.material.color.MaterialColors.getColor(themeContext, com.google.android.material.R.attr.colorOnSurface, Color.WHITE)
        val activeSystemColor = Color.parseColor("#00B693")
        var currentAppColor = Color.parseColor("#F44336")
        var currentActiveTool: ImageView? = null
        refreshToolColors = { foreground ->
            baseIconColor = foreground
            allIcons.forEach { it.setColorFilter(foreground, PorterDuff.Mode.SRC_IN) }
            currentActiveTool?.setColorFilter(
                if (drawingSurface.currentMode in listOf(DrawMode.BRUSH, DrawMode.RECT, DrawMode.CIRCLE, DrawMode.ARROW, DrawMode.TEXT)) currentAppColor else activeSystemColor,
                PorterDuff.Mode.SRC_IN)
            panelBinding.toolColorPicker.setColorFilter(currentAppColor, PorterDuff.Mode.SRC_IN)
            panelBinding.toolCensor.setColorFilter(if (drawingSurface.isCensorMode) activeSystemColor else foreground, PorterDuff.Mode.SRC_IN)
            syncLaserModeUI()
        }

        allIcons.forEach { (it as? ImageView)?.setColorFilter(baseIconColor, PorterDuff.Mode.SRC_IN) }
        panelBinding.toolColorPicker.setColorFilter(currentAppColor, PorterDuff.Mode.SRC_IN)
        panelBinding.toolClose.setColorFilter(Color.parseColor("#FF4444"), PorterDuff.Mode.SRC_IN)
        panelBinding.toolSettings.setColorFilter(baseIconColor, PorterDuff.Mode.SRC_IN)
        panelBinding.toolCensor.setColorFilter(if (drawingSurface.isCensorMode) activeSystemColor else baseIconColor, PorterDuff.Mode.SRC_IN)
        buttonBinding.btnMainFab.setColorFilter(com.google.android.material.color.MaterialColors.getColor(themeContext,
            com.google.android.material.R.attr.colorOnPrimary, Color.WHITE), PorterDuff.Mode.SRC_IN)
        (closeTargetBinding.root.getChildAt(0) as? ImageView)?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)

        val prefs = getSharedPreferences("DrawSettings", Context.MODE_PRIVATE)
        
        syncLaserModeUI()

        panelBinding.toolLaserMode.setOnClickListener {
            animateClick(it)
            isCometMode = !isCometMode
            prefs.edit().putBoolean("laser_comet_mode", isCometMode).apply()
            
            syncLaserModeUI()
            
            val msg = if (isCometMode) locContext.getString(R.string.toast_laser_comet) else locContext.getString(R.string.toast_laser_time)
            Toast.makeText(this@FloatingDrawService, msg, Toast.LENGTH_SHORT).show()
            triggerHapticFeedback(25)
        }

        fun setActiveTool(selected: ImageView, mode: DrawMode) {
            hideColorPopup()
            if (currentActiveTool == selected) {
                allIcons.forEach { (it as? ImageView)?.setColorFilter(baseIconColor, PorterDuff.Mode.SRC_IN) }
                drawingSurface.currentMode = DrawMode.NONE
                currentActiveTool = null
                cancelTextInput()
                panelBinding.toolColorPicker.setColorFilter(currentAppColor, PorterDuff.Mode.SRC_IN)
                drawParams.flags = baseWindowFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                requestSmoothUpdate(drawingSurface)
            } else {
                allIcons.forEach { (it as? ImageView)?.setColorFilter(baseIconColor, PorterDuff.Mode.SRC_IN) }
                val isColorableTool = mode in listOf(DrawMode.BRUSH, DrawMode.RECT, DrawMode.CIRCLE, DrawMode.ARROW, DrawMode.TEXT)
                selected.setColorFilter(if (isColorableTool) currentAppColor else activeSystemColor, PorterDuff.Mode.SRC_IN)
                panelBinding.toolColorPicker.setColorFilter(currentAppColor, PorterDuff.Mode.SRC_IN)
                animateClick(selected)
                drawingSurface.currentMode = mode
                currentActiveTool = selected
                if (mode == DrawMode.LASER) triggerHapticFeedback(30)
                if (mode != DrawMode.TEXT) cancelTextInput()
                drawParams.flags = baseWindowFlags
                requestSmoothUpdate(drawingSurface)
            }
        }

        panelBinding.toolBrush.setOnClickListener { setActiveTool(it as ImageView, DrawMode.BRUSH) }
        panelBinding.toolLaser.setOnClickListener { setActiveTool(it as ImageView, DrawMode.LASER) }
        panelBinding.toolRect.setOnClickListener { setActiveTool(it as ImageView, DrawMode.RECT) }
        panelBinding.toolCircle.setOnClickListener { setActiveTool(it as ImageView, DrawMode.CIRCLE) }
        panelBinding.toolArrow.setOnClickListener { setActiveTool(it as ImageView, DrawMode.ARROW) }
        panelBinding.toolText.setOnClickListener { setActiveTool(it as ImageView, DrawMode.TEXT) }
        panelBinding.toolMove.setOnClickListener { setActiveTool(it as ImageView, DrawMode.MOVE) }
        panelBinding.toolEraser.setOnClickListener { setActiveTool(it as ImageView, DrawMode.ERASER) }
        
        panelBinding.toolCensor.setOnClickListener {
            drawingSurface.isCensorMode = !drawingSurface.isCensorMode
            animateClick(panelBinding.toolCensor)
            
            if (drawingSurface.isCensorMode) {
                panelBinding.toolCensor.setColorFilter(activeSystemColor, PorterDuff.Mode.SRC_IN)
                Toast.makeText(this@FloatingDrawService, locContext.getString(R.string.toast_censor_instruction), Toast.LENGTH_SHORT).show()
            } else {
                panelBinding.toolCensor.setColorFilter(baseIconColor, PorterDuff.Mode.SRC_IN)
            }
            triggerHapticFeedback(20)
        }
        
        panelBinding.toolColorPicker.setOnClickListener {
            animateClick(it)
            val isOpening = colorPopupWrapper.visibility == View.GONE || panelBinding.colorsContainer.visibility == View.GONE
            if (isOpening) {
                showColorPopup(panelBinding.colorsContainer, panelBinding.customPickerContainer)
            } else {
                hideColorPopup()
            }
            triggerHapticFeedback(20)
        }
        
        panelBinding.toolUndo.setOnClickListener {
            animateClick(it)
            if (textInputBinding != null) cancelTextInput() else drawingSurface.undoLastAction()
            triggerHapticFeedback(20)
        }
        
        panelBinding.toolClear.setOnClickListener {
            animateClick(it)
            cancelTextInput(); drawingSurface.clearCanvas()
            triggerHapticFeedback(30)
        }
        
        panelBinding.toolScreenshot.setOnClickListener {
            animateClick(it)
            triggerHapticFeedback(30)
            takeScreenshot()
        }

        panelBinding.toolSettings.setOnClickListener {
            animateClick(it)
            triggerHapticFeedback(20)
            val intent = Intent(this@FloatingDrawService, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            closePanelAction() 
        }
        
        panelBinding.toolClose.setOnClickListener {
            animateClick(it)
            closePanelAction() 
        }

        fun applyColor(view: View?, color: Int) {
            view?.let { animateClick(it) }
            currentAppColor = color
            drawingSurface.currentColor = color
            val isColorableTool = drawingSurface.currentMode in listOf(DrawMode.BRUSH, DrawMode.RECT, DrawMode.CIRCLE, DrawMode.ARROW, DrawMode.TEXT)
            if (isColorableTool && currentActiveTool != null) {
                currentActiveTool?.setColorFilter(currentAppColor, PorterDuff.Mode.SRC_IN)
            }
            panelBinding.toolColorPicker.setColorFilter(currentAppColor, PorterDuff.Mode.SRC_IN)
            if (view != null) {
                hideColorPopup()
                triggerHapticFeedback(25)
            }
        }

        val colorWheel = ColorWheelView(themeContext)
        panelBinding.colorWheelFrame.addView(colorWheel)
        var selectedCustomColor = Color.WHITE
        colorWheel.onColorSelected = { color ->
            selectedCustomColor = color
            panelBinding.colorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            applyColor(null, color)
        }
        
        panelBinding.colorCustom.setOnClickListener {
            animateClick(it)
            showColorPopup(panelBinding.customPickerContainer, panelBinding.colorsContainer)
            triggerHapticFeedback(20)
        }
        
        panelBinding.colorRed.setOnClickListener { applyColor(it, Color.parseColor("#F44336")) }
        panelBinding.colorBlue.setOnClickListener { applyColor(it, Color.parseColor("#2196F3")) }
        panelBinding.colorGreen.setOnClickListener { applyColor(it, Color.parseColor("#4CAF50")) }
        panelBinding.colorYellow.setOnClickListener { applyColor(it, Color.parseColor("#FFEB3B")) }
        panelBinding.colorTeal.setOnClickListener { applyColor(it, Color.parseColor("#00B693")) }
        panelBinding.colorBlack.setOnClickListener { applyColor(it, Color.parseColor("#FFFFFF")) }
        panelBinding.btnApplyCustomColor.setOnClickListener { applyColor(it, selectedCustomColor) }
    }

    private fun restoreUI() {
        buttonWrapper.visibility = View.VISIBLE
        val currentBtnScale = buttonWrapper.targetScale
        buttonWrapper.childView.scaleX = currentBtnScale
        buttonWrapper.childView.scaleY = currentBtnScale
        buttonWrapper.childView.alpha = 1f
        drawParams.flags = baseWindowFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        requestSmoothUpdate(drawingSurface)
    }

    private fun takeScreenshot() {
        if (isCapturing || destroyed) return
        isCapturing = true
        panelWrapper.visibility = View.GONE
        buttonWrapper.visibility = View.GONE
        closeTargetBinding.root.visibility = View.GONE
        hideColorPopup()
        val engine = getSharedPreferences("DrawSettings", MODE_PRIVATE).getInt("core_engine_type", EngineManager.ENGINE_STANDARD)
        serviceScope.launch {
            try {
                delay(150)
                withTimeout(120_000) { CaptureCoordinator(this@FloatingDrawService).capture(engine) }
                triggerHapticFeedback(50)
                SmartNotificationHelper.showScreenshotSuccessNotification(this@FloatingDrawService)
            } catch (cancelled: CancellationException) {
                if (cancelled is TimeoutCancellationException && !destroyed)
                    Toast.makeText(this@FloatingDrawService, locContext.getString(R.string.ui_capture_timeout), Toast.LENGTH_LONG).show()
                else throw cancelled
            } catch (e: Exception) {
                if (!destroyed) Toast.makeText(this@FloatingDrawService,
                    locContext.getString(R.string.toast_error_prefix) + " " + (e.message ?: locContext.getString(R.string.error_unknown)), Toast.LENGTH_LONG).show()
            } finally { if (!destroyed) finishCapture() }
        }
    }

    private fun finishCapture() {
        isCapturing = false
        restoreUI()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        systemDark = newConfig.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        refreshOverlayAppearance()
        if (!::windowManager.isInitialized || !::buttonParams.isInitialized || destroyed) return
        val previousWidth = screenWidth
        val previousHeight = screenHeight
        updateScreenDimensions()
        val metricsChanged = layoutDensity != resources.displayMetrics.density || layoutFontScale != newConfig.fontScale
        layoutDensity = resources.displayMetrics.density
        layoutFontScale = newConfig.fontScale
        // A night-mode-only update must not move or dock either window.
        if (previousWidth == screenWidth && previousHeight == screenHeight && !metricsChanged) return
        val prefs = getSharedPreferences("DrawSettings", MODE_PRIVATE)
        applyButtonScale(prefs.getFloat("button_scale", 1f))
        applyPanelScale(prefs.getFloat("panel_scale", 1f))
        buttonWrapper.post { if (!destroyed) buttonPositioner?.restore() }
    }

    override fun onDestroy() {
        destroyed = true
        buttonPositioner?.cancel()
        serviceScope.cancel()
        refreshToolColors = null
        Choreographer.getInstance().removeFrameCallback(updateFrameCallback)
        pendingUpdates.clear()
        if (::drawingSurface.isInitialized) drawingSurface.animate().cancel()
        super.onDestroy()
        ServiceController.publish(false)
        isRunning = false
        instance = null
        if (::buttonWrapper.isInitialized) try { windowManager.removeView(buttonWrapper) } catch (e: Exception) {}
        if (::panelWrapper.isInitialized) try { windowManager.removeView(panelWrapper) } catch (e: Exception) {}
        if (::colorPopupWrapper.isInitialized) try { windowManager.removeView(colorPopupWrapper) } catch (e: Exception) {}
        if (::closeTargetBinding.isInitialized) try { windowManager.removeView(closeTargetBinding.root) } catch (e: Exception) {}
        if (textInputBinding != null) try { windowManager.removeView(textInputBinding!!.root) } catch (e: Exception) {}
        if (::drawingSurface.isInitialized) try { windowManager.removeView(drawingSurface) } catch (e: Exception) {}
    }
}
