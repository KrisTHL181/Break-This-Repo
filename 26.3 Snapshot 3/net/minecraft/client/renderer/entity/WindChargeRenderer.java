package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.WindChargeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;

public class WindChargeRenderer extends EntityRenderer<AbstractWindCharge, EntityRenderState> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/projectiles/wind_charge.png");
   private final WindChargeModel model;

   public WindChargeRenderer(final EntityRendererProvider.Context context) {
      super(context);
      this.model = new WindChargeModel(context.bakeLayer(ModelLayers.WIND_CHARGE));
   }

   @Override
   public void submit(final EntityRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      submitNodeCollector.submitModel(
         this.model,
         state,
         poseStack,
         RenderTypes.breezeWind(TEXTURE_LOCATION, this.xOffset(state.ageInTicks) % 1.0F, 0.0F),
         state.lightCoords,
         OverlayTexture.NO_OVERLAY,
         state.outlineColor
      );
      super.submit(state, poseStack, submitNodeCollector, camera);
   }

   protected float xOffset(final float t) {
      return t * 0.03F;
   }

   @Override
   public EntityRenderState createRenderState() {
      return new EntityRenderState();
   }
}
