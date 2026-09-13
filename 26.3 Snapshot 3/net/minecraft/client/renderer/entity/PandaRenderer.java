package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.model.animal.panda.BabyPandaModel;
import net.minecraft.client.model.animal.panda.PandaModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.PandaHoldsItemLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PandaRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.panda.Panda;

public class PandaRenderer extends AgeableMobRenderer<Panda, PandaRenderState, PandaModel> {
   private static final Map<Panda.Gene, Identifier> TEXTURES = Maps.newEnumMap(
      Map.of(
         Panda.Gene.NORMAL,
         Identifier.withDefaultNamespace("textures/entity/panda/panda.png"),
         Panda.Gene.LAZY,
         Identifier.withDefaultNamespace("textures/entity/panda/panda_lazy.png"),
         Panda.Gene.WORRIED,
         Identifier.withDefaultNamespace("textures/entity/panda/panda_worried.png"),
         Panda.Gene.PLAYFUL,
         Identifier.withDefaultNamespace("textures/entity/panda/panda_playful.png"),
         Panda.Gene.BROWN,
         Identifier.withDefaultNamespace("textures/entity/panda/panda_brown.png"),
         Panda.Gene.WEAK,
         Identifier.withDefaultNamespace("textures/entity/panda/panda_weak.png"),
         Panda.Gene.AGGRESSIVE,
         Identifier.withDefaultNamespace("textures/entity/panda/panda_aggressive.png")
      )
   );
   private static final Map<Panda.Gene, Identifier> BABY_TEXTURES = Maps.newEnumMap(
      Map.of(
         Panda.Gene.NORMAL,
         Identifier.withDefaultNamespace("textures/entity/panda/panda_baby.png"),
         Panda.Gene.LAZY,
         Identifier.withDefaultNamespace("textures/entity/panda/lazy_panda_baby.png"),
         Panda.Gene.WORRIED,
         Identifier.withDefaultNamespace("textures/entity/panda/worried_panda_baby.png"),
         Panda.Gene.PLAYFUL,
         Identifier.withDefaultNamespace("textures/entity/panda/playful_panda_baby.png"),
         Panda.Gene.BROWN,
         Identifier.withDefaultNamespace("textures/entity/panda/brown_panda_baby.png"),
         Panda.Gene.WEAK,
         Identifier.withDefaultNamespace("textures/entity/panda/weak_panda_baby.png"),
         Panda.Gene.AGGRESSIVE,
         Identifier.withDefaultNamespace("textures/entity/panda/aggressive_panda_baby.png")
      )
   );

   public PandaRenderer(final EntityRendererProvider.Context context) {
      super(context, new PandaModel(context.bakeLayer(ModelLayers.PANDA)), new BabyPandaModel(context.bakeLayer(ModelLayers.PANDA_BABY)), 0.9F);
      this.addLayer(new PandaHoldsItemLayer(this));
   }

   public Identifier getTextureLocation(final PandaRenderState state) {
      Map<Panda.Gene, Identifier> textures = state.isBaby ? BABY_TEXTURES : TEXTURES;
      return textures.getOrDefault(state.variant, textures.get(Panda.Gene.NORMAL));
   }

   public PandaRenderState createRenderState() {
      return new PandaRenderState();
   }

   public void extractRenderState(final Panda entity, final PandaRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
      state.variant = entity.getVariant();
      state.isUnhappy = entity.getUnhappyCounter() > 0;
      state.isSneezing = entity.isSneezing();
      state.sneezeTime = entity.getSneezeCounter();
      state.isEating = entity.isEating();
      state.isScared = entity.isScared();
      state.isSitting = entity.isSitting();
      state.sitAmount = entity.getSitAmount(partialTicks);
      state.lieOnBackAmount = entity.getLieOnBackAmount(partialTicks);
      state.rollAmount = entity.isBaby() ? 0.0F : entity.getRollAmount(partialTicks);
      state.rollTime = entity.rollCounter > 0 ? entity.rollCounter + partialTicks : 0.0F;
   }

   protected void setupRotations(final PandaRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
      super.setupRotations(state, poseStack, bodyRot, entityScale);
      if (state.rollTime > 0.0F) {
         float rollTransitionTime = Mth.frac(state.rollTime);
         int rollPos = Mth.floor(state.rollTime);
         int nextRollPos = rollPos + 1;
         float divider = 7.0F;
         float y = state.isBaby ? 0.3F : 0.8F;
         if (rollPos < 8.0F) {
            float thisAngle = 90.0F * rollPos / 7.0F;
            float nextAngle = 90.0F * nextRollPos / 7.0F;
            float angle = this.getAngle(thisAngle, nextAngle, nextRollPos, rollTransitionTime, 8.0F);
            poseStack.translate(0.0F, (y + 0.2F) * (angle / 90.0F), 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-angle));
         } else if (rollPos < 16.0F) {
            float internalRollCounter = (rollPos - 8.0F) / 7.0F;
            float thisAngle = 90.0F + 90.0F * internalRollCounter;
            float nextAngle = 90.0F + 90.0F * (nextRollPos - 8.0F) / 7.0F;
            float angle = this.getAngle(thisAngle, nextAngle, nextRollPos, rollTransitionTime, 16.0F);
            poseStack.translate(0.0F, y + 0.2F + (y - 0.2F) * (angle - 90.0F) / 90.0F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-angle));
         } else if (rollPos < 24.0F) {
            float internalRollCounter = (rollPos - 16.0F) / 7.0F;
            float thisAngle = 180.0F + 90.0F * internalRollCounter;
            float nextAngle = 180.0F + 90.0F * (nextRollPos - 16.0F) / 7.0F;
            float angle = this.getAngle(thisAngle, nextAngle, nextRollPos, rollTransitionTime, 24.0F);
            poseStack.translate(0.0F, y + y * (270.0F - angle) / 90.0F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-angle));
         } else if (rollPos < 32) {
            float internalRollCounter = (rollPos - 24.0F) / 7.0F;
            float thisAngle = 270.0F + 90.0F * internalRollCounter;
            float nextAngle = 270.0F + 90.0F * (nextRollPos - 24.0F) / 7.0F;
            float angle = this.getAngle(thisAngle, nextAngle, nextRollPos, rollTransitionTime, 32.0F);
            poseStack.translate(0.0F, y * ((360.0F - angle) / 90.0F), 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-angle));
         }
      }

      float sitAmount = state.sitAmount;
      if (sitAmount > 0.0F) {
         poseStack.translate(0.0F, 0.8F * sitAmount, 0.0F);
         poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(sitAmount, state.xRot, state.xRot + 90.0F)));
         poseStack.translate(0.0F, -1.0F * sitAmount, 0.0F);
         if (state.isScared) {
            float shakeRot = (float)(Math.cos(state.ageInTicks * 1.25F) * Math.PI * 0.05F);
            poseStack.mulPose(Axis.YP.rotationDegrees(shakeRot));
            if (state.isBaby) {
               poseStack.translate(0.0F, 0.8F, 0.55F);
            }
         }
      }

      float lieOnBackAmount = state.lieOnBackAmount;
      if (lieOnBackAmount > 0.0F) {
         float y = state.isBaby ? 0.5F : 1.3F;
         poseStack.translate(0.0F, y * lieOnBackAmount, 0.0F);
         poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(lieOnBackAmount, state.xRot, state.xRot + 180.0F)));
      }
   }

   private float getAngle(final float thisAngle, final float nextAngle, final int nextRollPos, final float rollTransitionTime, final float threshold) {
      return nextRollPos < threshold ? Mth.lerp(rollTransitionTime, thisAngle, nextAngle) : thisAngle;
   }
}
