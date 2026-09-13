package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.warden.WardenModel;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.WardenRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WardenRenderer extends MobRenderer<Warden, WardenRenderState, WardenModel> {
   private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/warden/warden.png");
   private static final Identifier BIOLUMINESCENT_LAYER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/warden/warden_bioluminescent_layer.png");
   private static final Identifier HEART_TEXTURE = Identifier.withDefaultNamespace("textures/entity/warden/warden_heart.png");
   private static final Identifier PULSATING_SPOTS_TEXTURE_1 = Identifier.withDefaultNamespace("textures/entity/warden/warden_pulsating_spots_1.png");
   private static final Identifier PULSATING_SPOTS_TEXTURE_2 = Identifier.withDefaultNamespace("textures/entity/warden/warden_pulsating_spots_2.png");

   public WardenRenderer(EntityRendererProvider.Context p_234787_) {
      super(p_234787_, new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN)), 0.9F);
      WardenModel wardenmodel = new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN_BIOLUMINESCENT));
      WardenModel wardenmodel1 = new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN_PULSATING_SPOTS));
      WardenModel wardenmodel2 = new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN_TENDRILS));
      WardenModel wardenmodel3 = new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN_HEART));
      this.addLayer(
         new LivingEntityEmissiveLayer<>(
            this, p_448334_ -> BIOLUMINESCENT_LAYER_TEXTURE, (p_361019_, p_234810_) -> 1.0F, wardenmodel, RenderTypes::entityTranslucentEmissive, false
         )
      );
      this.addLayer(
         new LivingEntityEmissiveLayer<>(
            this,
            p_448333_ -> PULSATING_SPOTS_TEXTURE_1,
            (p_448335_, p_448336_) -> Math.max(0.0F, Mth.cos(p_448336_ * 0.045F) * 0.25F),
            wardenmodel1,
            RenderTypes::entityTranslucentEmissive,
            false
         )
      );
      this.addLayer(
         new LivingEntityEmissiveLayer<>(
            this,
            p_448340_ -> PULSATING_SPOTS_TEXTURE_2,
            (p_448337_, p_448338_) -> Math.max(0.0F, Mth.cos(p_448338_ * 0.045F + (float) Math.PI) * 0.25F),
            wardenmodel1,
            RenderTypes::entityTranslucentEmissive,
            false
         )
      );
      this.addLayer(
         new LivingEntityEmissiveLayer<>(
            this, p_448332_ -> TEXTURE, (p_358002_, p_358003_) -> p_358002_.tendrilAnimation, wardenmodel2, RenderTypes::entityTranslucentEmissive, false
         )
      );
      this.addLayer(
         new LivingEntityEmissiveLayer<>(
            this, p_448339_ -> HEART_TEXTURE, (p_358004_, p_358005_) -> p_358004_.heartAnimation, wardenmodel3, RenderTypes::entityTranslucentEmissive, false
         )
      );
   }

   public Identifier getTextureLocation(WardenRenderState p_454840_) {
      return TEXTURE;
   }

   public WardenRenderState createRenderState() {
      return new WardenRenderState();
   }

   public void extractRenderState(Warden p_363046_, WardenRenderState p_369482_, float p_368535_) {
      super.extractRenderState(p_363046_, p_369482_, p_368535_);
      p_369482_.tendrilAnimation = p_363046_.getTendrilAnimation(p_368535_);
      p_369482_.heartAnimation = p_363046_.getHeartAnimation(p_368535_);
      p_369482_.roarAnimationState.copyFrom(p_363046_.roarAnimationState);
      p_369482_.sniffAnimationState.copyFrom(p_363046_.sniffAnimationState);
      p_369482_.emergeAnimationState.copyFrom(p_363046_.emergeAnimationState);
      p_369482_.diggingAnimationState.copyFrom(p_363046_.diggingAnimationState);
      p_369482_.attackAnimationState.copyFrom(p_363046_.attackAnimationState);
      p_369482_.sonicBoomAnimationState.copyFrom(p_363046_.sonicBoomAnimationState);
   }
}
