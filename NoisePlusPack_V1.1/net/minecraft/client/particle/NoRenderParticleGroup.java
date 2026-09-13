package net.minecraft.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NoRenderParticleGroup extends ParticleGroup<NoRenderParticle> {
   private static final ParticleGroupRenderState EMPTY_RENDER_STATE = (p_427682_, p_429447_) -> {};

   public NoRenderParticleGroup(ParticleEngine p_426004_) {
      super(p_426004_);
   }

   @Override
   public ParticleGroupRenderState extractRenderState(Frustum p_426078_, Camera p_428684_, float p_426634_) {
      return EMPTY_RENDER_STATE;
   }
}
