package xyz.siwane.drawix.pro.capture;
import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
/** No Kotlin or AndroidX classes are loaded into a host to answer a request. */
public final class LSPosedReceiver extends BroadcastReceiver {
 public interface Capture { Bitmap run() throws Exception; }
 public interface ErrorLog { void accept(Throwable error); }
 private final Context host; private final String family; private final Capture capture; private final ErrorLog log;
 private final AtomicBoolean capturing = new AtomicBoolean();
 public LSPosedReceiver(Context host,String family,Capture capture,ErrorLog log) { this.host=host; this.family=family; this.capture=capture; this.log=log; }
 private void respond(ResultReceiver reply,int code,String error) {
  Bundle b=new Bundle(); b.putInt("version",4); b.putString("family",family); b.putString("host",host.getPackageName()); b.putString("revision","capture-signature-r2");
  b.putInt("uid",android.os.Process.myUid()); b.putString("error",error); reply.send(code,b);
 }
 @SuppressWarnings("deprecation") public void onReceive(Context context,Intent intent) {
  try {
   intent.setExtrasClassLoader(ResultReceiver.class.getClassLoader());
   if(!"xyz.siwane.drawix.pro.LSPOSED_REQUEST_V4".equals(intent.getAction()) || intent.getIntExtra("version",0)!=4) return;
   ResultReceiver reply=intent.getParcelableExtra("reply"); if(reply==null) return;
   Uri uri=intent.getParcelableExtra("output"); if(uri==null) { respond(reply,0,null); return; }
   if(!"content".equals(uri.getScheme()) || !"xyz.siwane.drawix.pro.provider".equals(uri.getAuthority())) { respond(reply,1,null); return; }
   if(!capturing.compareAndSet(false,true)) { respond(reply,2,null); return; }
   new Thread(() -> {
    long identity=Binder.clearCallingIdentity();
    try {
     Bitmap bitmap=capture.run();
     try(OutputStream stream=host.getContentResolver().openOutputStream(uri,"wt")) {
      if(stream==null || !bitmap.compress(Bitmap.CompressFormat.PNG,100,stream)) throw new IllegalStateException("PNG write failed");
     } finally { bitmap.recycle(); }
     respond(reply,0,null);
    } catch(Throwable error) { log.accept(error);
     Throwable cause=error;
     for(int depth=0;depth<12 && cause.getCause()!=null && cause.getCause()!=cause;depth++) cause=cause.getCause();
     respond(reply,1,cause.getClass().getSimpleName()+": "+cause.getMessage()); }
    finally { Binder.restoreCallingIdentity(identity); capturing.set(false); }
   },"DrawixCapture").start();
  } catch(Throwable error) { log.accept(error); }
 }
}
