package xyz.siwane.drawix.pro.xposed;
import android.content.Context;
import android.graphics.*;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.util.*;
import android.view.WindowManager;
import de.robv.android.xposed.XposedHelpers;
public final class SystemScreenCapture {
 public static Bitmap capture(Context context,ClassLoader loader) throws Exception {
  if(Build.VERSION.SDK_INT>=34) {
   Object wm=XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.view.WindowManagerGlobal",loader),"getWindowManagerService");
   CaptureApiResolver.Binding binding=CaptureApiResolver.resolve(wm.getClass());
   de.robv.android.xposed.XposedBridge.log("DrawixPro: capture API="+binding.apiName());
   HostStatus.report(context,"capture_api",binding.apiName());
   Object sync=binding.newListener();
   Object listener=sync instanceof Pair ? ((Pair<?,?>)sync).first : sync;
   Object waiter=sync instanceof Pair ? ((Pair<?,?>)sync).second : sync;
   binding.capture(wm,listener);
   return copyBuffer(CaptureResultReader.read(waiter));
  }
  Class<?> surface=XposedHelpers.findClass("android.view.SurfaceControl",loader);
  if(Build.VERSION.SDK_INT>=31) {
   Object token=XposedHelpers.callStaticMethod(surface,"getInternalDisplayToken");
   Object builder=XposedHelpers.newInstance(XposedHelpers.findClass("android.view.SurfaceControl$DisplayCaptureArgs$Builder",loader),token);
   return copyBuffer(XposedHelpers.callStaticMethod(surface,"captureDisplay",XposedHelpers.callMethod(builder,"build")));
  }
  android.view.Display display=context.getSystemService(WindowManager.class).getDefaultDisplay();
  DisplayMetrics metrics=new DisplayMetrics(); display.getRealMetrics(metrics);
  Bitmap bitmap=(Bitmap)XposedHelpers.callStaticMethod(surface,"screenshot",new Rect(),metrics.widthPixels,metrics.heightPixels,display.getRotation());
  if(bitmap==null) throw new IllegalStateException("No screenshot buffer"); return bitmap;
 }
 @android.annotation.TargetApi(31)
 private static Bitmap copyBuffer(Object buffer) {
  if(buffer==null) throw new IllegalStateException("No screenshot buffer");
  HardwareBuffer hardware=(HardwareBuffer)XposedHelpers.callMethod(buffer,"getHardwareBuffer");
  Bitmap bitmap=null;
  try {
   bitmap=(Bitmap)XposedHelpers.callMethod(buffer,"asBitmap");
   Bitmap copy=bitmap==null?null:bitmap.copy(Bitmap.Config.ARGB_8888,false);
   if(copy==null) throw new IllegalStateException("No screenshot bitmap"); return copy;
  } finally { if(bitmap!=null) bitmap.recycle(); if(hardware!=null) hardware.close(); }
 }
}
