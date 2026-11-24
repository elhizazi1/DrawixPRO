package xyz.siwane.drawix.pro

import android.content.Intent
import android.os.Bundle
import xyz.siwane.drawix.pro.ui.Destination

class GalleryActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java).putExtra("destination", Destination.GALLERY.name)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
}
