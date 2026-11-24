package xyz.siwane.drawix.pro

import android.app.Application
import android.content.Context
import xyz.siwane.drawix.pro.data.AppLocale

class DrawApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.topjohnwu.superuser.Shell.setDefaultBuilder(com.topjohnwu.superuser.Shell.Builder.create().setTimeout(30))
    }
    override fun attachBaseContext(base: Context) { super.attachBaseContext(AppLocale.wrap(base)) }
}
