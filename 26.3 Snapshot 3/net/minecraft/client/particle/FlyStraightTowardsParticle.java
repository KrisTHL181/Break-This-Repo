package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class FlyStraightTowardsParticle extends SingleQuadParticle {
   private final double xStart;
   private final double yStart;
   private final double zStart;
   private final int startColor;
   private final int endColor;

   private FlyStraightTowardsParticle(
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final double xd,
      final double yd,
      final double zd,
      final int startColor,
      final int endColor,
      final TextureAtlasSprite sprite
   ) {
      super(level, x, y, z, sprite);
      this.xd = xd;
      this.yd = yd;
      this.zd = zd;
      this.xStart = x;
      this.yStart = y;
      this.zStart = z;
      this.xo = x + xd;
      this.yo = y + yd;
      this.zo = z + zd;
      this.x = this.xo;
      this.y = this.yo;
      this.z = this.zo;
      this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.2F);
      this.hasPhysics = false;
      this.lifetime = (int)(this.random.nextFloat() * 5.0F) + 25;
      this.startColor = startColor;
      this.endColor = endColor;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public void move(final double xa, final double ya, final double za) {
   }

   @Override
   public int getLightCoords(final float a) {
      return LightCoordsUtil.withBlock(super.getLightCoords(a), 15);
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float normalizedAge = (float)this.age / this.lifetime;
         float posAlpha = 1.0F - normalizedAge;
         this.x = this.xStart + this.xd * posAlpha;
         this.y = this.yStart + this.yd * posAlpha;
         this.z = this.zStart + this.zd * posAlpha;
         int color = ARGB.srgbLerp(normalizedAge, this.startColor, this.endColor);
         this.setColor(ARGB.red(color) / 255.0F, ARGB.green(color) / 255.0F, ARGB.blue(color) / 255.0F);
         this.setAlpha(ARGB.alpha(color) / 255.0F);
      }
   }

   public static class OminousSpawnProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public OminousSpawnProvider(final SpriteSet sprite) {
         this.sprite = sprite;
      }

      public Particle createParticle(
         final SimpleParticleType options,
         final ClientLevel level,
         final double x,
         final double y,
         final double z,
         final double xAux,
         final double yAux,
         final double zAux,
         final RandomSource random
      ) {
         FlyStraightTowardsParticle particle = new FlyStraightTowardsParticle(level, x, y, z, xAux, yAux, zAux, -12210434, -1, this.sprite.get(random));
         particle.scale(Mth.randomBetween(level.getRandom(), 3.0F, 5.0F));
         return particle;
      }
   }
}
