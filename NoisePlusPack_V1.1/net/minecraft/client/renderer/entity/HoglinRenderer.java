package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.HoglinRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HoglinRenderer extends AbstractHoglinRenderer<Hoglin> {
   private static final Identifier HOGLIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/hoglin/hoglin.png");

   public HoglinRenderer(EntityRendererProvider.Context p_174165_) {
      super(p_174165_, ModelLayers.HOGLIN, ModelLayers.HOGLIN_BABY, 0.7F);
   }

   public Identifier getTextureLocation(HoglinRenderState p_368945_) {
      return HOGLIN_LOCATION;
   }

   public void extractRenderState(Hoglin p_368627_, HoglinRenderState p_365127_, float p_365776_) {
      super.extractRenderState(p_368627_, p_365127_, p_365776_);
      p_365127_.isConverting = p_368627_.isConverting();
   }

   protected boolean isShaking(HoglinRenderState p_369897_) {
      return super.isShaking(p_369897_) || p_369897_.isConverting;
   }
}
