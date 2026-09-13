package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class CryingObsidianBlock extends Block {
   public CryingObsidianBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      if (random.nextInt(5) == 0) {
         Direction dir = Direction.getRandom(random);
         if (dir != Direction.UP) {
            BlockPos relativePos = pos.relative(dir);
            BlockState blockState = level.getBlockState(relativePos);
            if (!state.canOcclude() || !blockState.isFaceSturdy(level, relativePos, dir.getOpposite())) {
               double xOffset = dir.getStepX() == 0 ? random.nextDouble() : 0.5 + dir.getStepX() * 0.6;
               double yOffset = dir.getStepY() == 0 ? random.nextDouble() : 0.5 + dir.getStepY() * 0.6;
               double zOffset = dir.getStepZ() == 0 ? random.nextDouble() : 0.5 + dir.getStepZ() * 0.6;
               level.addParticle(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, pos.getX() + xOffset, pos.getY() + yOffset, pos.getZ() + zOffset, 0.0, 0.0, 0.0);
            }
         }
      }
   }
}
