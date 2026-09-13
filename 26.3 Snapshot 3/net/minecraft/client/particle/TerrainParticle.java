package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class TerrainParticle extends SingleQuadParticle {
   private final SingleQuadParticle.Layer layer;
   private final BlockPos pos;
   private final float uo;
   private final float vo;

   public TerrainParticle(
      final ClientLevel level, final double x, final double y, final double z, final double xa, final double ya, final double za, final BlockState blockState
   ) {
      this(level, x, y, z, xa, ya, za, blockState, BlockPos.containing(x, y, z));
   }

   public TerrainParticle(
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final double xa,
      final double ya,
      final double za,
      final BlockState blockState,
      final BlockPos pos
   ) {
      super(level, x, y, z, xa, ya, za, Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(blockState).sprite());
      this.pos = pos;
      this.gravity = 1.0F;
      this.rCol = 0.6F;
      this.gCol = 0.6F;
      this.bCol = 0.6F;
      BlockTintSource tintSource = Minecraft.getInstance().getBlockColors().getTintSource(blockState, 0);
      if (tintSource != null) {
         int col = tintSource.colorAsTerrainParticle(blockState, level, pos);
         this.rCol *= (col >> 16 & 0xFF) / 255.0F;
         this.gCol *= (col >> 8 & 0xFF) / 255.0F;
         this.bCol *= (col & 0xFF) / 255.0F;
      }

      this.quadSize /= 2.0F;
      this.uo = this.random.nextFloat() * 3.0F;
      this.vo = this.random.nextFloat() * 3.0F;
      this.layer = SingleQuadParticle.Layer.bySprite(this.sprite);
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return this.layer;
   }

   @Override
   protected float getU0() {
      return this.sprite.getU((this.uo + 1.0F) / 4.0F);
   }

   @Override
   protected float getU1() {
      return this.sprite.getU(this.uo / 4.0F);
   }

   @Override
   protected float getV0() {
      return this.sprite.getV(this.vo / 4.0F);
   }

   @Override
   protected float getV1() {
      return this.sprite.getV((this.vo + 1.0F) / 4.0F);
   }

   private static @Nullable TerrainParticle createTerrainParticle(
      final BlockParticleOption options,
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final double xAux,
      final double yAux,
      final double zAux
   ) {
      BlockState state = options.getState();
      return !state.isAir() && !state.is(Blocks.MOVING_PISTON) && state.shouldSpawnTerrainParticles()
         ? new TerrainParticle(level, x, y, z, xAux, yAux, zAux, state)
         : null;
   }

   public static class CrumblingProvider implements ParticleProvider<BlockParticleOption> {
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
         Particle particle = TerrainParticle.createTerrainParticle(options, level, x, y, z, xAux, yAux, zAux);
         if (particle != null) {
            particle.setParticleSpeed(0.0, 0.0, 0.0);
            particle.setLifetime(random.nextInt(10) + 1);
         }

         return particle;
      }
   }

   public static class DustPillarProvider implements ParticleProvider<BlockParticleOption> {
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
         Particle particle = TerrainParticle.createTerrainParticle(options, level, x, y, z, xAux, yAux, zAux);
         if (particle != null) {
            particle.setParticleSpeed(random.nextGaussian() / 30.0, yAux + random.nextGaussian() / 2.0, random.nextGaussian() / 30.0);
            particle.setLifetime(random.nextInt(20) + 20);
         }

         return particle;
      }
   }

   public static class Provider implements ParticleProvider<BlockParticleOption> {
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
         return TerrainParticle.createTerrainParticle(options, level, x, y, z, xAux, yAux, zAux);
      }
   }
}
