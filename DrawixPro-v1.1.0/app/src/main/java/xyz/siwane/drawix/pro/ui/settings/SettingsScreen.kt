package xyz.siwane.drawix.pro.ui.settings

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.siwane.drawix.pro.FloatingDrawService
import xyz.siwane.drawix.pro.SplashActivity
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.ui.DrawViewModel
import xyz.siwane.drawix.pro.ui.components.*
import xyz.siwane.drawix.pro.ui.engine.EngineScreen
import xyz.siwane.drawix.pro.ui.layout.LocalLayoutSpec
import xyz.siwane.drawix.pro.ui.theme.LocalAccents

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SettingsScreen(vm: DrawViewModel, onShizuku: () -> Unit, onRecreate: () -> Unit) {
    val revision by vm.revision.collectAsStateWithLifecycle()
    val prefs = remember(revision) { PreferenceSnapshot(vm.preferences) }
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf("home") }
    var showLanguages by remember { mutableStateOf(false) }
    var showHideDialog by remember { mutableStateOf(false) }
    BackHandler(page != "home") { page = "home" }
    fun setHidden(hidden: Boolean) {
        context.packageManager.setComponentEnabledSetting(ComponentName(context, SplashActivity::class.java),
            if (hidden) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP)
        prefs.put("hide_app_icon", hidden)
    }
    Column(Modifier.fillMaxSize()) {
        if (page != "home") Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { page = "home" }) { DrawIcon(R.drawable.ic_arrow_back, text(R.string.nav_settings)) }
            Text(text(when (page) { "look" -> R.string.ui_style_title; "engines" -> R.string.ui_engines_title
                "drawing" -> R.string.settings_section_drawing; else -> R.string.settings_community_title }),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        AnimatedContent(page, label = "settings page") { current ->
            if (current == "engines") EngineScreen(vm, onShizuku)
            else LazyColumn(contentPadding = PaddingValues(start = LocalLayoutSpec.current.padding.dp, end = LocalLayoutSpec.current.padding.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when (current) {
                    "home" -> {
                        item { PageHero(text(R.string.nav_settings), text(R.string.ui_style_subtitle), R.drawable.ic_setting_2) }
                        item { CategoryCard(text(R.string.ui_style_title), text(R.string.ui_style_subtitle), R.drawable.ic_color_swatch,
                            LocalAccents.current.mint, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 6.dp, bottomEnd = 6.dp)) { page = "look" } }
                        item { CategoryCard(text(R.string.ui_engines_title), text(R.string.ui_engine_subtitle), R.drawable.ic_speedometer,
                            LocalAccents.current.sky, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 22.dp, bottomEnd = 22.dp)) { page = "engines" } }
                        item { Spacer(Modifier.height(8.dp)) }
                        item { CategoryCard(text(R.string.settings_section_drawing), text(R.string.ui_drawing_subtitle), R.drawable.ic_designtools,
                            LocalAccents.current.lilac, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 6.dp, bottomEnd = 6.dp)) { page = "drawing" } }
                        item { CategoryCard(text(R.string.settings_community_title), text(R.string.ui_community_subtitle), R.drawable.ic_language_circle,
                            LocalAccents.current.rose, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 22.dp, bottomEnd = 22.dp)) { page = "community" } }
                    }
                    "look" -> {
                        item { AppearanceControls(prefs) }
                        item { ActionRow(text(R.string.settings_language), languageName(prefs.text("app_language"), prefs), R.drawable.ic_language_circle, shape = CardCorners.grouped(0, 2)) { showLanguages = true } }
                        item { ToggleRow(text(R.string.settings_hide_icon), text(R.string.settings_hide_icon_desc), prefs.bool("hide_app_icon", false), shape = CardCorners.grouped(1, 2)) {
                            if (it) showHideDialog = true else setHidden(false)
                        } }
                    }
                    "drawing" -> {
                        item { SectionTitle(text(R.string.settings_section_sizes)) }
                        item { PreferenceSlider(text(R.string.settings_size), prefs.float("button_scale"), .5f..1.5f, "%", shape = CardCorners.grouped(0, 3)) {
                            prefs.put("button_scale", it); prefs.put("button_scale_progress", ((it - .5f) * 100).toInt()); FloatingDrawService.instance?.applyButtonScale(it)
                        } }
                        item { PreferenceSlider(text(R.string.settings_ui_size), prefs.float("panel_scale"), .5f..1.5f, "%", shape = CardCorners.grouped(1, 3)) {
                            prefs.put("panel_scale", it); prefs.put("ui_scale_progress", ((it - .5f) * 100).toInt()); FloatingDrawService.instance?.applyPanelScale(it)
                        } }
                        item { PreferenceSlider(text(R.string.settings_opacity), prefs.float("btn_alpha_value"), .2f..1f, "%", shape = CardCorners.grouped(2, 3)) {
                            prefs.put("btn_alpha_value", it); prefs.put("btn_opacity_progress", 100 - (it * 100).toInt()); FloatingDrawService.instance?.applyButtonOpacity(it)
                        } }
                        item { OverlayControls(prefs) }
                        item { SectionTitle(text(R.string.settings_section_drawing)) }
                        item { ToggleRow(text(R.string.settings_laser_mode), text(R.string.settings_laser_desc), prefs.bool("laser_comet_mode"), shape = CardCorners.grouped(0, if (prefs.bool("laser_comet_mode")) 1 else 2)) {
                            prefs.put("laser_comet_mode", it); FloatingDrawService.instance?.syncLaserModeUI()
                        } }
                        if (!prefs.bool("laser_comet_mode")) item { PreferenceSlider(text(R.string.settings_laser_duration), prefs.int("laser_duration_seconds", 2).toFloat(), 1f..10f, text(R.string.settings_seconds_unit), shape = CardCorners.grouped(1, 2)) {
                            prefs.put("laser_duration_seconds", it.toInt()); prefs.put("laser_duration_progress", it.toInt() - 1)
                        } }
                        item { SectionTitle(text(R.string.settings_section_saving)) }
                        item { ToggleRow(text(R.string.settings_save_png), text(R.string.settings_save_png_desc), prefs.bool("save_as_png"), shape = CardCorners.grouped(0, 2)) { prefs.put("save_as_png", it) } }
                        item { ToggleRow(text(R.string.settings_haptic), text(R.string.settings_haptic_desc), prefs.bool("haptic_enabled"), shape = CardCorners.grouped(1, 2)) { prefs.put("haptic_enabled", it) } }
                    }
                    else -> { item { TranslationControls(vm, onRecreate) } }
                }
            }
        }
    }
    if (showLanguages) ModalBottomSheet(onDismissRequest = { showLanguages = false }) {
        val codes = listOf("ar", "en", "fr", "es", "pt", "ru", "hi", "zh") + prefs.store.all.keys.filter { it.startsWith("custom_lang_name_") }.map { it.removePrefix("custom_lang_name_") }
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(text(R.string.settings_language), style = MaterialTheme.typography.headlineSmall) }
            codes.forEachIndexed { index, code -> item { ActionRow(languageName(code, prefs), shape = CardCorners.grouped(index, codes.size)) { prefs.put("app_language", code); showLanguages = false; onRecreate() } } }
        }
    }
    if (showHideDialog) AlertDialog(onDismissRequest = { showHideDialog = false },
        title = { Text(text(R.string.settings_hide_icon)) }, text = { Text(text(R.string.ui_hide_confirm)) },
        confirmButton = { TextButton(onClick = { setHidden(true); showHideDialog = false }) { Text(text(R.string.btn_apply)) } },
        dismissButton = { TextButton(onClick = { showHideDialog = false }) { Text(text(R.string.dialog_cancel)) } })
}
private fun languageName(code: String, prefs: PreferenceSnapshot): String =
    if (code.startsWith("custom_")) prefs.text("custom_lang_name_$code", code)
    else java.util.Locale.forLanguageTag(code).getDisplayName(java.util.Locale.forLanguageTag(code))
