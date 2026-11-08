package rip.ysm.compat.bettercombat.forge;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.compat.bettercombat.BetterCombatCompat;

public final class BetterCombatCompatImpl {

    private BetterCombatCompatImpl() {
    }

    public static boolean isLoaded() {
        return BetterCombatCompat.isLoaded();
    }

    public static void registerBindings(CtrlBinding binding) {
        BetterCombatCompat.registerBindings(binding);
    }
}
