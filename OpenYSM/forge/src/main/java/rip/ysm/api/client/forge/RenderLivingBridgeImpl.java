package rip.ysm.api.client.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;

public final class RenderLivingBridgeImpl {

    private RenderLivingBridgeImpl() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean firePre(LivingEntity entity, LivingEntityRenderer<?, ?> renderer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        return MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Pre<>(entity, (LivingEntityRenderer) renderer, partialTick, poseStack, bufferSource, packedLight));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void firePost(LivingEntity entity, LivingEntityRenderer<?, ?> renderer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post<>(entity, (LivingEntityRenderer) renderer, partialTick, poseStack, bufferSource, packedLight));
    }
}
