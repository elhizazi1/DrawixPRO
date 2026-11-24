package xyz.siwane.drawix.pro

import android.content.Intent
import android.os.Bundle

/** Preserves the installed launcher component without an artificial startup delay. */
class SplashActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
