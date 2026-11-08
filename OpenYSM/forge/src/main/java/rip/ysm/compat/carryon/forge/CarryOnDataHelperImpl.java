package rip.ysm.compat.carryon.forge;

import com.elfmcys.yesstevemodel.client.compat.carryon.CarryOnDataHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class CarryOnDataHelperImpl {

    private CarryOnDataHelperImpl() {
    }

    public static boolean isPlayerCarrying(LivingEntity livingEntity) {
        return CarryOnDataHelper.isPlayerCarrying(livingEntity);
    }

    public static rip.ysm.compat.carryon.CarryOnDataHelper.CarryType getCarryType(Player player) {
        CarryOnDataHelper.CarryType raw = CarryOnDataHelper.getCarryType(player);
        return rip.ysm.compat.carryon.CarryOnDataHelper.CarryType.valueOf(raw.name());
    }
}
