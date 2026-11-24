package xyz.siwane.drawix.pro.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.data.GalleryImageLoader
import xyz.siwane.drawix.pro.data.ScreenshotRepository
import xyz.siwane.drawix.pro.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GalleryViewer(images: List<ScreenshotRepository.Image>, initial: ScreenshotRepository.Image,
    onClose: () -> Unit, onShare: (ScreenshotRepository.Image) -> Unit,
    onOpen: (ScreenshotRepository.Image) -> Unit, onDelete: (ScreenshotRepository.Image) -> Unit) {
    val pager = rememberPagerState(initialPage = images.indexOfFirst { it.uri == initial.uri }.coerceAtLeast(0), pageCount = { images.size })
    val scope = rememberCoroutineScope()
    var zoomed by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    val image = images.getOrNull(pager.currentPage.coerceAtMost(images.lastIndex).coerceAtLeast(0)) ?: return
    LaunchedEffect(pager.currentPage) { zoomed = false }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Scaffold(modifier = Modifier.fillMaxSize().systemBarsPadding(), containerColor = Color.Black, contentColor = Color.White,
                topBar = { TopAppBar(title = { Text(image.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall) },
                    navigationIcon = { IconButton(onClick = onClose) { DrawIcon(R.drawable.ic_arrow_back, text(R.string.ui_close)) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White, navigationIconContentColor = Color.White)) },
                bottomBar = { Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
                    Text(text(R.string.gallery_zoom_hint), style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally), color = Color.LightGray)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(enabled = pager.currentPage > 0, onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) { DrawIcon(R.drawable.ic_arrow_back, text(R.string.gallery_previous)) }
                        Text("${pager.currentPage + 1} / ${images.size}", style = MaterialTheme.typography.labelLarge)
                        IconButton(enabled = pager.currentPage < images.lastIndex, onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { DrawIcon(R.drawable.ic_arrow_right_3, text(R.string.gallery_next)) }
                        IconButton(onClick = { onShare(image) }) { DrawIcon(R.drawable.ic_share, text(R.string.share_chooser_title)) }
                        IconButton(onClick = { details = true }) { DrawIcon(R.drawable.ic_info_circle, text(R.string.gallery_details)) }
                        IconButton(onClick = { onOpen(image) }) { DrawIcon(R.drawable.ic_gallery_export, text(R.string.gallery_open_external)) }
                        IconButton(onClick = { onDelete(image) }) { DrawIcon(R.drawable.ic_trash, text(R.string.dialog_delete_title)) }
                    }
                } }) { padding ->
                HorizontalPager(state = pager, userScrollEnabled = !zoomed, modifier = Modifier.fillMaxSize().padding(padding), key = { images[it].uri.toString() }) { page ->
                    OriginalPicture(images[page], onZoom = { if (page == pager.currentPage) zoomed = it })
                }
            }
            if (details) ImageDetails(image, onClose = { details = false })
        }
    }
}

@Composable private fun OriginalPicture(image: ScreenshotRepository.Image, onZoom: (Boolean) -> Unit) {
    val context = LocalContext.current
    var failed by remember(image.uri) { mutableStateOf(false) }
    val bitmap by produceState<android.graphics.Bitmap?>(null, image.uri) {
        value = withContext(Dispatchers.IO) { runCatching { GalleryImageLoader(context).decode(image.uri) }.getOrNull() }
        failed = value == null
    }
    var scale by remember(image.uri) { mutableFloatStateOf(1f) }
    var offset by remember(image.uri) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    fun bound(pan: Offset, zoom: Float): Offset {
        val b = bitmap ?: return Offset.Zero
        val fit = minOf(viewport.width.toFloat() / b.width, viewport.height.toFloat() / b.height)
        val x = ((b.width * fit * zoom - viewport.width) / 2).coerceAtLeast(0f)
        val y = ((b.height * fit * zoom - viewport.height) / 2).coerceAtLeast(0f)
        return Offset(pan.x.coerceIn(-x, x), pan.y.coerceIn(-y, y))
    }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 6f)
        offset = bound(offset + pan, scale)
        onZoom(scale > 1f)
    }
    Box(Modifier.fillMaxSize().clipToBounds().onSizeChanged { viewport = it }
        .pointerInput(image.uri) { detectTapGestures(onDoubleTap = {
            scale = if (scale > 1f) 1f else 2.5f; offset = Offset.Zero; onZoom(scale > 1f)
        }) }.transformable(state = transform, canPan = { scale > 1f }), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it.asImageBitmap(), image.name,
            Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }, contentScale = ContentScale.Fit) }
            ?: if (failed) Text(text(R.string.ui_gallery_error), modifier = Modifier.padding(24.dp)) else CircularProgressIndicator()
    }
}

@Composable private fun ImageDetails(image: ScreenshotRepository.Image, onClose: () -> Unit) {
    val context = LocalContext.current
    val info by produceState("", image.uri) {
        value = withContext(Dispatchers.IO) { runCatching {
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(image.uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
            var size = 0L
            context.contentResolver.query(image.uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { if (it.moveToFirst()) size = it.getLong(0) }
            "${bounds.outWidth} × ${bounds.outHeight} px\n${android.text.format.Formatter.formatFileSize(context, size)}\n${context.contentResolver.getType(image.uri).orEmpty()}\n${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(image.date))}"
        }.getOrDefault("") }
    }
    AlertDialog(onDismissRequest = onClose, title = { Text(text(R.string.gallery_details)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(image.name); Text(info) } },
        confirmButton = { TextButton(onClick = onClose) { Text(text(R.string.ui_close)) } })
}
