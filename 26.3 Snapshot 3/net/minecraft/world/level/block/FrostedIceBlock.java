package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class FrostedIceBlock extends IceBlock {
   public static final int MAX_AGE = 3;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
   private static final int NEIGHBORS_TO_AGE = 4;
   private static final int NEIGHBORS_TO_MELT = 2;

   public FrostedIceBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
   }

   @Override
   public void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
      level.scheduleTick(pos, this, Mth.nextInt(level.getRandom(), 60, 120));
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (random.nextInt(3) == 0 || this.fewerNeigboursThan(level, pos, 4)) {
         int brightness = level.dimension() == Level.END ? level.getBrightness(LightLayer.BLOCK, pos) : level.getMaxLocalRawBrightness(pos);
         if (brightness > 11 - state.getValue(AGE) - state.getLightDampening() && this.slightlyMelt(state, level, pos)) {
            BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

            for (Direction direction : Direction.values()) {
               neighborPos.setWithOffset(pos, direction);
               BlockState neighbour = level.getBlockState(neighborPos);
               if (neighbour.is(this) && !this.slightlyMelt(neighbour, level, neighborPos)) {
                  level.scheduleTick(neighborPos, this, Mth.nextInt(random, 20, 40));
               }
            }

            return;
         }
      }

      level.scheduleTick(pos, this, Mth.nextInt(random, 20, 40));
   }

   private boolean slightlyMelt(final BlockState state, final Level level, final BlockPos pos) {
      int age = state.getValue(AGE);
      if (age < 3) {
         level.setBlock(pos, state.setValue(AGE, age + 1), 2);
         return false;
      } else {
         this.melt(state, level, pos);
         return true;
      }
   }

   @Override
   protected void neighborChanged(
      final BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
   ) {
      if (block.defaultBlockState().is(this) && this.fewerNeigboursThan(level, pos, 2)) {
         this.melt(state, level, pos);
      }

      super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
   }

   private boolean fewerNeigboursThan(final BlockGetter level, final BlockPos pos, final int limit) {
      int result = 0;
      BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

      for (Direction direction : Direction.values()) {
         neighborPos.setWithOffset(pos, direction);
         if (level.getBlockState(neighborPos).is(this)) {
            if (++result >= limit) {
               return false;
            }
         }
      }

      return true;
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(AGE);
   }

   @Override
   protected ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state, final boolean includeData) {
      return ItemStack.EMPTY;
   }
}
