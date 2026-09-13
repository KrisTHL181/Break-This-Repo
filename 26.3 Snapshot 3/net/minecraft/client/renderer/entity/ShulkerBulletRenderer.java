package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ShulkerBulletModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ShulkerBulletRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ShulkerBullet;

public class ShulkerBulletRenderer extends EntityRenderer<ShulkerBullet, ShulkerBulletRenderState> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/shulker/spark.png");
   private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(TEXTURE_LOCATION);
   private final ShulkerBulletModel model;

   public ShulkerBulletRenderer(final EntityRendererProvider.Context context) {
      super(context);
      this.model = new ShulkerBulletModel(context.bakeLayer(ModelLayers.SHULKER_BULLET));
   }

   protected int getBlockLightLevel(final ShulkerBullet entity, final BlockPos blockPos) {
      return 15;
   }

   public void submit(
      final ShulkerBulletRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      poseStack.pushPose();
      float tc = state.ageInTicks;
      poseStack.translate(0.0F, 0.15F, 0.0F);
      poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(tc * 0.1F) * 180.0F));
      poseStack.mulPose(Axis.XP.rotationDegrees(Mth.cos(tc * 0.1F) * 180.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(tc * 0.15F) * 360.0F));
      poseStack.scale(-0.5F, -0.5F, 0.5F);
      submitNodeCollector.submitModel(this.model, state, poseStack, TEXTURE_LOCATION, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
      poseStack.scale(1.5F, 1.5F, 1.5F);
      submitNodeCollector.order(1)
         .submitModel(this.model, state, poseStack, RENDER_TYPE, state.lightCoords, OverlayTexture.NO_OVERLAY, 654311423, null, state.outlineColor);
      poseStack.popPose();
      super.submit(state, poseStack, submitNodeCollector, camera);
   }

   public ShulkerBulletRenderState createRenderState() {
      return new ShulkerBulletRenderState();
   }

   public void extractRenderState(final ShulkerBullet entity, final ShulkerBulletRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.yRot = entity.getYRot(partialTicks);
      state.xRot = entity.getXRot(partialTicks);
   }
}
