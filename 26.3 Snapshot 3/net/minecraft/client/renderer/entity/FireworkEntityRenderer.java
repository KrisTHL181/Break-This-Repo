package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.FireworkRocketRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemDisplayContext;

public class FireworkEntityRenderer extends EntityRenderer<FireworkRocketEntity, FireworkRocketRenderState> {
   private final ItemModelResolver itemModelResolver;

   public FireworkEntityRenderer(final EntityRendererProvider.Context context) {
      super(context);
      this.itemModelResolver = context.getItemModelResolver();
   }

   public void submit(
      final FireworkRocketRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      poseStack.pushPose();
      poseStack.mulPose(camera.orientation);
      if (state.isShotAtAngle) {
         poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
      }

      state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      poseStack.popPose();
      super.submit(state, poseStack, submitNodeCollector, camera);
   }

   public FireworkRocketRenderState createRenderState() {
      return new FireworkRocketRenderState();
   }

   public void extractRenderState(final FireworkRocketEntity entity, final FireworkRocketRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isShotAtAngle = entity.isShotAtAngle();
      this.itemModelResolver.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.GROUND, entity);
   }
}
