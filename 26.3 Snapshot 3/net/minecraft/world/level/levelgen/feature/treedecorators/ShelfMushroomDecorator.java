package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShelfMushroomBlock;

public class ShelfMushroomDecorator extends TreeDecorator {
   public static final MapCodec<ShelfMushroomDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
      .fieldOf("probability")
      .xmap(ShelfMushroomDecorator::new, d -> d.placementProbability);
   private static final int MIN_HEIGHT_OFFSET = 1;
   private static final int MAX_HEIGHT_OFFSET = 4;
   private static final float PER_SIDE_PLACEMENT_CHANCE = 0.25F;
   private static final int MAX_AGE_EXCLUSIVE = 2;
   private final float placementProbability;

   public ShelfMushroomDecorator(final float probability) {
      this.placementProbability = probability;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.SHELF_MUSHROOM;
   }

   @Override
   public void place(final TreeDecorator.Context context) {
      RandomSource random = context.random();
      if (!(random.nextFloat() >= this.placementProbability)) {
         List<BlockPos> logs = context.logs();
         if (!logs.isEmpty()) {
            if (isFallenLog(logs)) {
               placeOnFallenLog(context, logs, random);
            } else {
               placeOnStandingTree(context, logs, random);
            }
         }
      }
   }

   private static void placeOnStandingTree(final TreeDecorator.Context context, final List<BlockPos> logs, final RandomSource random) {
      Direction[] directions = pickTwoPerpendicularDirections(random);
      int treeBaseY = logs.getFirst().getY();

      for (BlockPos logPos : logs) {
         if (isWithinDecoratableHeight(logPos, treeBaseY)) {
            for (Direction facing : directions) {
               if (!(random.nextFloat() > 0.25F) && tryPlaceMushroomOnStandingTree(context, logPos, facing, random)) {
                  break;
               }
            }
         }
      }
   }

   private static void placeOnFallenLog(final TreeDecorator.Context context, final List<BlockPos> logs, final RandomSource random) {
      Direction[] directions = perpendicularToFallenLog(logs);

      for (BlockPos logPos : logs) {
         for (Direction facing : directions) {
            if (!(random.nextFloat() > 0.25F)) {
               tryPlaceMushroomOnFallenTree(context, logPos, facing, random);
            }
         }
      }
   }

   private static boolean tryPlaceMushroomOnStandingTree(
      final TreeDecorator.Context context, final BlockPos logPos, final Direction facing, final RandomSource random
   ) {
      BlockPos mushroomPos = mushroomPosFor(logPos, facing);
      if (!isBlockReplaceableWithShelfMushroom(context, mushroomPos)) {
         return false;
      }

      if (hasShelfMushroomAt(context, mushroomPos.below())) {
         return false;
      }

      placeMushroom(context, mushroomPos, facing, random);
      return true;
   }

   private static void tryPlaceMushroomOnFallenTree(
      final TreeDecorator.Context context, final BlockPos logPos, final Direction facing, final RandomSource random
   ) {
      BlockPos mushroomPos = mushroomPosFor(logPos, facing);
      if (isBlockReplaceableWithShelfMushroom(context, mushroomPos)) {
         if (!hasHorizontallyAdjacentShelfMushroom(context, mushroomPos) && !hasHorizontallyAdjacentShelfMushroom(context, logPos)) {
            placeMushroom(context, mushroomPos, facing, random);
         }
      }
   }

   private static boolean isFallenLog(final List<BlockPos> logs) {
      return logs.getFirst().getY() == logs.getLast().getY();
   }

   private static Direction[] pickTwoPerpendicularDirections(final RandomSource random) {
      Direction first = Direction.Plane.HORIZONTAL.getRandomDirection(random);
      return new Direction[]{first, first.getClockWise()};
   }

   private static Direction[] perpendicularToFallenLog(final List<BlockPos> logs) {
      BlockPos first = logs.getFirst();
      BlockPos last = logs.getLast();
      Direction.Axis logAxis = first.getX() != last.getX() ? Direction.Axis.X : Direction.Axis.Z;
      return logAxis == Direction.Axis.X ? new Direction[]{Direction.NORTH, Direction.SOUTH} : new Direction[]{Direction.EAST, Direction.WEST};
   }

   private static boolean isWithinDecoratableHeight(final BlockPos pos, final int treeBaseY) {
      int dy = pos.getY() - treeBaseY;
      return dy >= 1 && dy <= 4;
   }

   private static BlockPos mushroomPosFor(final BlockPos logPos, final Direction facing) {
      return logPos.offset(facing.getStepX(), 0, facing.getStepZ());
   }

   private static void placeMushroom(final TreeDecorator.Context context, final BlockPos pos, final Direction facing, final RandomSource random) {
      context.setBlock(
         pos, Blocks.SHELF_MUSHROOM.defaultBlockState().setValue(ShelfMushroomBlock.AGE, random.nextInt(2)).setValue(ShelfMushroomBlock.FACING, facing)
      );
   }

   private static boolean hasShelfMushroomAt(final TreeDecorator.Context context, final BlockPos pos) {
      return context.checkBlock(pos, state -> state.is(Blocks.SHELF_MUSHROOM));
   }

   private static boolean hasHorizontallyAdjacentShelfMushroom(final TreeDecorator.Context context, final BlockPos pos) {
      for (Direction dir : Direction.Plane.HORIZONTAL) {
         if (hasShelfMushroomAt(context, pos.relative(dir))) {
            return true;
         }
      }

      return false;
   }

   public static boolean isBlockReplaceableWithShelfMushroom(final TreeDecorator.Context context, final BlockPos pos) {
      return context.isReplaceable(pos) && !context.isWaterOrWaterNearby(pos);
   }
}
