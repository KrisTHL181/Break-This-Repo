package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.frog.FrogModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.FrogRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FrogRenderer extends MobRenderer<Frog, FrogRenderState, FrogModel> {
   public FrogRenderer(EntityRendererProvider.Context p_234619_) {
      super(p_234619_, new FrogModel(p_234619_.bakeLayer(ModelLayers.FROG)), 0.3F);
   }

   public Identifier getTextureLocation(FrogRenderState p_365343_) {
      return p_365343_.texture;
   }

   public FrogRenderState createRenderState() {
      return new FrogRenderState();
   }

   public void extractRenderState(Frog p_362929_, FrogRenderState p_369193_, float p_361583_) {
      super.extractRenderState(p_362929_, p_369193_, p_361583_);
      p_369193_.isSwimming = p_362929_.isInWater();
      p_369193_.jumpAnimationState.copyFrom(p_362929_.jumpAnimationState);
      p_369193_.croakAnimationState.copyFrom(p_362929_.croakAnimationState);
      p_369193_.tongueAnimationState.copyFrom(p_362929_.tongueAnimationState);
      p_369193_.swimIdleAnimationState.copyFrom(p_362929_.swimIdleAnimationState);
      p_369193_.texture = p_362929_.getVariant().value().assetInfo().texturePath();
   }
}
