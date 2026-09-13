package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.HoglinRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.hoglin.Hoglin;

public class HoglinRenderer extends AbstractHoglinRenderer<Hoglin> {
   private static final Identifier HOGLIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/hoglin/hoglin.png");
   private static final Identifier BABY_HOGLIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/hoglin/hoglin_baby.png");

   public HoglinRenderer(final EntityRendererProvider.Context context) {
      super(context, ModelLayers.HOGLIN, ModelLayers.HOGLIN_BABY, 0.7F);
   }

   public Identifier getTextureLocation(final HoglinRenderState state) {
      return state.isBaby ? BABY_HOGLIN_LOCATION : HOGLIN_LOCATION;
   }

   public void extractRenderState(final Hoglin entity, final HoglinRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isConverting = entity.isConverting();
   }

   protected boolean isShaking(final HoglinRenderState state) {
      return super.isShaking(state) || state.isConverting;
   }
}
