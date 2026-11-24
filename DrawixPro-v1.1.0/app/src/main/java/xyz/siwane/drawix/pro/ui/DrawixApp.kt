package xyz.siwane.drawix.pro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.ui.components.*
import xyz.siwane.drawix.pro.ui.layout.*
import xyz.siwane.drawix.pro.ui.home.HomeScreen
import xyz.siwane.drawix.pro.ui.permissions.PermissionsScreen
import xyz.siwane.drawix.pro.ui.settings.SettingsScreen
import xyz.siwane.drawix.pro.ui.gallery.GalleryScreen

enum class Destination(val label: Int, val icon: Int) {
    HOME(R.string.nav_home, R.drawable.ic_home_2),
    GALLERY(R.string.nav_gallery, R.drawable.ic_gallery),
    PERMISSIONS(R.string.nav_permissions, R.drawable.ic_shield_tick),
    SETTINGS(R.string.nav_settings, R.drawable.ic_setting_2)
}

@Composable
fun DrawixApp(vm: DrawViewModel, initialDestination: String?, onOverlay: () -> Unit,
    onPermission: (String) -> Unit, onShizuku: () -> Unit, onRecreate: () -> Unit) {
    val revision by vm.revision.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    LaunchedEffect(initialDestination) {
        if (initialDestination != null) destination = Destination.entries.firstOrNull { it.name == initialDestination } ?: Destination.HOME
    }
    BackHandler(destination != Destination.HOME) { destination = Destination.HOME }
    val snackbar = remember { SnackbarHostState() }
    val message by vm.message.collectAsStateWithLifecycle()
    val messageText = message?.let { text(it) }
    LaunchedEffect(messageText) {
        if (messageText != null) { snackbar.showSnackbar(messageText); vm.messageConsumed() }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        val layoutSpec = LayoutSpec.forWidth(maxWidth.value.toInt())
        val glassLayer = rememberGraphicsLayer()
        val glassScene = remember(glassLayer) { GlassScene(glassLayer) }
        Box(Modifier.fillMaxSize()) {
        LiquidBackdrop(glassScene)
        CompositionLocalProvider(LocalLayoutSpec provides layoutSpec, LocalGlassScene provides glassScene) {
        Row {
            if (wide) NavigationRail(Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical))) {
                Spacer(Modifier.height(24.dp))
                Destination.entries.forEach { item -> NavigationRailItem(
                    selected = destination == item, onClick = { destination = item },
                    icon = { DrawIcon(item.icon) }, label = { Text(text(item.label)) }) }
            }
            val backdrop = rememberGraphicsLayer()
            var backdropOrigin by remember { mutableStateOf(Offset.Zero) }
            Box(Modifier.weight(1f).fillMaxHeight()) {
            Scaffold(modifier = Modifier.fillMaxSize().onGloballyPositioned { backdropOrigin = it.positionInRoot() }
                .drawWithContent {
                    backdrop.record { this@drawWithContent.drawContent() }
                    drawLayer(backdrop)
                }, containerColor = androidx.compose.ui.graphics.Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface, snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = if (wide) 0.dp else 80.dp)) }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                    Column(Modifier.widthIn(max = 900.dp).fillMaxSize()) {
                        if (destination == Destination.GALLERY) Text(text(destination.label),
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp))
                        AnimatedContent(destination, label = "destination") { screen ->
                            // Reading revision makes preference edits refresh every destination consistently.
                            @Suppress("UNUSED_VARIABLE") val currentRevision = revision
                            when (screen) {
                                Destination.HOME -> HomeScreen(vm, onToggle = { if (!vm.hasOverlay()) onOverlay() else vm.toggleService() })
                                Destination.GALLERY -> GalleryScreen(revision, onPermission)
                                Destination.PERMISSIONS -> PermissionsScreen(vm, onPermission)
                                Destination.SETTINGS -> SettingsScreen(vm, onShizuku, onRecreate)
                            }
                        }
                    }
                }
            }
            if (!wide) Box(Modifier.align(Alignment.BottomCenter)) {
                FloatingNavigation(destination, backdrop, backdropOrigin) { destination = it }
            }
            }

        }
    }
}
}

}
