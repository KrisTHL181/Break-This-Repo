package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WetSpongeBlock extends Block {
   public static final MapCodec<WetSpongeBlock> CODEC = simpleCodec(WetSpongeBlock::new);

   @Override
   public MapCodec<WetSpongeBlock> codec() {
      return CODEC;
   }

   protected WetSpongeBlock(BlockBehaviour.Properties p_58222_) {
      super(p_58222_);
   }

   @Override
   protected void onPlace(BlockState p_58229_, Level p_58230_, BlockPos p_58231_, BlockState p_58232_, boolean p_58233_) {
      if (p_58230_.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, p_58231_)) {
         p_58230_.setBlock(p_58231_, Blocks.SPONGE.defaultBlockState(), 3);
         p_58230_.levelEvent(2009, p_58231_, 0);
         p_58230_.playSound(null, p_58231_, SoundEvents.WET_SPONGE_DRIES, SoundSource.BLOCKS, 1.0F, (1.0F + p_58230_.getRandom().nextFloat() * 0.2F) * 0.7F);
      }
   }

   @Override
   public void animateTick(BlockState p_222682_, Level p_222683_, BlockPos p_222684_, RandomSource p_222685_) {
      Direction direction = Direction.getRandom(p_222685_);
      if (direction != Direction.UP) {
         BlockPos blockpos = p_222684_.relative(direction);
         BlockState blockstate = p_222683_.getBlockState(blockpos);
         if (!p_222682_.canOcclude() || !blockstate.isFaceSturdy(p_222683_, blockpos, direction.getOpposite())) {
            double d0 = p_222684_.getX();
            double d1 = p_222684_.getY();
            double d2 = p_222684_.getZ();
            if (direction == Direction.DOWN) {
               d1 -= 0.05;
               d0 += p_222685_.nextDouble();
               d2 += p_222685_.nextDouble();
            } else {
               d1 += p_222685_.nextDouble() * 0.8;
               if (direction.getAxis() == Direction.Axis.X) {
                  d2 += p_222685_.nextDouble();
                  if (direction == Direction.EAST) {
                     d0++;
                  } else {
                     d0 += 0.05;
                  }
               } else {
                  d0 += p_222685_.nextDouble();
                  if (direction == Direction.SOUTH) {
                     d2++;
                  } else {
                     d2 += 0.05;
                  }
               }
            }

            p_222683_.addParticle(ParticleTypes.DRIPPING_WATER, d0, d1, d2, 0.0, 0.0, 0.0);
         }
      }
   }
}
