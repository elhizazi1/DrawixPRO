package xyz.siwane.drawix.pro.ui.theme

enum class ThemeMode(val key: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark; LIGHT -> false; DARK -> true
    }
    companion object {
        fun resolve(value: String?, legacyDark: Boolean? = null): ThemeMode =
            entries.firstOrNull { it.key == value } ?: when (legacyDark) {
                true -> DARK; false -> LIGHT; null -> SYSTEM
            }
    }
}
