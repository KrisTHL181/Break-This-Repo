package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.skeleton.BoggedModel;
import net.minecraft.client.renderer.entity.layers.SkeletonClothingLayer;
import net.minecraft.client.renderer.entity.state.BoggedRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BoggedRenderer extends AbstractSkeletonRenderer<Bogged, BoggedRenderState> {
   private static final Identifier BOGGED_SKELETON_LOCATION = Identifier.withDefaultNamespace("textures/entity/skeleton/bogged.png");
   private static final Identifier BOGGED_OUTER_LAYER_LOCATION = Identifier.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png");

   public BoggedRenderer(EntityRendererProvider.Context p_329255_) {
      super(p_329255_, ModelLayers.BOGGED_ARMOR, new BoggedModel(p_329255_.bakeLayer(ModelLayers.BOGGED)));
      this.addLayer(new SkeletonClothingLayer<>(this, p_329255_.getModelSet(), ModelLayers.BOGGED_OUTER_LAYER, BOGGED_OUTER_LAYER_LOCATION));
   }

   public Identifier getTextureLocation(BoggedRenderState p_362684_) {
      return BOGGED_SKELETON_LOCATION;
   }

   public BoggedRenderState createRenderState() {
      return new BoggedRenderState();
   }

   public void extractRenderState(Bogged p_459553_, BoggedRenderState p_363990_, float p_364913_) {
      super.extractRenderState(p_459553_, p_363990_, p_364913_);
      p_363990_.isSheared = p_459553_.isSheared();
   }
}
