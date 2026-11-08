package rip.ysm.compat.bettercombat.fabric;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;

public final class BetterCombatCompatImpl {

    private BetterCombatCompatImpl() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static void registerBindings(CtrlBinding binding) {
        binding.clientPlayerEntityVar("bcombat_attack_animation", ctx -> StringPool.EMPTY);
    }
}
