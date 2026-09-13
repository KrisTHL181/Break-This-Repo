package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MyceliumBlock extends SpreadingSnowyDirtBlock {
   public static final MapCodec<MyceliumBlock> CODEC = simpleCodec(MyceliumBlock::new);

   @Override
   public MapCodec<MyceliumBlock> codec() {
      return CODEC;
   }

   public MyceliumBlock(BlockBehaviour.Properties p_54898_) {
      super(p_54898_);
   }

   @Override
   public void animateTick(BlockState p_221789_, Level p_221790_, BlockPos p_221791_, RandomSource p_221792_) {
      super.animateTick(p_221789_, p_221790_, p_221791_, p_221792_);
      if (p_221792_.nextInt(10) == 0) {
         p_221790_.addParticle(
            ParticleTypes.MYCELIUM, p_221791_.getX() + p_221792_.nextDouble(), p_221791_.getY() + 1.1, p_221791_.getZ() + p_221792_.nextDouble(), 0.0, 0.0, 0.0
         );
      }
   }
}
