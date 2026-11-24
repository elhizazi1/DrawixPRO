package xyz.siwane.drawix.pro.xposed;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Select the capture API that the running WindowManager actually accepts. */
public final class CaptureApiResolver {
 public static Binding resolve(Class<?> manager) throws NoSuchMethodException {
  StringBuilder observed=new StringBuilder();
  for(Method method:manager.getMethods()) {
   Class<?>[] types=method.getParameterTypes();
   if(!method.getName().equals("captureDisplay") || types.length!=3 || types[0]!=int.class) continue;
   Class<?> owner=types[2].getEnclosingClass();
   observed.append(method.toGenericString()).append(';');
   if(owner==null) continue;
   try {
    Method factory=owner.getDeclaredMethod("createSyncCaptureListener");
    if(!Modifier.isStatic(factory.getModifiers())) continue;
    method.setAccessible(true); factory.setAccessible(true);
    return new Binding(method,factory,types[2]);
   } catch(NoSuchMethodException absent) { /* Try another declared signature, never another engine. */ }
  }
  throw new NoSuchMethodException("No supported captureDisplay listener on "+manager.getName()+"; observed="+observed);
 }
 public static final class Binding {
  private final Method capture;
  private final Method factory;
  private final Class<?> listenerType;
  Binding(Method capture,Method factory,Class<?> listenerType) { this.capture=capture;this.factory=factory;this.listenerType=listenerType; }
  public String apiName(){return factory.getDeclaringClass().getName();}
  public Object newListener() throws Exception {return invoke(factory,null);}
  public void capture(Object manager,Object listener) throws Exception {
   if(!listenerType.isInstance(listener)) throw new IllegalArgumentException("Listener does not match "+capture.toGenericString());
   invoke(capture,manager,0,null,listener);
  }
 }
 private static Object invoke(Method method,Object target,Object... args) throws Exception {
  try{return method.invoke(target,args);}
  catch(InvocationTargetException wrapped) {
   Throwable cause=wrapped.getCause();
   if(cause instanceof Exception) throw (Exception)cause;
   if(cause instanceof Error) throw (Error)cause;
   throw wrapped;
  }
 }
}
