package rip.ysm.compat.oculus.forge;
import com.elfmcys.yesstevemodel.client.compat.oculus.OculusCompat;

public final class OculusCompatImpl {

    private OculusCompatImpl() {
    }

    public static boolean isLoaded() {
        return OculusCompat.isLoaded();
    }

    public static boolean isPBRActive() {
        return OculusCompat.isPBRActive();
    }

    public static void updatePBRState() {
        OculusCompat.updatePBRState();
    }

    public static boolean isShaderPackInUse() {
        return OculusCompat.isShaderPackInUse();
    }

    public static boolean isRenderingShadowPass() {
        return OculusCompat.isRenderingShadowPass();
    }
}
