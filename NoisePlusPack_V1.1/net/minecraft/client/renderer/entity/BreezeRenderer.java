package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.breeze.BreezeModel;
import net.minecraft.client.renderer.entity.layers.BreezeEyesLayer;
import net.minecraft.client.renderer.entity.layers.BreezeWindLayer;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreezeRenderer extends MobRenderer<Breeze, BreezeRenderState, BreezeModel> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/breeze/breeze.png");

   public BreezeRenderer(EntityRendererProvider.Context p_311628_) {
      super(p_311628_, new BreezeModel(p_311628_.bakeLayer(ModelLayers.BREEZE)), 0.5F);
      this.addLayer(new BreezeWindLayer(this, p_311628_.getModelSet()));
      this.addLayer(new BreezeEyesLayer(this, p_311628_.getModelSet()));
   }

   public Identifier getTextureLocation(BreezeRenderState p_363766_) {
      return TEXTURE_LOCATION;
   }

   public BreezeRenderState createRenderState() {
      return new BreezeRenderState();
   }

   public void extractRenderState(Breeze p_362838_, BreezeRenderState p_366825_, float p_367068_) {
      super.extractRenderState(p_362838_, p_366825_, p_367068_);
      p_366825_.idle.copyFrom(p_362838_.idle);
      p_366825_.shoot.copyFrom(p_362838_.shoot);
      p_366825_.slide.copyFrom(p_362838_.slide);
      p_366825_.slideBack.copyFrom(p_362838_.slideBack);
      p_366825_.inhale.copyFrom(p_362838_.inhale);
      p_366825_.longJump.copyFrom(p_362838_.longJump);
   }
}
