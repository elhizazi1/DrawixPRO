package xyz.siwane.drawix.pro.engine;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
/** Status only: never accepts commands, image data or executable code. */
public final class BridgeStatusProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 public Bundle call(String method,String arg,Bundle extras){
  int uid=Binder.getCallingUid();
  SharedPreferences prefs=getContext().getSharedPreferences("bridge_host_status",Context.MODE_PRIVATE);
  int boot=Settings.Global.getInt(getContext().getContentResolver(),Settings.Global.BOOT_COUNT,-1);
  if("report".equals(method)) {
   int hostUid;
   try { hostUid=getContext().getPackageManager().getApplicationInfo(arg,0).uid; }
   catch(android.content.pm.PackageManager.NameNotFoundException absent) { throw new SecurityException("Unknown host"); }
   if(uid!=hostUid) throw new SecurityException("Caller UID does not own this host");
   if(!"android".equals(arg) && !"com.android.systemui".equals(arg)) throw new IllegalArgumentException("Invalid host");
   String phase=extras==null?null:extras.getString("phase");
   String detail=extras==null?null:extras.getString("detail");
   String value="boot="+boot+"; phase="+phase+"; uid="+uid+"; time="+System.currentTimeMillis()+"; detail="+detail;
   prefs.edit().putInt("boot",boot).putString(arg,value.substring(0,Math.min(1000,value.length()))).apply();
   return Bundle.EMPTY;
  }
  if(!"read".equals(method) || uid!=android.os.Process.myUid()) throw new SecurityException("Private diagnostics");
  Bundle result=new Bundle();
  if(prefs.getInt("boot",-2)==boot) for(String host:new String[]{"android","com.android.systemui"}) result.putString(host,prefs.getString(host,"not_reported"));
  return result;
 }
 public Cursor query(Uri u,String[] p,String s,String[] a,String o){throw new UnsupportedOperationException();}
 public String getType(Uri u){return null;}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
 public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
 public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
