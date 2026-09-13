package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.skeleton.Parched;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParchedRenderer extends AbstractSkeletonRenderer<Parched, SkeletonRenderState> {
   private static final Identifier PARCHED_SKELETON_LOCATION = Identifier.withDefaultNamespace("textures/entity/skeleton/parched.png");

   public ParchedRenderer(EntityRendererProvider.Context p_453054_) {
      super(p_453054_, ModelLayers.PARCHED, ModelLayers.PARCHED_ARMOR);
   }

   public Identifier getTextureLocation(SkeletonRenderState p_450845_) {
      return PARCHED_SKELETON_LOCATION;
   }

   public SkeletonRenderState createRenderState() {
      return new SkeletonRenderState();
   }
}
