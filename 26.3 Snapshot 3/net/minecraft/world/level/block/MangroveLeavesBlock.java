package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MangroveLeavesBlock extends TintedParticleLeavesBlock implements BonemealableBlock {
   public MangroveLeavesBlock(final float leafParticleChance, final BlockBehaviour.Properties properties) {
      super(leafParticleChance, properties);
   }

   @Override
   public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
      return level.getBlockState(pos.below()).isAir();
   }

   @Override
   public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
      return true;
   }

   @Override
   public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
      level.setBlock(pos.below(), MangrovePropaguleBlock.createNewHangingPropagule(), 2);
   }

   @Override
   public BlockPos getParticlePos(final BlockPos blockPos) {
      return blockPos.below();
   }
}
