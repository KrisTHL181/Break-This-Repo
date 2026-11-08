package rip.ysm.compat.cosmeticarmorreworked.forge;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.elfmcys.yesstevemodel.client.compat.cosmeticarmorreworked.CosmeticArmorHelper;

public final class CosmeticArmorHelperImpl {

    private CosmeticArmorHelperImpl() {
    }

    public static ItemStack getArmorItem(LivingEntity entity, EquipmentSlot slot) {
        return CosmeticArmorHelper.getArmorItem(entity, slot);
    }

    public static ItemStack getElytraItem(LivingEntity livingEntity) {
        return CosmeticArmorHelper.getElytraItem(livingEntity);
    }
}
