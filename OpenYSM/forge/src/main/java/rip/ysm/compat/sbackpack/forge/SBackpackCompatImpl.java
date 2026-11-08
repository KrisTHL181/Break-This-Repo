package rip.ysm.compat.sbackpack.forge;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import com.elfmcys.yesstevemodel.client.compat.sbackpack.SBackpackCompat;

public final class SBackpackCompatImpl {

    private SBackpackCompatImpl() {
    }

    public static boolean isLoaded() {
        return SBackpackCompat.isLoaded();
    }

    public static void setupRenderLayers() {
        SBackpackCompat.setupRenderLayers();
    }

    public static Optional<Pair<String, String>> getInCompatibleInfo() {
        return SBackpackCompat.getInCompatibleInfo();
    }

    public static void registerControllerFunctions(CtrlBinding binding) {
        SBackpackCompat.registerControllerFunctions(binding);
    }

    public static ItemStack getBackpackItem(LivingEntity livingEntity) {
        return SBackpackCompat.getBackpackItem(livingEntity);
    }
}
