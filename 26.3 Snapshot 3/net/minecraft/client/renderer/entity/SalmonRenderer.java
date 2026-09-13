package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.fish.SalmonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.SalmonRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.fish.Salmon;

public class SalmonRenderer extends MobRenderer<Salmon, SalmonRenderState, SalmonModel> {
   private static final Identifier SALMON_LOCATION = Identifier.withDefaultNamespace("textures/entity/fish/salmon.png");
   private final SalmonModel smallSalmonModel;
   private final SalmonModel mediumSalmonModel;
   private final SalmonModel largeSalmonModel;

   public SalmonRenderer(final EntityRendererProvider.Context context) {
      super(context, new SalmonModel(context.bakeLayer(ModelLayers.SALMON)), 0.4F);
      this.smallSalmonModel = new SalmonModel(context.bakeLayer(ModelLayers.SALMON_SMALL));
      this.mediumSalmonModel = new SalmonModel(context.bakeLayer(ModelLayers.SALMON));
      this.largeSalmonModel = new SalmonModel(context.bakeLayer(ModelLayers.SALMON_LARGE));
   }

   public void extractRenderState(final Salmon entity, final SalmonRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.variant = entity.getVariant();
   }

   public Identifier getTextureLocation(final SalmonRenderState state) {
      return SALMON_LOCATION;
   }

   public SalmonRenderState createRenderState() {
      return new SalmonRenderState();
   }

   protected void setupRotations(final SalmonRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
      super.setupRotations(state, poseStack, bodyRot, entityScale);
      float amplitudeMultiplier = 1.0F;
      float angleMultiplier = 1.0F;
      if (!state.isInWater) {
         amplitudeMultiplier = 1.3F;
         angleMultiplier = 1.7F;
      }

      float bodyZRot = amplitudeMultiplier * 4.3F * Mth.sin(angleMultiplier * 0.6F * state.ageInTicks);
      poseStack.mulPose(Axis.YP.rotationDegrees(bodyZRot));
      if (!state.isInWater) {
         poseStack.translate(0.2F, 0.1F, 0.0F);
         poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
      }
   }

   public void submit(final SalmonRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      this.model = switch (state.variant) {
         case SMALL -> this.smallSalmonModel;
         case MEDIUM -> this.mediumSalmonModel;
         case LARGE -> this.largeSalmonModel;
      };
      super.submit(state, poseStack, submitNodeCollector, camera);
   }
}
