package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.creaking.CreakingModel;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.CreakingRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.creaking.Creaking;

public class CreakingRenderer<T extends Creaking> extends MobRenderer<T, CreakingRenderState, CreakingModel> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/creaking/creaking.png");
   private static final Identifier EYES_TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/creaking/creaking_eyes.png");

   public CreakingRenderer(final EntityRendererProvider.Context context) {
      super(context, new CreakingModel(context.bakeLayer(ModelLayers.CREAKING)), 0.6F);
      this.addLayer(
         new LivingEntityEmissiveLayer<>(
            this,
            renderState -> EYES_TEXTURE_LOCATION,
            (state, ageInTicks) -> state.eyesGlowing ? 1.0F : 0.0F,
            new CreakingModel(context.bakeLayer(ModelLayers.CREAKING_EYES)),
            RenderTypes::eyes,
            true
         )
      );
   }

   public Identifier getTextureLocation(final CreakingRenderState state) {
      return TEXTURE_LOCATION;
   }

   public CreakingRenderState createRenderState() {
      return new CreakingRenderState();
   }

   public void extractRenderState(final T entity, final CreakingRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.attackAnimationState.copyFrom(entity.attackAnimationState);
      state.invulnerabilityAnimationState.copyFrom(entity.invulnerabilityAnimationState);
      state.deathAnimationState.copyFrom(entity.deathAnimationState);
      if (entity.isTearingDown()) {
         state.deathTime = 0.0F;
         state.hasRedOverlay = false;
         state.eyesGlowing = entity.hasGlowingEyes();
      } else {
         state.eyesGlowing = entity.isActive();
      }

      state.canMove = entity.canMove();
   }
}
