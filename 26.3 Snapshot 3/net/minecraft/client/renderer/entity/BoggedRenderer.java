package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.skeleton.BoggedModel;
import net.minecraft.client.renderer.entity.layers.SkeletonClothingLayer;
import net.minecraft.client.renderer.entity.state.BoggedRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.skeleton.Bogged;

public class BoggedRenderer extends AbstractSkeletonRenderer<Bogged, BoggedRenderState> {
   private static final Identifier BOGGED_SKELETON_LOCATION = Identifier.withDefaultNamespace("textures/entity/skeleton/bogged.png");
   private static final Identifier BOGGED_OUTER_LAYER_LOCATION = Identifier.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png");

   public BoggedRenderer(final EntityRendererProvider.Context context) {
      super(context, ModelLayers.BOGGED_ARMOR, new BoggedModel(context.bakeLayer(ModelLayers.BOGGED)));
      this.addLayer(new SkeletonClothingLayer<>(this, context.getModelSet(), ModelLayers.BOGGED_OUTER_LAYER, BOGGED_OUTER_LAYER_LOCATION));
   }

   public Identifier getTextureLocation(final BoggedRenderState state) {
      return BOGGED_SKELETON_LOCATION;
   }

   public BoggedRenderState createRenderState() {
      return new BoggedRenderState();
   }

   public void extractRenderState(final Bogged entity, final BoggedRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isSheared = entity.isSheared();
   }
}
