package xyz.siwane.drawix.pro.xposed;
import android.content.*;
import android.os.*;
import de.robv.android.xposed.*;
import java.util.concurrent.atomic.AtomicBoolean;
import xyz.siwane.drawix.pro.capture.LSPosedReceiver;
public final class LSPosedSystemBridge {
 private static final AtomicBoolean registered = new AtomicBoolean();
 public static String family() {
  try { Class.forName("org.matrix.vector.Startup", false, XposedBridge.class.getClassLoader()); return "Vector"; }
  catch(Throwable ignored) { return "LSPosed"; }
 }
 public static void install(final ClassLoader loader) {
  Class<?> ams = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", loader);
  XposedBridge.hookAllMethods(ams, "systemReady", new XC_MethodHook() {
   protected void afterHookedMethod(MethodHookParam p) {
    if(!p.hasThrowable()) try { register((Context)XposedHelpers.getObjectField(p.thisObject,"mContext"), loader); }
    catch(Throwable e) { XposedBridge.log("DrawixPro: systemReady: " + e); }
   }
  });
  if(Looper.getMainLooper() != null) new Handler(Looper.getMainLooper()).post(new Runnable() {
   int tries;
   public void run() {
    if(registered.get()) return;
    try {
     Class<?> at = XposedHelpers.findClass("android.app.ActivityThread",loader);
     Object thread = XposedHelpers.callStaticMethod(at,"currentActivityThread");
     Context context = (Context)XposedHelpers.callMethod(thread,"getSystemContext");
     Object manager = XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityManager",loader),"getService");
     if(manager != null) register(context,loader);
    } catch(Throwable e) { if(tries == 30) XposedBridge.log("DrawixPro: late registration: " + e); }
    if(!registered.get() && tries++ < 30) new Handler(Looper.getMainLooper()).postDelayed(this,2000);
   }
  });
 }
 public static void attachApplication(final Context context, final ClassLoader loader) {
  new Handler(Looper.getMainLooper()).post(() -> {
   try { register(context,loader); } catch(Throwable e) { XposedBridge.log("DrawixPro: application registration: " + e); }
  });
 }
 // Older Android releases use the permission-protected legacy overload; API 33+ explicitly exports.
 @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
 private static synchronized void register(Context context, ClassLoader loader) {
  if(registered.get()) return;
  HostStatus.report(context,"registering",family());
  try {
  LSPosedReceiver receiver = new LSPosedReceiver(context, family(), () -> SystemScreenCapture.capture(context,loader),
   error -> {
    HostStatus.report(context,"capture_error",error.toString());
    XposedBridge.log("DrawixPro: request failed: " + error);
    XposedBridge.log(error);
   });
  IntentFilter filter = new IntentFilter("xyz.siwane.drawix.pro.LSPOSED_REQUEST_V4");
  if(Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver,filter,"xyz.siwane.drawix.pro.permission.CAPTURE",null,Context.RECEIVER_EXPORTED);
  else context.registerReceiver(receiver,filter,"xyz.siwane.drawix.pro.permission.CAPTURE",null);
  registered.set(true);
  HostStatus.report(context,"ready",family());
  XposedBridge.log("DrawixPro: bridge v4 ready in " + context.getPackageName());
  } catch(Throwable error) {
   HostStatus.report(context,"register_error",error.toString());
   throw error;
  }
 }
}
