package xyz.siwane.drawix.pro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rikka.shizuku.Shizuku
import xyz.siwane.drawix.pro.ui.DrawViewModel
import xyz.siwane.drawix.pro.ui.DrawixApp
import xyz.siwane.drawix.pro.ui.theme.DrawixTheme

class MainActivity : BaseActivity() {
    private val vm: DrawViewModel by viewModels()
    private var destination by mutableStateOf<String?>(null)
    private var startAfterOverlay = false
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.refresh() }
    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.refresh()
        if (startAfterOverlay && vm.hasOverlay() && !vm.running.value) vm.toggleService()
        startAfterOverlay = false
    }
    private val shizukuResult = Shizuku.OnRequestPermissionResultListener { code, grant ->
        if (code == 1001) runOnUiThread {
            vm.onShizukuPermissionResult(grant == PackageManager.PERMISSION_GRANTED)
        }
    }
    private val binderReady = Shizuku.OnBinderReceivedListener { runOnUiThread { vm.refresh() } }
    private val binderDead = Shizuku.OnBinderDeadListener { runOnUiThread { vm.onShizukuDisconnected() } }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        destination = intent.getStringExtra("destination")
        Shizuku.addRequestPermissionResultListener(shizukuResult)
        Shizuku.addBinderReceivedListenerSticky(binderReady)
        Shizuku.addBinderDeadListener(binderDead)
        setContent {
            val revision by vm.revision.collectAsStateWithLifecycle()
            @Suppress("UNUSED_VARIABLE") val currentRevision = revision
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val mode = xyz.siwane.drawix.pro.ui.theme.ThemeMode.resolve(
                vm.preferences.store.getString("theme_mode", null),
                if (vm.preferences.store.contains("dark_mode_enabled")) vm.preferences.bool("dark_mode_enabled") else null)
            val dark = mode.isDark(systemDark)
            SideEffect {
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            DrawixTheme(dark = dark, dynamic = vm.preferences.bool("dynamic_color"),
                seed = vm.preferences.int("accent_color", 0xFF006B5E.toInt())) {
                DrawixApp(vm, destination, onOverlay = {
                    startAfterOverlay = true
                    openPermission("overlay")
                }, onPermission = ::openPermission, onShizuku = {
                    EngineManager.requestShizukuPermission(1001)
                }, onRecreate = { recreate() })
            }
        }
    }
    override fun onResume() { super.onResume(); vm.refresh() }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        destination = intent.getStringExtra("destination")
    }
    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuResult)
        Shizuku.removeBinderReceivedListener(binderReady)
        Shizuku.removeBinderDeadListener(binderDead)
        super.onDestroy()
    }
    private fun openPermission(key: String) {
        val intent = when (key) {
            "overlay" -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            "battery" -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            "storage" -> {
                if (Build.VERSION.SDK_INT < 29) permissionLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE))
                else if (Build.VERSION.SDK_INT >= 34) permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
                else if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                else permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                return
            }
            "notifications" -> {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS)); return
                }
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        }
        runCatching { settingsLauncher.launch(intent) }.onFailure { vm.showMessage(R.string.ui_no_handler) }
    }
}
