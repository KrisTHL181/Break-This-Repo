package rip.ysm.compat.carryon.forge;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.function.BiFunction;
import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnCompat;

public final class CarryOnCompatImpl {

    private CarryOnCompatImpl() {
    }

    public static boolean isLoaded() {
        return CarryOnCompat.isLoaded();
    }

    public static Optional<BiFunction<String, CustomPlayerEntity, IAnimationController<CustomPlayerEntity>>> getControllerFactory() {
        return CarryOnCompat.getControllerFactory();
    }

    public static boolean isPlayerCarrying(Player player) {
        return CarryOnCompat.isPlayerCarrying(player);
    }

    public static void registerBindings(CtrlBinding binding) {
        CarryOnCompat.registerBindings(binding);
    }
}
