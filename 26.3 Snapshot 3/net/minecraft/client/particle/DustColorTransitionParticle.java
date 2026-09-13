package net.minecraft.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

public class DustColorTransitionParticle extends DustParticleBase<DustColorTransitionOptions> {
   private final Vector3f fromColor;
   private final Vector3f toColor;

   protected DustColorTransitionParticle(
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final double xAux,
      final double yAux,
      final double zAux,
      final DustColorTransitionOptions options,
      final SpriteSet sprites
   ) {
      super(level, x, y, z, xAux, yAux, zAux, options, sprites);
      float baseFactor = this.random.nextFloat() * 0.4F + 0.6F;
      this.fromColor = this.randomizeColor(options.getFromColor(), baseFactor);
      this.toColor = this.randomizeColor(options.getToColor(), baseFactor);
   }

   private Vector3f randomizeColor(final Vector3f color, final float baseFactor) {
      return new Vector3f(this.randomizeColor(color.x(), baseFactor), this.randomizeColor(color.y(), baseFactor), this.randomizeColor(color.z(), baseFactor));
   }

   private void lerpColors(final float partialTickTime) {
      float a = (this.age + partialTickTime) / (this.lifetime + 1.0F);
      Vector3f lerpedColor = new Vector3f(this.fromColor).lerp(this.toColor, a);
      this.rCol = lerpedColor.x();
      this.gCol = lerpedColor.y();
      this.bCol = lerpedColor.z();
   }

   @Override
   public void extract(final QuadParticleRenderState particleTypeRenderState, final Camera camera, final float partialTickTime) {
      this.lerpColors(partialTickTime);
      super.extract(particleTypeRenderState, camera, partialTickTime);
   }

   public static class Provider implements ParticleProvider<DustColorTransitionOptions> {
      private final SpriteSet sprites;

      public Provider(final SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(
         final DustColorTransitionOptions options,
         final ClientLevel level,
         final double x,
         final double y,
         final double z,
         final double xAux,
         final double yAux,
         final double zAux,
         final RandomSource random
      ) {
         return new DustColorTransitionParticle(level, x, y, z, xAux, yAux, zAux, options, this.sprites);
      }
   }
}
