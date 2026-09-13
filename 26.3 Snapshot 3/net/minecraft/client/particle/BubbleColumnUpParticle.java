package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;

public class BubbleColumnUpParticle extends SingleQuadParticle {
   private BubbleColumnUpParticle(
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final double xa,
      final double ya,
      final double za,
      final TextureAtlasSprite sprite
   ) {
      super(level, x, y, z, sprite);
      this.gravity = -0.125F;
      this.friction = 0.85F;
      this.setSize(0.02F, 0.02F);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
      this.xd = xa * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
      this.yd = ya * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
      this.zd = za * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
      this.lifetime = (int)(40.0 / (this.random.nextFloat() * 0.8 + 0.2));
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.removed && !this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).is(FluidTags.WATER)) {
         this.remove();
      }
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
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
         return new BubbleColumnUpParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random));
      }
   }
}
