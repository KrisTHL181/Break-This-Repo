package rip.ysm.compat.swem.fabric;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import net.minecraft.world.entity.LivingEntity;

public final class SWEMCompatImpl {

    private SWEMCompatImpl() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static String getHorseGaitName(LivingEntity livingEntity) {
        return "";
    }

    public static void registerControllerFunctions(CtrlBinding ctrlBinding) {
        ctrlBinding.livingEntityVar("swem_is_ride", ctx -> false);
        ctrlBinding.livingEntityVar("swem_state", ctx -> StringPool.EMPTY);
    }
}
