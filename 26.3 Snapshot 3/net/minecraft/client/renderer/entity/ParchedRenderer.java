package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.skeleton.Parched;

public class ParchedRenderer extends AbstractSkeletonRenderer<Parched, SkeletonRenderState> {
   private static final Identifier PARCHED_SKELETON_LOCATION = Identifier.withDefaultNamespace("textures/entity/skeleton/parched.png");

   public ParchedRenderer(final EntityRendererProvider.Context context) {
      super(context, ModelLayers.PARCHED, ModelLayers.PARCHED_ARMOR);
   }

   public Identifier getTextureLocation(final SkeletonRenderState state) {
      return PARCHED_SKELETON_LOCATION;
   }

   public SkeletonRenderState createRenderState() {
      return new SkeletonRenderState();
   }
}
