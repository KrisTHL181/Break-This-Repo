package rip.ysm.compat.create.forge;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.player.Player;
import com.elfmcys.yesstevemodel.client.compat.create.CreateCompat;

public final class CreateCompatImpl {

    private CreateCompatImpl() {
    }

    public static boolean isLoaded() {
        return CreateCompat.isLoaded();
    }

    public static boolean isPlayerOnCreateContraption(Player player) {
        return CreateCompat.isPlayerOnCreateContraption(player);
    }

    public static void registerCreateFunctions(CtrlBinding binding) {
        CreateCompat.registerCreateFunctions(binding);
    }
}
