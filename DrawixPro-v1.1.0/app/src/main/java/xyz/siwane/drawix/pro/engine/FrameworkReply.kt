package xyz.siwane.drawix.pro.engine

import android.os.Parcel
import android.os.ResultReceiver

/** A boot-classloader-safe envelope; sending the anonymous app subclass would expose its class name. */
object FrameworkReply {
    fun forTransport(callback: ResultReceiver): ResultReceiver {
        val parcel = Parcel.obtain()
        return try {
            callback.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            ResultReceiver.CREATOR.createFromParcel(parcel)
        } finally { parcel.recycle() }
    }
}
