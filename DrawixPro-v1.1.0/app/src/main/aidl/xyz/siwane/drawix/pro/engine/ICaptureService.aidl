package xyz.siwane.drawix.pro.engine;
import android.os.ParcelFileDescriptor;
interface ICaptureService {
    ParcelFileDescriptor capture() = 1;
    void destroy() = 16777114;
}
