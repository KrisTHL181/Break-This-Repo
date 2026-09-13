package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Continuation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class NetherrackBlock extends Block implements BonemealableBlock {
   public NetherrackBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
      return !level.getBlockState(pos.above()).propagatesSkylightDown()
         ? false
         : level.findBlocksIn(pos.offset(-1, -1, -1), pos.offset(1, 1, 1)).filterState(blockState -> blockState.is(BlockTags.NYLIUM)).anyMatched();
   }

   @Override
   public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
      return true;
   }

   @Override
   public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
      MutableBoolean foundRed = new MutableBoolean();
      MutableBoolean foundBlue = new MutableBoolean();
      level.findBlocksIn(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))
         .filterState(blockState -> blockState.is(BlockTags.NYLIUM))
         .forEachUntil((var2, blockState) -> {
            if (blockState.is(Blocks.WARPED_NYLIUM)) {
               foundBlue.setTrue();
            } else if (blockState.is(Blocks.CRIMSON_NYLIUM)) {
               foundRed.setTrue();
            }

            return Continuation.abortIf(foundBlue.isTrue() && foundRed.isTrue());
         });
      if (foundBlue.isTrue() && foundRed.isTrue()) {
         level.setBlockAndUpdate(pos, random.nextBoolean() ? Blocks.WARPED_NYLIUM.defaultBlockState() : Blocks.CRIMSON_NYLIUM.defaultBlockState());
      } else if (foundBlue.isTrue()) {
         level.setBlockAndUpdate(pos, Blocks.WARPED_NYLIUM.defaultBlockState());
      } else if (foundRed.isTrue()) {
         level.setBlockAndUpdate(pos, Blocks.CRIMSON_NYLIUM.defaultBlockState());
      }
   }

   @Override
   public BonemealableBlock.Type getType() {
      return BonemealableBlock.Type.NEIGHBOR_SPREADER;
   }
}
