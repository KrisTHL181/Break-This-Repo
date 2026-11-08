package rip.ysm.compat.simplehats.forge;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.elfmcys.yesstevemodel.client.compat.simplehats.SimpleHatsHelper;

public final class SimpleHatsHelperImpl {

    private SimpleHatsHelperImpl() {
    }

    public static boolean isLoaded() {
        return SimpleHatsHelper.isLoaded();
    }

    public static ItemStack getHatItem(LivingEntity livingEntity) {
        return SimpleHatsHelper.getHatItem(livingEntity);
    }
}
