package xyz.siwane.drawix.pro;
import android.app.Application;
import android.content.Context;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.siwane.drawix.pro.xposed.LSPosedSystemBridge;
public final class LSPosedModule implements IXposedHookLoadPackage {
 public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam p) {
  try {
   if (p.packageName.equals("xyz.siwane.drawix.pro")) {
    XposedHelpers.findAndHookMethod("xyz.siwane.drawix.pro.EngineManager", p.classLoader, "isLSPosedActive", XC_MethodReplacement.returnConstant(true));
    XposedHelpers.findAndHookMethod("xyz.siwane.drawix.pro.EngineManager", p.classLoader, "lsposedLoadedProtocol", XC_MethodReplacement.returnConstant(4));
    XposedHelpers.findAndHookMethod("xyz.siwane.drawix.pro.EngineManager", p.classLoader, "lsposedManagerPackage", XC_MethodReplacement.returnConstant(LSPosedSystemBridge.family().equals("Vector") ? "org.matrix.vector.manager" : "org.lsposed.manager"));
   }
   if (p.packageName.equals("android") && (p.processName.equals("android") || p.processName.equals("system_server"))) LSPosedSystemBridge.install(p.classLoader);
   if (p.packageName.equals("com.android.systemui") && p.processName.equals("com.android.systemui")) {
    XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
     protected void afterHookedMethod(MethodHookParam h) { LSPosedSystemBridge.attachApplication((Context)h.args[0], p.classLoader); }
    });
    Class<?> thread = XposedHelpers.findClass("android.app.ActivityThread", p.classLoader);
    Application app = (Application)XposedHelpers.callStaticMethod(thread, "currentApplication");
    if(app != null) LSPosedSystemBridge.attachApplication(app, p.classLoader);
   }
  } catch(Throwable error) { XposedBridge.log("DrawixPro: entry " + p.packageName + ": " + error); }
 }
}
