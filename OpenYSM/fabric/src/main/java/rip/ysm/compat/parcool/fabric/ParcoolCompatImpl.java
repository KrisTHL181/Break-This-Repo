package rip.ysm.compat.parcool.fabric;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.function.BiFunction;

public final class ParcoolCompatImpl {

    private ParcoolCompatImpl() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static Optional<Pair<String, String>> getInCompatibleInfo() {
        return Optional.empty();
    }

    public static Optional<BiFunction<String, CustomPlayerEntity, IAnimationController<CustomPlayerEntity>>> getControllerFactory() {
        return Optional.empty();
    }

    public static boolean isPlayerParcooling(Player player) {
        return false;
    }

    public static String getActionName(Player player) {
        return "";
    }

    public static void registerBindings(CtrlBinding binding) {
        binding.livingEntityVar("parcool_state", interfaceC0807x6b368640 -> StringPool.EMPTY);
    }
}
