package xyz.siwane.drawix.pro.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.TranslationEngine
import xyz.siwane.drawix.pro.ui.DrawViewModel
import xyz.siwane.drawix.pro.ui.components.text

@Composable fun TranslationControls(vm: DrawViewModel, onRecreate: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var english by rememberSaveable { mutableStateOf(true) }
    var showExport by remember { mutableStateOf(false) }
    var importUri by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var rtl by rememberSaveable { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching {
                val json = TranslationEngine.generateTemplateJson(context, english)
                requireNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(json.toByteArray(Charsets.UTF_8)) }
            } }
            vm.showMessage(if (result.isSuccess) R.string.toast_export_success else R.string.toast_export_failed)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importUri = uri?.toString()
        if (uri != null) runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text(R.string.settings_community_warning), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { showExport = true }, modifier = Modifier.fillMaxWidth()) { Text(text(R.string.btn_export_template)) }
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.fillMaxWidth()) { Text(text(R.string.btn_import_language)) }
    }
    if (showExport) AlertDialog(onDismissRequest = { showExport = false }, title = { Text(text(R.string.dialog_export_title)) },
        text = { Column {
            TextButton(onClick = { english = true; showExport = false; exportLauncher.launch("DrawixPro-English.json") }) { Text(text(R.string.export_option_en)) }
            TextButton(onClick = { english = false; showExport = false; exportLauncher.launch("DrawixPro-Arabic.json") }) { Text(text(R.string.export_option_ar)) }
        } }, confirmButton = {})
    if (importUri != null) AlertDialog(onDismissRequest = { importUri = null }, title = { Text(text(R.string.dialog_import_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text(text(R.string.hint_lang_name)) }, singleLine = true)
            OutlinedTextField(code, { code = it }, label = { Text(text(R.string.hint_lang_code)) }, singleLine = true)
            Row { Checkbox(rtl, { rtl = it }); Text(text(R.string.checkbox_rtl)) }
        } }, confirmButton = { TextButton(onClick = {
            if (name.isBlank() || !code.matches(Regex("[A-Za-z][A-Za-z0-9_-]{0,23}"))) vm.showMessage(R.string.toast_import_empty)
            else {
                val uri = Uri.parse(importUri)
                val directionCode = (if (rtl) "rtl_" else "ltr_") + code.lowercase(java.util.Locale.ROOT)
                scope.launch {
                    val success = withContext(Dispatchers.IO) { TranslationEngine.importTranslation(context, uri, name, directionCode) }
                    if (success) { vm.preferences.put("app_language", "custom_$directionCode"); importUri = null; onRecreate() }
                    else vm.showMessage(R.string.toast_import_failed)
                }
            }
        }) { Text(text(R.string.btn_confirm_lang)) } }, dismissButton = { TextButton(onClick = { importUri = null }) { Text(text(R.string.dialog_cancel)) } })
}
