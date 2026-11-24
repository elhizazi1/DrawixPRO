package xyz.siwane.drawix.pro.xposed;
import android.content.Context;
import android.net.Uri;
import android.os.*;
import de.robv.android.xposed.XposedBridge;
/** Delays app-provider startup until credential storage is unlocked. */
final class HostStatus {
 static void report(Context context,String phase,String detail){report(context,phase,detail,0);}
 private static void report(Context context,String phase,String detail,int attempt){
  try {
   UserManager users=context.getSystemService(UserManager.class);
   if(users!=null && !users.isUserUnlocked()) {
    if(attempt<120) new Handler(Looper.getMainLooper()).postDelayed(() -> report(context,phase,detail,attempt+1),5000);
    return;
   }
   Bundle data=new Bundle();data.putString("phase",phase);data.putString("detail",detail);
   context.getContentResolver().call(Uri.parse("content://xyz.siwane.drawix.pro.bridge-status"),"report",context.getPackageName(),data);
  } catch(Throwable error){XposedBridge.log("DrawixPro: host status unavailable: "+error);}
 }
}
