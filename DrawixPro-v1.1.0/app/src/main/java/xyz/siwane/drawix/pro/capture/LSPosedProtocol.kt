package xyz.siwane.drawix.pro.capture

/** The bridge only accepts signature-protected requests sent by Drawix. */
object LSPosedProtocol {
    const val PACKAGE = "xyz.siwane.drawix.pro"
    const val ACTION = "$PACKAGE.LSPOSED_REQUEST_V4"
    const val PERMISSION = "$PACKAGE.permission.CAPTURE"
    const val VERSION = 4
    const val REVISION = "capture-signature-r2"
    val HOSTS = listOf("android", "com.android.systemui")
    const val READY = 0
    const val CAPTURE_FAILED = 1
    const val BUSY = 2
}
