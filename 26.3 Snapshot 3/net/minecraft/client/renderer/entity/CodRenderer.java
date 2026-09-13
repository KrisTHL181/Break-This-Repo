package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.fish.CodModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.fish.Cod;

public class CodRenderer extends MobRenderer<Cod, LivingEntityRenderState, CodModel> {
   private static final Identifier COD_LOCATION = Identifier.withDefaultNamespace("textures/entity/fish/cod.png");

   public CodRenderer(final EntityRendererProvider.Context context) {
      super(context, new CodModel(context.bakeLayer(ModelLayers.COD)), 0.3F);
   }

   @Override
   public Identifier getTextureLocation(final LivingEntityRenderState state) {
      return COD_LOCATION;
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   @Override
   protected void setupRotations(final LivingEntityRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
      super.setupRotations(state, poseStack, bodyRot, entityScale);
      float bodyZRot = 4.3F * Mth.sin(0.6F * state.ageInTicks);
      poseStack.mulPose(Axis.YP.rotationDegrees(bodyZRot));
      if (!state.isInWater) {
         poseStack.translate(0.1F, 0.1F, -0.1F);
         poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
      }
   }
}
