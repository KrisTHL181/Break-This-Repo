package rip.ysm.compat.realcamera.forge;
import com.elfmcys.yesstevemodel.client.compat.realcamera.RealCameraCompat;

public final class RealCameraCompatImpl {

    private RealCameraCompatImpl() {
    }

    public static boolean isLoaded() {
        return RealCameraCompat.isLoaded();
    }

    public static boolean isActive() {
        return RealCameraCompat.isActive();
    }
}
