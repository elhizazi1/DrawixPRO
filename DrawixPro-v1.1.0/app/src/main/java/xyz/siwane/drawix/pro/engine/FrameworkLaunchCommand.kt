package xyz.siwane.drawix.pro.engine

/** Fixed commands checked against LSPosed.zip/action.sh and Vector/zygisk/module/action.sh. */
object FrameworkLaunchCommand {
    fun command(vector: Boolean, sdk: Int): String = if (vector)
        "am start -c org.matrix.vector.manager.LAUNCH_MANAGER com.android.shell/.BugreportWarningActivity"
    else "am broadcast -a ${if (sdk >= 29) "android.telephony.action.SECRET_CODE" else "android.provider.Telephony.SECRET_CODE"} -d android_secret_code://5776733 android"
}
