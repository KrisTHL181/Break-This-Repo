package rip.ysm.compat.parcool.forge;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.function.BiFunction;
import com.elfmcys.yesstevemodel.client.compat.parcool.ParcoolCompat;

public final class ParcoolCompatImpl {

    private ParcoolCompatImpl() {
    }

    public static boolean isLoaded() {
        return ParcoolCompat.isLoaded();
    }

    public static Optional<Pair<String, String>> getInCompatibleInfo() {
        return ParcoolCompat.getInCompatibleInfo();
    }

    public static Optional<BiFunction<String, CustomPlayerEntity, IAnimationController<CustomPlayerEntity>>> getControllerFactory() {
        return ParcoolCompat.getControllerFactory();
    }

    public static boolean isPlayerParcooling(Player player) {
        return ParcoolCompat.isPlayerParcooling(player);
    }

    public static String getActionName(Player player) {
        return ParcoolCompat.getActionName(player);
    }

    public static void registerBindings(CtrlBinding binding) {
        ParcoolCompat.registerBindings(binding);
    }
}
