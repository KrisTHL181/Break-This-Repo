package rip.ysm.compat.gun.tacz.forge;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.elfmcys.yesstevemodel.client.compat.gun.tacz.TacCompat;

public final class TacCompatImpl {

    private TacCompatImpl() {
    }

    public static boolean isLoaded() {
        return TacCompat.isLoaded();
    }

    public static void registerControllerFunctions(CtrlBinding binding) {
        TacCompat.registerControllerFunctions(binding);
    }

    public static void applyItemTransform(ItemStack stack, AnimatedGeoModel model, LivingEntity entity, PoseStack poseStack, int packedLightIn, float partialTicks) {
        TacCompat.applyItemTransform(stack, model, entity, poseStack, packedLightIn, partialTicks);
    }

    public static PlayState handleTaczAnimState(LivingEntity entity, AnimationEvent<? extends LivingAnimatable<?>> event, String animation, ILoopType loopType) {
        return TacCompat.handleTaczAnimState(entity, event, animation, loopType);
    }

    public static PlayState handleGunHoldAnimState(ItemStack stack, AnimationEvent<? extends LivingAnimatable<?>> event) {
        return TacCompat.handleGunHoldAnimState(stack, event);
    }

    public static PlayState handleGunActionAnimState(ItemStack stack, AnimationEvent<? extends LivingAnimatable<?>> event) {
        return TacCompat.handleGunActionAnimState(stack, event);
    }

    public static void handleGunSound(LivingEntity entity, ItemStack stack) {
        TacCompat.handleGunSound(entity, stack);
    }

    public static void handleItemSound(ItemStack stack) {
        TacCompat.handleItemSound(stack);
    }

    public static ResourceLocation getGunTexture(ItemStack stack) {
        return TacCompat.getGunTexture(stack);
    }
}
