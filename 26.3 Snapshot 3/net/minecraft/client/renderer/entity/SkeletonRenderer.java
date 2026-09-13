package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.skeleton.Skeleton;

public class SkeletonRenderer extends AbstractSkeletonRenderer<Skeleton, SkeletonRenderState> {
   private static final Identifier SKELETON_LOCATION = Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

   public SkeletonRenderer(final EntityRendererProvider.Context context) {
      super(context, ModelLayers.SKELETON, ModelLayers.SKELETON_ARMOR);
   }

   public Identifier getTextureLocation(final SkeletonRenderState state) {
      return SKELETON_LOCATION;
   }

   public SkeletonRenderState createRenderState() {
      return new SkeletonRenderState();
   }
}
