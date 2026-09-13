package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.breeze.BreezeModel;
import net.minecraft.client.renderer.entity.layers.BreezeEyesLayer;
import net.minecraft.client.renderer.entity.layers.BreezeWindLayer;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.breeze.Breeze;

public class BreezeRenderer extends MobRenderer<Breeze, BreezeRenderState, BreezeModel> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/breeze/breeze.png");

   public BreezeRenderer(final EntityRendererProvider.Context context) {
      super(context, new BreezeModel(context.bakeLayer(ModelLayers.BREEZE)), 0.5F);
      this.addLayer(new BreezeWindLayer(this, context.getModelSet()));
      this.addLayer(new BreezeEyesLayer(this, context.getModelSet()));
   }

   public Identifier getTextureLocation(final BreezeRenderState state) {
      return TEXTURE_LOCATION;
   }

   public BreezeRenderState createRenderState() {
      return new BreezeRenderState();
   }

   public void extractRenderState(final Breeze entity, final BreezeRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.idle.copyFrom(entity.idle);
      state.shoot.copyFrom(entity.shoot);
      state.slide.copyFrom(entity.slide);
      state.slideBack.copyFrom(entity.slideBack);
      state.inhale.copyFrom(entity.inhale);
      state.longJump.copyFrom(entity.longJump);
   }
}
