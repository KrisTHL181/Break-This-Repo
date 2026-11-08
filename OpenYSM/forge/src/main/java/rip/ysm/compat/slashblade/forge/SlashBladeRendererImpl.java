package rip.ysm.compat.slashblade.forge;

import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeRenderer;

public final class SlashBladeRendererImpl {

    private SlashBladeRendererImpl() {
    }

    public static void renderOnEntity(LivingEntity entity, AnimatedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, ItemStack stack, float partialTick) {
        SlashBladeRenderer.renderOnEntity(entity, model, poseStack, bufferSource, packedLight, stack, partialTick);
    }

    public static void renderRightWaist(AnimatedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, ItemStack stack) {
        SlashBladeRenderer.renderRightWaist(model, poseStack, bufferSource, packedLight, stack);
    }
}
