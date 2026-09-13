package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.feline.AbstractFelineModel;
import net.minecraft.client.model.animal.feline.AdultCatModel;
import net.minecraft.client.model.animal.feline.BabyCatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.CatCollarLayer;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.feline.Cat;

public class CatRenderer extends AgeableMobRenderer<Cat, CatRenderState, AbstractFelineModel<CatRenderState>> {
   public CatRenderer(final EntityRendererProvider.Context context) {
      super(context, new AdultCatModel(context.bakeLayer(ModelLayers.CAT)), new BabyCatModel(context.bakeLayer(ModelLayers.CAT_BABY)), 0.4F);
      this.addLayer(new CatCollarLayer(this, context.getModelSet()));
   }

   public Identifier getTextureLocation(final CatRenderState state) {
      return state.texture;
   }

   public CatRenderState createRenderState() {
      return new CatRenderState();
   }

   public void extractRenderState(final Cat entity, final CatRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.texture = entity.getVariant().value().assetInfo(state.isBaby).texturePath();
      state.isCrouching = entity.isCrouching();
      state.isSprinting = entity.isSprinting();
      state.isSitting = entity.isInSittingPose();
      state.lieDownAmount = entity.getLieDownAmount(partialTicks);
      state.lieDownAmountTail = entity.getLieDownAmountTail(partialTicks);
      state.relaxStateOneAmount = entity.getRelaxStateOneAmount(partialTicks);
      state.isLyingOnTopOfSleepingPlayer = entity.isLyingOnTopOfSleepingPlayer();
      state.collarColor = entity.isTame() ? entity.getCollarColor() : null;
   }

   protected void setupRotations(final CatRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
      super.setupRotations(state, poseStack, bodyRot, entityScale);
      float lieDownAmount = state.lieDownAmount;
      if (lieDownAmount > 0.0F) {
         poseStack.translate(0.4F * lieDownAmount, 0.15F * lieDownAmount, 0.1F * lieDownAmount);
         poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.rotLerp(lieDownAmount, 0.0F, 90.0F)));
         if (state.isLyingOnTopOfSleepingPlayer) {
            poseStack.translate(0.15F * lieDownAmount, 0.0F, 0.0F);
         }
      }
   }
}
