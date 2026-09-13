package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.model.animal.panda.PandaModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.PandaHoldsItemLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PandaRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PandaRenderer extends AgeableMobRenderer<Panda, PandaRenderState, PandaModel> {
   private static final Map<Panda.Gene, Identifier> TEXTURES = Maps.newEnumMap(
      Map.of(
         Panda.Gene.NORMAL,
         Identifier.withDefaultNamespace("textures/entity/panda/panda.png"),
         Panda.Gene.LAZY,
         Identifier.withDefaultNamespace("textures/entity/panda/lazy_panda.png"),
         Panda.Gene.WORRIED,
         Identifier.withDefaultNamespace("textures/entity/panda/worried_panda.png"),
         Panda.Gene.PLAYFUL,
         Identifier.withDefaultNamespace("textures/entity/panda/playful_panda.png"),
         Panda.Gene.BROWN,
         Identifier.withDefaultNamespace("textures/entity/panda/brown_panda.png"),
         Panda.Gene.WEAK,
         Identifier.withDefaultNamespace("textures/entity/panda/weak_panda.png"),
         Panda.Gene.AGGRESSIVE,
         Identifier.withDefaultNamespace("textures/entity/panda/aggressive_panda.png")
      )
   );

   public PandaRenderer(EntityRendererProvider.Context p_174334_) {
      super(p_174334_, new PandaModel(p_174334_.bakeLayer(ModelLayers.PANDA)), new PandaModel(p_174334_.bakeLayer(ModelLayers.PANDA_BABY)), 0.9F);
      this.addLayer(new PandaHoldsItemLayer(this));
   }

   public Identifier getTextureLocation(PandaRenderState p_457515_) {
      return TEXTURES.getOrDefault(p_457515_.variant, TEXTURES.get(Panda.Gene.NORMAL));
   }

   public PandaRenderState createRenderState() {
      return new PandaRenderState();
   }

   public void extractRenderState(Panda p_458369_, PandaRenderState p_365611_, float p_368259_) {
      super.extractRenderState(p_458369_, p_365611_, p_368259_);
      HoldingEntityRenderState.extractHoldingEntityRenderState(p_458369_, p_365611_, this.itemModelResolver);
      p_365611_.variant = p_458369_.getVariant();
      p_365611_.isUnhappy = p_458369_.getUnhappyCounter() > 0;
      p_365611_.isSneezing = p_458369_.isSneezing();
      p_365611_.sneezeTime = p_458369_.getSneezeCounter();
      p_365611_.isEating = p_458369_.isEating();
      p_365611_.isScared = p_458369_.isScared();
      p_365611_.isSitting = p_458369_.isSitting();
      p_365611_.sitAmount = p_458369_.getSitAmount(p_368259_);
      p_365611_.lieOnBackAmount = p_458369_.getLieOnBackAmount(p_368259_);
      p_365611_.rollAmount = p_458369_.isBaby() ? 0.0F : p_458369_.getRollAmount(p_368259_);
      p_365611_.rollTime = p_458369_.rollCounter > 0 ? p_458369_.rollCounter + p_368259_ : 0.0F;
   }

   protected void setupRotations(PandaRenderState p_367375_, PoseStack p_115642_, float p_115643_, float p_115644_) {
      super.setupRotations(p_367375_, p_115642_, p_115643_, p_115644_);
      if (p_367375_.rollTime > 0.0F) {
         float f = Mth.frac(p_367375_.rollTime);
         int i = Mth.floor(p_367375_.rollTime);
         int j = i + 1;
         float f1 = 7.0F;
         float f2 = p_367375_.isBaby ? 0.3F : 0.8F;
         if (i < 8.0F) {
            float f4 = 90.0F * i / 7.0F;
            float f5 = 90.0F * j / 7.0F;
            float f3 = this.getAngle(f4, f5, j, f, 8.0F);
            p_115642_.translate(0.0F, (f2 + 0.2F) * (f3 / 90.0F), 0.0F);
            p_115642_.mulPose(Axis.XP.rotationDegrees(-f3));
         } else if (i < 16.0F) {
            float f14 = (i - 8.0F) / 7.0F;
            float f17 = 90.0F + 90.0F * f14;
            float f6 = 90.0F + 90.0F * (j - 8.0F) / 7.0F;
            float f11 = this.getAngle(f17, f6, j, f, 16.0F);
            p_115642_.translate(0.0F, f2 + 0.2F + (f2 - 0.2F) * (f11 - 90.0F) / 90.0F, 0.0F);
            p_115642_.mulPose(Axis.XP.rotationDegrees(-f11));
         } else if (i < 24.0F) {
            float f15 = (i - 16.0F) / 7.0F;
            float f18 = 180.0F + 90.0F * f15;
            float f20 = 180.0F + 90.0F * (j - 16.0F) / 7.0F;
            float f12 = this.getAngle(f18, f20, j, f, 24.0F);
            p_115642_.translate(0.0F, f2 + f2 * (270.0F - f12) / 90.0F, 0.0F);
            p_115642_.mulPose(Axis.XP.rotationDegrees(-f12));
         } else if (i < 32) {
            float f16 = (i - 24.0F) / 7.0F;
            float f19 = 270.0F + 90.0F * f16;
            float f21 = 270.0F + 90.0F * (j - 24.0F) / 7.0F;
            float f13 = this.getAngle(f19, f21, j, f, 32.0F);
            p_115642_.translate(0.0F, f2 * ((360.0F - f13) / 90.0F), 0.0F);
            p_115642_.mulPose(Axis.XP.rotationDegrees(-f13));
         }
      }

      float f7 = p_367375_.sitAmount;
      if (f7 > 0.0F) {
         p_115642_.translate(0.0F, 0.8F * f7, 0.0F);
         p_115642_.mulPose(Axis.XP.rotationDegrees(Mth.lerp(f7, p_367375_.xRot, p_367375_.xRot + 90.0F)));
         p_115642_.translate(0.0F, -1.0F * f7, 0.0F);
         if (p_367375_.isScared) {
            float f8 = (float)(Math.cos(p_367375_.ageInTicks * 1.25F) * Math.PI * 0.05F);
            p_115642_.mulPose(Axis.YP.rotationDegrees(f8));
            if (p_367375_.isBaby) {
               p_115642_.translate(0.0F, 0.8F, 0.55F);
            }
         }
      }

      float f9 = p_367375_.lieOnBackAmount;
      if (f9 > 0.0F) {
         float f10 = p_367375_.isBaby ? 0.5F : 1.3F;
         p_115642_.translate(0.0F, f10 * f9, 0.0F);
         p_115642_.mulPose(Axis.XP.rotationDegrees(Mth.lerp(f9, p_367375_.xRot, p_367375_.xRot + 180.0F)));
      }
   }

   private float getAngle(float p_115625_, float p_115626_, int p_115627_, float p_115628_, float p_115629_) {
      return p_115627_ < p_115629_ ? Mth.lerp(p_115628_, p_115625_, p_115626_) : p_115625_;
   }
}
