package xyz.siwane.drawix.pro.ui.engine

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.siwane.drawix.pro.EngineManager
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.ui.DrawViewModel
import xyz.siwane.drawix.pro.ui.components.*

private data class Engine(val id: Int, val title: Int, val description: Int, val icon: Int)
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EngineScreen(vm: DrawViewModel, onShizuku: () -> Unit) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val issue by vm.engineIssue.collectAsStateWithLifecycle()
    val bridgeHost by xyz.siwane.drawix.pro.engine.LSPosedCaptureClient.readyHost.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val engines = listOf(
        Engine(0, R.string.engine_standard_title, R.string.ui_standard_desc, R.drawable.ic_shield_tick),
        Engine(1, R.string.engine_shizuku_title, R.string.ui_shizuku_desc, R.drawable.ic_speedometer),
        Engine(2, R.string.engine_root_title, R.string.ui_root_desc, R.drawable.ic_code),
        Engine(3, R.string.engine_lsposed_title, R.string.ui_lsposed_desc, R.drawable.ic_3dcube))
    val revision by vm.revision.collectAsStateWithLifecycle()
    val selected = remember(revision) { vm.preferences.int("core_engine_type") }
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { InfoCard(text(R.string.ui_engines_title), text(R.string.ui_engine_subtitle), R.drawable.ic_info_circle) }
        if (busy) item { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text(R.string.ui_checking), style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } }
        engines.forEachIndexed { index, engine -> item {
            val status = when (engine.id) {
                1 -> when { !EngineManager.isShizukuRunning() -> R.string.ui_not_running
                    !EngineManager.hasShizukuPermission() -> R.string.ui_authorize
                    else -> R.string.ui_ready }
                2 -> when (com.topjohnwu.superuser.Shell.isAppGrantedRoot()) {
                    true -> R.string.ui_ready; false -> R.string.ui_setup_required; null -> R.string.ui_root_unknown }
                3 -> if (bridgeHost != null) R.string.ui_ready else if (EngineManager.isLSPosedActive()) R.string.ui_module_enabled else R.string.ui_setup_required
                else -> if (android.os.Build.VERSION.SDK_INT < 30 || vm.hasAccessibility()) R.string.ui_ready else R.string.ui_setup_required
            }
            val isSelected = selected == engine.id
            GlassSurface(shape = CardCorners.grouped(index, engines.size), color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().selectable(isSelected, role = Role.RadioButton,
                    enabled = !(busy && engine.id == 2), onClick = { vm.selectEngine(engine.id, onShizuku) }).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .65f)) {
                            DrawIcon(engine.icon, modifier = Modifier.padding(8.dp).size(22.dp))
                        }
                        Text(text(engine.title), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        RadioButton(isSelected, onClick = null)
                    }
                    Text(text(engine.description), style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(text(status))
                        if (isSelected) StatusPill(text(R.string.ui_selected), true)
                    }
                }
            }
        } }
        item { TextButton(onClick = { vm.refresh() }) { Text(text(R.string.ui_refresh)) } }
    }
    issue?.let { engine ->
        val descriptor = engines.first { it.id == engine }
        var managerMissing by remember(engine) { mutableStateOf(false) }
        ModalBottomSheet(onDismissRequest = vm::dismissEngineIssue) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                DrawixBloom(descriptor.icon, Modifier.size(80.dp))
                Text(text(descriptor.title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(text(when (engine) { 0 -> R.string.capture_standard_setup; 1 -> if (EngineManager.isShizukuRunning()) R.string.ui_authorize else R.string.dialog_shizuku_missing_desc; 2 -> R.string.dialog_root_missing_desc; else -> if (xyz.siwane.drawix.pro.engine.LSPosedCaptureClient.needsRestart()) R.string.capture_lsposed_restart else R.string.capture_lsposed_hosts_missing }), style = MaterialTheme.typography.bodyMedium)
                InfoCard(text(R.string.ui_selected), text(R.string.ui_engine_kept), R.drawable.ic_shield_tick)
                Button(onClick = { vm.dismissEngineIssue(); vm.selectEngine(engine, onShizuku) }, modifier = Modifier.fillMaxWidth()) { Text(text(R.string.ui_try_again)) }
                if (engine != 3) {
                    OutlinedButton(onClick = {
                        if (engine == 0) {
                            runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }.onFailure { managerMissing = true }
                        } else {
                            val packages = if (engine == 1) listOf("moe.shizuku.privileged.api") else listOf("com.topjohnwu.magisk", "me.weishu.kernelsu", "me.bmax.apatch")
                            val intent = packages.firstNotNullOfOrNull { context.packageManager.getLaunchIntentForPackage(it) }
                            if (intent == null) managerMissing = true else runCatching { context.startActivity(intent) }.onFailure { managerMissing = true }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text(text(if (engine == 0) R.string.btn_grant_settings else R.string.ui_open_manager)) }
                    if (managerMissing) Text(text(R.string.ui_no_handler), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = vm::dismissEngineIssue) { Text(text(R.string.ui_close)) }
            }
        }
    }
}
