package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class SpellParticle extends SingleQuadParticle {
   private static final RandomSource RANDOM = RandomSource.create();
   private final SpriteSet sprites;
   private float originalAlpha = 1.0F;

   private SpellParticle(
      final ClientLevel level, final double x, final double y, final double z, final double xa, final double ya, final double za, final SpriteSet sprites
   ) {
      super(level, x, y, z, 0.5 - RANDOM.nextDouble(), ya, 0.5 - RANDOM.nextDouble(), sprites.first());
      this.friction = 0.96F;
      this.gravity = -0.1F;
      this.speedUpWhenYMotionIsBlocked = true;
      this.sprites = sprites;
      this.yd *= 0.2F;
      if (xa == 0.0 && za == 0.0) {
         this.xd *= 0.1F;
         this.zd *= 0.1F;
      }

      this.quadSize *= 0.75F;
      this.lifetime = (int)(8.0 / (this.random.nextFloat() * 0.8 + 0.2));
      this.hasPhysics = false;
      this.setSpriteFromAge(sprites);
      if (this.isCloseToScopingPlayer()) {
         this.setAlpha(0.0F);
      }
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
      if (this.isCloseToScopingPlayer()) {
         this.alpha = 0.0F;
      } else {
         this.alpha = Mth.lerp(0.05F, this.alpha, this.originalAlpha);
      }
   }

   @Override
   protected void setAlpha(final float alpha) {
      super.setAlpha(alpha);
      this.originalAlpha = alpha;
   }

   private boolean isCloseToScopingPlayer() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      return player != null
         && player.getEyePosition().distanceToSqr(this.x, this.y, this.z) <= 9.0
         && instance.options.getCameraType().isFirstPerson()
         && player.isScoping();
   }

   public static class InstantProvider implements ParticleProvider<SpellParticleOption> {
      private final SpriteSet sprite;

      public InstantProvider(final SpriteSet sprite) {
         this.sprite = sprite;
      }

      public Particle createParticle(
         final SpellParticleOption options,
         final ClientLevel level,
         final double x,
         final double y,
         final double z,
         final double xAux,
         final double yAux,
         final double zAux,
         final RandomSource random
      ) {
         SpellParticle particle = new SpellParticle(level, x, y, z, xAux, yAux, zAux, this.sprite);
         particle.setColor(options.getRed(), options.getGreen(), options.getBlue());
         particle.setPower(options.getPower());
         return particle;
      }
   }

   public static class MobEffectProvider implements ParticleProvider<ColorParticleOption> {
      private final SpriteSet sprite;

      public MobEffectProvider(final SpriteSet sprite) {
         this.sprite = sprite;
      }

      public Particle createParticle(
         final ColorParticleOption options,
         final ClientLevel level,
         final double x,
         final double y,
         final double z,
         final double xAux,
         final double yAux,
         final double zAux,
         final RandomSource random
      ) {
         SpellParticle particle = new SpellParticle(level, x, y, z, xAux, yAux, zAux, this.sprite);
         particle.setColor(options.getRed(), options.getGreen(), options.getBlue());
         particle.setAlpha(options.getAlpha());
         return particle;
      }
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(final SpriteSet sprite) {
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
         return new SpellParticle(level, x, y, z, xAux, yAux, zAux, this.sprite);
      }
   }

   public static class WitchProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public WitchProvider(final SpriteSet sprite) {
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
         SpellParticle particle = new SpellParticle(level, x, y, z, xAux, yAux, zAux, this.sprite);
         float randBrightness = random.nextFloat() * 0.5F + 0.35F;
         particle.setColor(1.0F * randBrightness, 0.0F * randBrightness, 1.0F * randBrightness);
         return particle;
      }
   }
}
