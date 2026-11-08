package rip.ysm.api.item.forge;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolActions;

public final class ToolActionBridgeImpl {

    private ToolActionBridgeImpl() {
    }

    public static boolean canFishingRodCast(ItemStack stack) {
        return stack.canPerformAction(ToolActions.FISHING_ROD_CAST);
    }

    public static boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return stack.onEntitySwing(entity);
    }
}
