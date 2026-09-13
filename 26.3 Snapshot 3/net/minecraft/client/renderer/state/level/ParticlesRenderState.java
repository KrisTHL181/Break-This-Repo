package net.minecraft.client.renderer.state.level;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;

public class ParticlesRenderState {
   public final List<ParticleGroupRenderState> particles = new ArrayList<>();

   public void reset() {
      this.particles.forEach(ParticleGroupRenderState::clear);
      this.particles.clear();
   }

   public void add(final ParticleGroupRenderState state) {
      this.particles.add(state);
   }

   public void submit(final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      for (ParticleGroupRenderState particle : this.particles) {
         particle.submit(submitNodeCollector, camera);
      }
   }
}
