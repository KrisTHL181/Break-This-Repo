package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.GeyserParticleOptions;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class GeyserPlumeParticle extends SingleQuadParticle {
   private static final float NO_FRICTION = 1.0F;
   private static final float MAX_FRICTION = 0.0F;
   private static final float INITIAL_PROPULSION_FACTOR = 1.45F;
   private static final float GRAVITY_EXPONENT = 3.0F;
   private static final float GRADUAL_GRAVITY_FACTOR = 0.12F;
   private static final float INITIAL_SPRAY_SPREAD = 0.2F;
   private static final float LINEAR_SPRAY_SPREAD = 0.2F;
   private static final int REMAINING_FRAMES_AFTER_REACHING_MAX_HEIGHT = 5;
   private static final float MIN_SCALE_FACTOR = 2.0F;
   private static final float MAX_SCALE_FACTOR = 3.0F;
   private final SpriteSet sprites;
   private final double startY;
   private final double maxY;
   private final float initialPropulsion;
   private final float horizontalSprayX;
   private final float horizontalSprayZ;
   private final float minSize;
   private final float maxSize;
   private boolean done;

   private GeyserPlumeParticle(
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final double xa,
      final double ya,
      final double za,
      final GeyserParticleOptions options,
      final SpriteSet sprites
   ) {
      super(level, x, y, z, xa, ya, za, sprites.first());
      int plumeHeight = 5 * Math.max(1, options.waterBlocks());
      this.hasPhysics = true;
      this.speedUpWhenYMotionIsBlocked = true;
      this.lifetime = plumeHeight * 5;
      this.yd = 0.0;
      this.startY = y;
      this.maxY = this.startY + plumeHeight - 1.0;
      this.horizontalSprayX = (level.getRandom().nextFloat() - 0.5F) * 0.2F;
      this.horizontalSprayZ = (level.getRandom().nextFloat() - 0.5F) * 0.2F;
      this.friction = 1.0F;
      this.initialPropulsion = (options.waterBlocks() == 1 ? 1.5F : 1.0F) * plumeHeight * 1.45F;
      this.gravity = -this.initialPropulsion;
      float initiallyRandomizedSize = this.quadSize * 0.75F;
      this.minSize = initiallyRandomizedSize * (2.0F + plumeHeight / 8.0F);
      this.maxSize = initiallyRandomizedSize * (3.0F + plumeHeight / 8.0F);
      this.quadSize = this.minSize;
      this.sprites = sprites;
      this.setSpriteFromAge(sprites);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.done && (this.yd < 0.0 || this.y > this.maxY || this.y == this.yo)) {
         this.lifetime = Math.min(this.lifetime, this.age + 5);
         this.friction = 0.0F;
         this.done = true;
      }

      double yProgressLinear = Math.clamp((this.y - this.startY) / (this.maxY - this.startY), 0.0, 1.0);
      double yProgressExponential = Math.pow(yProgressLinear, 3.0);
      this.gravity = this.initialPropulsion * (float)yProgressExponential * 0.12F;
      this.xd = yProgressLinear * this.horizontalSprayX;
      this.zd = yProgressLinear * this.horizontalSprayZ;
      this.setSpriteFromAge(this.sprites);
      this.quadSize = this.minSize + (float)(yProgressLinear * (this.maxSize - this.minSize));
   }

   @Override
   protected SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   public static class Provider implements ParticleProvider<GeyserParticleOptions> {
      private final SpriteSet sprites;

      public Provider(final SpriteSet sprites) {
         this.sprites = sprites;
      }

      public @Nullable Particle createParticle(
         final GeyserParticleOptions options,
         final ClientLevel level,
         final double x,
         final double y,
         final double z,
         final double xAux,
         final double yAux,
         final double zAux,
         final RandomSource random
      ) {
         double randomX = x + (random.nextFloat() - 0.5F) * 0.2F;
         double randomY = y + random.nextFloat();
         double randomZ = z + (random.nextFloat() - 0.5F) * 0.2F;
         return new GeyserPlumeParticle(level, randomX, randomY, randomZ, xAux, yAux, zAux, options, this.sprites);
      }
   }
}
