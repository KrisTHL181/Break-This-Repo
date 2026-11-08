package rip.ysm.compat.create;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;

public final class CreateCompat {

    private CreateCompat() {
    }

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isPlayerOnCreateContraption(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerCreateFunctions(CtrlBinding binding) {
        throw new AssertionError();
    }
}
