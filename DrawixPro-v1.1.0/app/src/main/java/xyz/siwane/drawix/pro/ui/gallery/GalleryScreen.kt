package xyz.siwane.drawix.pro.ui.gallery

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.data.ScreenshotRepository
import xyz.siwane.drawix.pro.ui.components.*

@Composable fun GalleryScreen(revision: Int, onPermission: (String) -> Unit) {
    val context = LocalContext.current
    val repo = remember { ScreenshotRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var images by remember { mutableStateOf<List<ScreenshotRepository.Image>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var oldestFirst by remember { mutableStateOf(false) }
    var compact by remember { mutableStateOf(false) }
    val shown = remember(images, query, oldestFirst) {
        images.filter { it.name.contains(query.trim(), ignoreCase = true) }.let { if (oldestFirst) it.sortedBy { image -> image.date } else it.sortedByDescending { image -> image.date } }
    }
    var selected by remember { mutableStateOf<ScreenshotRepository.Image?>(null) }
    var toDelete by remember { mutableStateOf<ScreenshotRepository.Image?>(null) }
    var retryDelete by remember { mutableStateOf<ScreenshotRepository.Image?>(null) }
    val deleteRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val retry = retryDelete
        retryDelete = null
        if (retry != null && result.resultCode == android.app.Activity.RESULT_OK) scope.launch {
            error = !withContext(Dispatchers.IO) { runCatching { repo.delete(retry) }.getOrDefault(false) }
            refresh++
        } else refresh++
    }
    DisposableEffect(context) {
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { refresh++ }
        }
        context.contentResolver.registerContentObserver(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    LaunchedEffect(revision, refresh) {
        loading = true
        val result = withContext(Dispatchers.IO) { runCatching { repo.list() } }
        images = result.getOrDefault(emptyList()); error = result.isFailure; loading = false
    }
    val shareTitle = text(R.string.share_chooser_title)
    fun share(image: ScreenshotRepository.Image) {
        runCatching {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = context.contentResolver.getType(image.uri) ?: "image/*"
                putExtra(Intent.EXTRA_STREAM, image.uri)
                clipData = android.content.ClipData.newRawUri(image.name, image.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, shareTitle))
        }.onFailure { error = true }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = { refresh++ }) { Text(text(R.string.ui_refresh)) }
            TextButton(onClick = { onPermission("storage") }) { Text(text(R.string.ui_older_images)) }
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
            label = { Text(text(R.string.gallery_search)) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = CardCorners.single)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${shown.size}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { oldestFirst = !oldestFirst }) { Text(text(if (oldestFirst) R.string.gallery_oldest else R.string.gallery_newest)) }
            TextButton(onClick = { compact = !compact }) { Text(text(if (compact) R.string.gallery_large_grid else R.string.gallery_compact_grid)) }
        }
        if (error && images.isNotEmpty()) Text(text(R.string.ui_gallery_error),
            color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp))
        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            images.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DrawixBloom(R.drawable.ic_gallery, modifier = Modifier.size(136.dp))
                    Text(text(if (error) R.string.ui_gallery_error else R.string.empty_gallery_text), style = MaterialTheme.typography.titleLarge)
                }
            }
            shown.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) { Text(text(R.string.gallery_no_results)) }
            else -> LazyVerticalGrid(GridCells.Adaptive(if (compact) 120.dp else 180.dp), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 110.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shown, key = { it.uri.toString() }, span = { GridItemSpan(if (shown.size == 1) maxLineSpan else 1) }) { image ->
                    GlassSurface(shape = CardCorners.single) { Column {
                        ScreenshotThumbnail(image, Modifier.fillMaxWidth().aspectRatio(if (compact) 1f else .75f).clickable { selected = image })
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
                                .format(java.util.Date(image.date)), style = MaterialTheme.typography.labelMedium)
                            Text(image.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            IconButton(onClick = { share(image) }) { DrawIcon(R.drawable.ic_share, text(R.string.share_chooser_title)) }
                            IconButton(onClick = { toDelete = image }) { DrawIcon(R.drawable.ic_trash, text(R.string.dialog_delete_title)) }
                        }
                    } }
                }
            }
        }
    }
    selected?.let { image ->
        if (images.isNotEmpty()) GalleryViewer(shown, image, onClose = { selected = null }, onShare = ::share,
            onOpen = { original ->
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(original.uri, context.contentResolver.getType(original.uri) ?: "image/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }.onFailure { error = true }
            }, onDelete = { toDelete = it; selected = null })
    }
    toDelete?.let { image -> AlertDialog(onDismissRequest = { toDelete = null },
        title = { Text(text(R.string.dialog_delete_title)) }, text = { Text(text(R.string.dialog_delete_message)) },
        confirmButton = { TextButton(onClick = {
            toDelete = null
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { repo.delete(image) } }
                if (result.getOrDefault(false)) refresh++
                else if (Build.VERSION.SDK_INT >= 30) {
                    runCatching { deleteRequest.launch(IntentSenderRequest.Builder(android.provider.MediaStore.createDeleteRequest(context.contentResolver, listOf(image.uri)).intentSender).build()) }
                        .onFailure { error = true }
                } else if (Build.VERSION.SDK_INT == 29 && result.exceptionOrNull() is android.app.RecoverableSecurityException) {
                    retryDelete = image
                    val exception = result.exceptionOrNull() as android.app.RecoverableSecurityException
                    deleteRequest.launch(IntentSenderRequest.Builder(exception.userAction.actionIntent.intentSender).build())
                } else error = true
            }
        }) { Text(text(R.string.dialog_delete_confirm)) } },
        dismissButton = { TextButton(onClick = { toDelete = null }) { Text(text(R.string.dialog_cancel)) } }) }
}

@Composable private fun ScreenshotThumbnail(image: ScreenshotRepository.Image, modifier: Modifier) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    BoxWithConstraints(modifier.clip(CardCorners.single), contentAlignment = Alignment.Center) {
        val widthPixels = with(density) { maxWidth.roundToPx() }.coerceIn(1, 2048)
        val heightPixels = with(density) { maxHeight.roundToPx() }.coerceIn(1, 2048)
        val bitmap by produceState<android.graphics.Bitmap?>(null, image.uri, widthPixels, heightPixels) {
            value = withContext(Dispatchers.IO) { runCatching {
                xyz.siwane.drawix.pro.data.GalleryImageLoader(context).decodePreview(image.uri, widthPixels, heightPixels)
            }.getOrNull() }
        }
        bitmap?.let { Image(it.asImageBitmap(), image.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter, filterQuality = androidx.compose.ui.graphics.FilterQuality.High) }
            ?: DrawIcon(R.drawable.ic_image, image.name)
    }
}
