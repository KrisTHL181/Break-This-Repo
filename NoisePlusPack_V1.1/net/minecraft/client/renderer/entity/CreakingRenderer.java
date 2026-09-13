package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.creaking.CreakingModel;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.CreakingRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreakingRenderer<T extends Creaking> extends MobRenderer<T, CreakingRenderState, CreakingModel> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/creaking/creaking.png");
   private static final Identifier EYES_TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/creaking/creaking_eyes.png");

   public CreakingRenderer(EntityRendererProvider.Context p_368368_) {
      super(p_368368_, new CreakingModel(p_368368_.bakeLayer(ModelLayers.CREAKING)), 0.6F);
      this.addLayer(
         new LivingEntityEmissiveLayer<>(
            this,
            p_448315_ -> EYES_TEXTURE_LOCATION,
            (p_421015_, p_421016_) -> p_421015_.eyesGlowing ? 1.0F : 0.0F,
            new CreakingModel(p_368368_.bakeLayer(ModelLayers.CREAKING_EYES)),
            RenderTypes::eyes,
            true
         )
      );
   }

   public Identifier getTextureLocation(CreakingRenderState p_451555_) {
      return TEXTURE_LOCATION;
   }

   public CreakingRenderState createRenderState() {
      return new CreakingRenderState();
   }

   public void extractRenderState(T p_366568_, CreakingRenderState p_362167_, float p_368483_) {
      super.extractRenderState(p_366568_, p_362167_, p_368483_);
      p_362167_.attackAnimationState.copyFrom(p_366568_.attackAnimationState);
      p_362167_.invulnerabilityAnimationState.copyFrom(p_366568_.invulnerabilityAnimationState);
      p_362167_.deathAnimationState.copyFrom(p_366568_.deathAnimationState);
      if (p_366568_.isTearingDown()) {
         p_362167_.deathTime = 0.0F;
         p_362167_.hasRedOverlay = false;
         p_362167_.eyesGlowing = p_366568_.hasGlowingEyes();
      } else {
         p_362167_.eyesGlowing = p_366568_.isActive();
      }

      p_362167_.canMove = p_366568_.canMove();
   }
}
