package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class FallingDustParticle extends SingleQuadParticle {
   private final float rotSpeed;
   private final SpriteSet sprites;

   private FallingDustParticle(
      final ClientLevel level, final double x, final double y, final double z, final float r, final float g, final float b, final SpriteSet sprites
   ) {
      super(level, x, y, z, sprites.first());
      this.sprites = sprites;
      this.rCol = r;
      this.gCol = g;
      this.bCol = b;
      float scale = 0.9F;
      this.quadSize *= 0.67499995F;
      int baseLifetime = (int)(32.0 / (this.random.nextFloat() * 0.8 + 0.2));
      this.lifetime = (int)Math.max(baseLifetime * 0.9F, 1.0F);
      this.setSpriteFromAge(sprites);
      this.rotSpeed = (this.random.nextFloat() - 0.5F) * 0.1F;
      this.roll = this.random.nextFloat() * (float) (Math.PI * 2);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.OPAQUE;
   }

   @Override
   public float getQuadSize(final float a) {
      return this.quadSize * Mth.clamp((this.age + a) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.setSpriteFromAge(this.sprites);
         this.oRoll = this.roll;
         this.roll = this.roll + (float) Math.PI * this.rotSpeed * 2.0F;
         if (this.onGround) {
            this.oRoll = this.roll = 0.0F;
         }

         this.move(this.xd, this.yd, this.zd);
         this.yd -= 0.003F;
         this.yd = Math.max(this.yd, -0.14F);
      }
   }

   public static class Provider implements ParticleProvider<BlockParticleOption> {
      private final SpriteSet sprite;

      public Provider(final SpriteSet sprite) {
         this.sprite = sprite;
      }

      public @Nullable Particle createParticle(
         final BlockParticleOption options,
         final ClientLevel level,
         final double x,
         final double y,
         final double z,
         final double xAux,
         final double yAux,
         final double zAux,
         final RandomSource random
      ) {
         BlockState blockState = options.getState();
         if (!blockState.isAir() && blockState.getRenderShape() == RenderShape.INVISIBLE) {
            return null;
         }

         BlockPos pos = BlockPos.containing(x, y, z);
         int tintColor;
         if (blockState.getBlock() instanceof FallingBlock fallingBlock) {
            tintColor = fallingBlock.getDustColor(blockState, level, pos);
         } else {
            BlockTintSource tintSource = Minecraft.getInstance().getBlockColors().getTintSource(blockState, 0);
            if (tintSource != null) {
               tintColor = tintSource.colorAsTerrainParticle(blockState, level, pos);
            } else {
               tintColor = blockState.getMapColor(level, pos).col;
            }
         }

         float r = (tintColor >> 16 & 0xFF) / 255.0F;
         float g = (tintColor >> 8 & 0xFF) / 255.0F;
         float b = (tintColor & 0xFF) / 255.0F;
         return new FallingDustParticle(level, x, y, z, r, g, b, this.sprite);
      }
   }
}
