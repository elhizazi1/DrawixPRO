package xyz.siwane.drawix.pro

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import xyz.siwane.drawix.pro.data.AppLocale
import xyz.siwane.drawix.pro.engine.ServiceController

/** Launches the visible Activity on recent Android before starting foreground work. */
class DrawTileService : TileService() {
    override fun onStartListening() { super.onStartListening(); updateTileState() }
    override fun onClick() {
        super.onClick()
        if (isLocked) { unlockAndRun { openApp() }; return }
        if (FloatingDrawService.isRunning) ServiceController.stop(this) else openApp()
        updateTileState()
    }
    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated") // PendingIntent overload is unavailable below API 34.
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 4, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        else { @Suppress("DEPRECATION") startActivityAndCollapse(intent) }
    }
    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.state = if (FloatingDrawService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = AppLocale.wrap(this).getString(if (FloatingDrawService.isRunning) R.string.tile_active_label else R.string.tile_inactive_label)
        tile.updateTile()
    }
}
