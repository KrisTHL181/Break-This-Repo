package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.frog.FrogModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.FrogRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.frog.Frog;

public class FrogRenderer extends MobRenderer<Frog, FrogRenderState, FrogModel> {
   public FrogRenderer(final EntityRendererProvider.Context context) {
      super(context, new FrogModel(context.bakeLayer(ModelLayers.FROG)), 0.3F);
   }

   public Identifier getTextureLocation(final FrogRenderState state) {
      return state.texture;
   }

   public FrogRenderState createRenderState() {
      return new FrogRenderState();
   }

   public void extractRenderState(final Frog entity, final FrogRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isSwimming = entity.isInWater();
      state.jumpAnimationState.copyFrom(entity.jumpAnimationState);
      state.croakAnimationState.copyFrom(entity.croakAnimationState);
      state.tongueAnimationState.copyFrom(entity.tongueAnimationState);
      state.swimIdleAnimationState.copyFrom(entity.swimIdleAnimationState);
      state.texture = entity.getVariant().value().assetInfo().texturePath();
   }
}
