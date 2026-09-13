package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemDisplayContext;

public class ThrownItemRenderer<T extends Entity & ItemSupplier> extends EntityRenderer<T, ThrownItemRenderState> {
   private final ItemModelResolver itemModelResolver;
   private final float scale;
   private final boolean fullBright;

   public ThrownItemRenderer(final EntityRendererProvider.Context context, final float scale, final boolean fullBright) {
      super(context);
      this.itemModelResolver = context.getItemModelResolver();
      this.scale = scale;
      this.fullBright = fullBright;
   }

   public ThrownItemRenderer(final EntityRendererProvider.Context context) {
      this(context, 1.0F, false);
   }

   @Override
   protected int getBlockLightLevel(final T entity, final BlockPos blockPos) {
      return this.fullBright ? 15 : super.getBlockLightLevel(entity, blockPos);
   }

   public void submit(
      final ThrownItemRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      poseStack.pushPose();
      poseStack.scale(this.scale, this.scale, this.scale);
      poseStack.mulPose(camera.orientation);
      state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      poseStack.popPose();
      super.submit(state, poseStack, submitNodeCollector, camera);
   }

   public ThrownItemRenderState createRenderState() {
      return new ThrownItemRenderState();
   }

   public void extractRenderState(final T entity, final ThrownItemRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      this.itemModelResolver.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.GROUND, entity);
   }
}
