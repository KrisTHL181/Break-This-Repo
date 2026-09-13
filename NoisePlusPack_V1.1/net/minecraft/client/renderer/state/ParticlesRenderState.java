package net.minecraft.client.renderer.state;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParticlesRenderState {
   public final List<ParticleGroupRenderState> particles = new ArrayList<>();

   public void reset() {
      this.particles.forEach(ParticleGroupRenderState::clear);
      this.particles.clear();
   }

   public void add(ParticleGroupRenderState p_424678_) {
      this.particles.add(p_424678_);
   }

   public void submit(SubmitNodeStorage p_429780_, CameraRenderState p_426160_) {
      for (ParticleGroupRenderState particlegrouprenderstate : this.particles) {
         particlegrouprenderstate.submit(p_429780_, p_426160_);
      }
   }
}
