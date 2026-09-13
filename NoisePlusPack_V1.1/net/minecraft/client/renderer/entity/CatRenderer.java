package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.feline.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.CatCollarLayer;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CatRenderer extends AgeableMobRenderer<Cat, CatRenderState, CatModel> {
   public CatRenderer(EntityRendererProvider.Context p_173943_) {
      super(p_173943_, new CatModel(p_173943_.bakeLayer(ModelLayers.CAT)), new CatModel(p_173943_.bakeLayer(ModelLayers.CAT_BABY)), 0.4F);
      this.addLayer(new CatCollarLayer(this, p_173943_.getModelSet()));
   }

   public Identifier getTextureLocation(CatRenderState p_454790_) {
      return p_454790_.texture;
   }

   public CatRenderState createRenderState() {
      return new CatRenderState();
   }

   public void extractRenderState(Cat p_460661_, CatRenderState p_369643_, float p_365598_) {
      super.extractRenderState(p_460661_, p_369643_, p_365598_);
      p_369643_.texture = p_460661_.getVariant().value().assetInfo().texturePath();
      p_369643_.isCrouching = p_460661_.isCrouching();
      p_369643_.isSprinting = p_460661_.isSprinting();
      p_369643_.isSitting = p_460661_.isInSittingPose();
      p_369643_.lieDownAmount = p_460661_.getLieDownAmount(p_365598_);
      p_369643_.lieDownAmountTail = p_460661_.getLieDownAmountTail(p_365598_);
      p_369643_.relaxStateOneAmount = p_460661_.getRelaxStateOneAmount(p_365598_);
      p_369643_.isLyingOnTopOfSleepingPlayer = p_460661_.isLyingOnTopOfSleepingPlayer();
      p_369643_.collarColor = p_460661_.isTame() ? p_460661_.getCollarColor() : null;
   }

   protected void setupRotations(CatRenderState p_364122_, PoseStack p_113945_, float p_113946_, float p_113947_) {
      super.setupRotations(p_364122_, p_113945_, p_113946_, p_113947_);
      float f = p_364122_.lieDownAmount;
      if (f > 0.0F) {
         p_113945_.translate(0.4F * f, 0.15F * f, 0.1F * f);
         p_113945_.mulPose(Axis.ZP.rotationDegrees(Mth.rotLerp(f, 0.0F, 90.0F)));
         if (p_364122_.isLyingOnTopOfSleepingPlayer) {
            p_113945_.translate(0.15F * f, 0.0F, 0.0F);
         }
      }
   }
}
