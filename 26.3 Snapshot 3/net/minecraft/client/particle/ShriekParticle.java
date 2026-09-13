package net.minecraft.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

public class ShriekParticle extends SingleQuadParticle {
   private static final float MAGICAL_X_ROT = 1.0472F;
   private int delay;

   private ShriekParticle(final ClientLevel level, final double x, final double y, final double z, final int delay, final TextureAtlasSprite sprite) {
      super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
      this.quadSize = 0.85F;
      this.delay = delay;
      this.lifetime = 30;
      this.gravity = 0.0F;
      this.xd = 0.0;
      this.yd = 0.1;
      this.zd = 0.0;
   }

   @Override
   public float getQuadSize(final float a) {
      return this.quadSize * Mth.clamp((this.age + a) / this.lifetime * 0.75F, 0.0F, 1.0F);
   }

   @Override
   public void extract(final QuadParticleRenderState particleTypeRenderState, final Camera camera, final float partialTickTime) {
      if (this.delay <= 0) {
         this.alpha = 1.0F - Mth.clamp((this.age + partialTickTime) / this.lifetime, 0.0F, 1.0F);
         Quaternionf rotation = new Quaternionf();
         rotation.rotationX(-1.0472F);
         this.extractRotatedQuad(particleTypeRenderState, camera, rotation, partialTickTime);
         rotation.rotationYXZ((float) -Math.PI, 1.0472F, 0.0F);
         this.extractRotatedQuad(particleTypeRenderState, camera, rotation, partialTickTime);
      }
   }

   @Override
   public int getLightCoords(final float a) {
      return LightCoordsUtil.withBlock(super.getLightCoords(a), 15);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   @Override
   public void tick() {
      if (this.delay > 0) {
         this.delay--;
      } else {
         super.tick();
      }
   }

   public static class Provider implements ParticleProvider<ShriekParticleOption> {
      private final SpriteSet sprite;

      public Provider(final SpriteSet sprite) {
         this.sprite = sprite;
      }

      public Particle createParticle(
         final ShriekParticleOption options,
         final ClientLevel level,
         final double x,
         final double y,
         final double z,
         final double xAux,
         final double yAux,
         final double zAux,
         final RandomSource random
      ) {
         ShriekParticle particle = new ShriekParticle(level, x, y, z, options.getDelay(), this.sprite.get(random));
         particle.setAlpha(1.0F);
         return particle;
      }
   }
}
