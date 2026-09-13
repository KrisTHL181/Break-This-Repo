package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class FallingParticlesLeavesBlock extends LeavesBlock {
   protected final float leafParticleChance;

   public FallingParticlesLeavesBlock(
      final float leafParticleChance, final AmbientLeavesBlockSoundPlayer ambientLeavesBlockSoundPlayer, final BlockBehaviour.Properties properties
   ) {
      super(ambientLeavesBlockSoundPlayer, properties);
      this.leafParticleChance = leafParticleChance;
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      super.animateTick(state, level, pos, random);
      this.makeFallingLeavesParticles(level, pos, random);
   }

   private void makeFallingLeavesParticles(final Level level, final BlockPos pos, final RandomSource random) {
      BlockPos below = pos.below();
      BlockState belowState = level.getBlockState(below);
      if (!(random.nextFloat() >= this.leafParticleChance)) {
         if (!isFaceFull(belowState.getCollisionShape(level, below), Direction.UP)) {
            this.spawnFallingLeavesParticle(level, pos, random);
         }
      }
   }

   protected abstract void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random);
}
