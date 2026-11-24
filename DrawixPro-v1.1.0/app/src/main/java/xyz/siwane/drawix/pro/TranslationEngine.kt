package xyz.siwane.drawix.pro

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.util.Locale

object TranslationEngine {
    @Volatile private var translations: Map<String, String> = emptyMap()
    var isCustomLanguageActive = false
        private set
    private val validCode = Regex("[a-z][a-z0-9_-]{0,31}")
    fun init(context: Context) {
        val language = context.getSharedPreferences("DrawSettings", Context.MODE_PRIVATE).getString("app_language", "ar") ?: "ar"
        translations = if (language.startsWith("custom_") && language.matches(validCode)) runCatching {
            val json = JSONObject(File(context.filesDir, "custom_langs/$language.json").readText())
            json.keys().asSequence().filter { !it.startsWith("_") }.associateWith { json.getString(it) }
        }.getOrDefault(emptyMap()) else emptyMap()
        isCustomLanguageActive = translations.isNotEmpty()
    }
    fun getCustomString(context: Context, resId: Int, defaultString: String): String =
        runCatching { translations[context.resources.getResourceEntryName(resId)] ?: defaultString }.getOrDefault(defaultString)
    fun generateTemplateJson(context: Context, baseIsEnglish: Boolean): String {
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale(if (baseIsEnglish) "en" else "ar")) }
        // Application context avoids the Activity translation wrapper during template export.
        val localized = context.applicationContext.createConfigurationContext(config)
        val json = JSONObject().put("_INSTRUCTION_1", "Translate values only. Keep resource keys unchanged.")
        R.string::class.java.fields.sortedBy { it.name }.forEach { field ->
            runCatching { json.put(field.name, localized.getString(field.getInt(null))) }
        }
        return json.toString(4)
    }
    fun importTranslation(context: Context, uri: Uri, langNameNative: String, langCodeStr: String): Boolean = runCatching {
        val code = langCodeStr.lowercase(Locale.ROOT)
        require(code.matches(validCode) && langNameNative.isNotBlank())
        val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                require(output.size() + count <= 1_048_576)
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        val valid = R.string::class.java.fields.map { it.name }.toSet()
        val clean = JSONObject()
        json.keys().forEach { key ->
            if (!key.startsWith("_") && key in valid) {
                val value = json.get(key)
                require(value is String)
                // Formatting failures in service notifications must not crash the process.
                val id = context.resources.getIdentifier(key, "string", context.packageName)
                val base = context.applicationContext.getString(id)
                val format = Regex("%(?:[0-9]+\\$)?[-#+ 0,(]*[0-9]*(?:\\.[0-9]+)?[a-zA-Z%]")
                require(format.findAll(base).map { it.value }.toList().sorted() == format.findAll(value).map { it.value }.toList().sorted())
                clean.put(key, value)
            }
        }
        require(clean.length() > 0)
        val directory = File(context.filesDir, "custom_langs").apply { mkdirs() }
        val file = File(directory, "custom_$code.json")
        val temporary = File(directory, "custom_$code.tmp")
        temporary.writeText(clean.toString(2))
        check(temporary.renameTo(file))
        context.getSharedPreferences("DrawSettings", Context.MODE_PRIVATE).edit().putString("custom_lang_name_custom_$code", langNameNative.trim()).apply()
        true
    }.getOrDefault(false)
}
