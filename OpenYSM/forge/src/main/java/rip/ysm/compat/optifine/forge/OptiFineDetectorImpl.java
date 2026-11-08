package rip.ysm.compat.optifine.forge;
import com.elfmcys.yesstevemodel.client.compat.optifine.OptiFineDetector;

public final class OptiFineDetectorImpl {

    private OptiFineDetectorImpl() {
    }

    public static boolean isOptifinePresent() {
        return OptiFineDetector.isOptifinePresent();
    }
}
