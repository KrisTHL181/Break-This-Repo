package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class TintedParticleLeavesBlock extends FallingParticlesLeavesBlock {
   public TintedParticleLeavesBlock(final float leafParticleChance, final BlockBehaviour.Properties properties) {
      super(leafParticleChance, AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties);
   }

   @Override
   protected void spawnFallingLeavesParticle(final Level level, final BlockPos pos, final RandomSource random) {
      ColorParticleOption particle = ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, level.getClientLeafTintColor(pos));
      ParticleUtils.spawnParticleBelow(level, pos, random, particle);
   }
}
