package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class UntintedParticleLeavesBlock extends FallingParticlesLeavesBlock {
   protected final ParticleOptions leafParticle;

   public UntintedParticleLeavesBlock(
      final float leafParticleChance,
      final ParticleOptions leafParticle,
      final AmbientLeavesBlockSoundPlayer ambientLeavesBlockSoundPlayer,
      final BlockBehaviour.Properties properties
   ) {
      super(leafParticleChance, ambientLeavesBlockSoundPlayer, properties);
      this.leafParticle = leafParticle;
   }

   @Override
   protected void spawnFallingLeavesParticle(final Level level, final BlockPos pos, final RandomSource random) {
      ParticleUtils.spawnParticleBelow(level, pos, random, this.leafParticle);
   }
}
