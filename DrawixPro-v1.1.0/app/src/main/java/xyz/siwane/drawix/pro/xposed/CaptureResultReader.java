package xyz.siwane.drawix.pro.xposed;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
/** OEM/AOSP variants expose getBuffer() or ScreenshotSync.get(). */
public final class CaptureResultReader {
 public static Object read(Object waiter) throws Exception {
  if(waiter==null) throw new IllegalStateException("No capture listener");
  Method method;
  try { method=waiter.getClass().getMethod("getBuffer"); }
  catch(NoSuchMethodException absent) { method=waiter.getClass().getMethod("get"); }
  method.setAccessible(true);
  try { return method.invoke(waiter); }
  catch(InvocationTargetException wrapper) {
   Throwable cause=wrapper.getCause();
   if(cause instanceof Exception) throw (Exception)cause;
   if(cause instanceof Error) throw (Error)cause;
   throw wrapper;
  }
 }
}
