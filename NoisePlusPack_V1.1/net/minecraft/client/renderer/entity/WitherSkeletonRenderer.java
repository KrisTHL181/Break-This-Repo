package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitherSkeletonRenderer extends AbstractSkeletonRenderer<WitherSkeleton, SkeletonRenderState> {
   private static final Identifier WITHER_SKELETON_LOCATION = Identifier.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png");

   public WitherSkeletonRenderer(EntityRendererProvider.Context p_174447_) {
      super(p_174447_, ModelLayers.WITHER_SKELETON, ModelLayers.WITHER_SKELETON_ARMOR);
   }

   public Identifier getTextureLocation(SkeletonRenderState p_455310_) {
      return WITHER_SKELETON_LOCATION;
   }

   public SkeletonRenderState createRenderState() {
      return new SkeletonRenderState();
   }
}
