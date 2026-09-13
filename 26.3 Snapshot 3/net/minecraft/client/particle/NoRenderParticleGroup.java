package net.minecraft.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;

public class NoRenderParticleGroup extends ParticleGroup<NoRenderParticle> {
   private static final ParticleGroupRenderState EMPTY_RENDER_STATE = (ignored, camera) -> {};

   public NoRenderParticleGroup(final ParticleEngine engine) {
      super(engine);
   }

   @Override
   public ParticleGroupRenderState extractRenderState(final Frustum frustum, final Camera camera, final float partialTickTime) {
      return EMPTY_RENDER_STATE;
   }
}
