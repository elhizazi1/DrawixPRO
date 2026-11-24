package xyz.siwane.drawix.pro.capture

/** Modern standard capture must never open app-sharing UI or capture a different app. */
object StandardCapturePolicy {
    enum class Route { ACCESSIBILITY, SETUP_ACCESSIBILITY, PROJECTION }
    fun route(sdk: Int, accessibilityConnected: Boolean): Route =
        if (sdk < 30) Route.PROJECTION
        else if (accessibilityConnected) Route.ACCESSIBILITY else Route.SETUP_ACCESSIBILITY
}
