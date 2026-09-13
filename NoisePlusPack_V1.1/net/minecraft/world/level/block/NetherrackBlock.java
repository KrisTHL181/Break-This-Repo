package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class NetherrackBlock extends Block implements BonemealableBlock {
   public static final MapCodec<NetherrackBlock> CODEC = simpleCodec(NetherrackBlock::new);

   @Override
   public MapCodec<NetherrackBlock> codec() {
      return CODEC;
   }

   public NetherrackBlock(BlockBehaviour.Properties p_54995_) {
      super(p_54995_);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_256620_, BlockPos p_55003_, BlockState p_55004_) {
      if (!p_256620_.getBlockState(p_55003_.above()).propagatesSkylightDown()) {
         return false;
      }

      for (BlockPos blockpos : BlockPos.betweenClosed(p_55003_.offset(-1, -1, -1), p_55003_.offset(1, 1, 1))) {
         if (p_256620_.getBlockState(blockpos).is(BlockTags.NYLIUM)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean isBonemealSuccess(Level p_221816_, RandomSource p_221817_, BlockPos p_221818_, BlockState p_221819_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_221811_, RandomSource p_221812_, BlockPos p_221813_, BlockState p_221814_) {
      boolean flag = false;
      boolean flag1 = false;

      for (BlockPos blockpos : BlockPos.betweenClosed(p_221813_.offset(-1, -1, -1), p_221813_.offset(1, 1, 1))) {
         BlockState blockstate = p_221811_.getBlockState(blockpos);
         if (blockstate.is(Blocks.WARPED_NYLIUM)) {
            flag1 = true;
         }

         if (blockstate.is(Blocks.CRIMSON_NYLIUM)) {
            flag = true;
         }

         if (flag1 && flag) {
            break;
         }
      }

      if (flag1 && flag) {
         p_221811_.setBlock(p_221813_, p_221812_.nextBoolean() ? Blocks.WARPED_NYLIUM.defaultBlockState() : Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
      } else if (flag1) {
         p_221811_.setBlock(p_221813_, Blocks.WARPED_NYLIUM.defaultBlockState(), 3);
      } else if (flag) {
         p_221811_.setBlock(p_221813_, Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
      }
   }

   @Override
   public BonemealableBlock.Type getType() {
      return BonemealableBlock.Type.NEIGHBOR_SPREADER;
   }
}
