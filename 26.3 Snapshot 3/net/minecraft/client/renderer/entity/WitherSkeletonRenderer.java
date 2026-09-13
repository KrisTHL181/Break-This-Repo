package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;

public class WitherSkeletonRenderer extends AbstractSkeletonRenderer<WitherSkeleton, SkeletonRenderState> {
   private static final Identifier WITHER_SKELETON_LOCATION = Identifier.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png");

   public WitherSkeletonRenderer(final EntityRendererProvider.Context context) {
      super(context, ModelLayers.WITHER_SKELETON, ModelLayers.WITHER_SKELETON_ARMOR);
   }

   public Identifier getTextureLocation(final SkeletonRenderState state) {
      return WITHER_SKELETON_LOCATION;
   }

   public SkeletonRenderState createRenderState() {
      return new SkeletonRenderState();
   }
}
