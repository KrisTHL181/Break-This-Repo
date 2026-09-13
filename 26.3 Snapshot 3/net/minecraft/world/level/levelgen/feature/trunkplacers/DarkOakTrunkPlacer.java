package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class DarkOakTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<DarkOakTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> trunkPlacerParts(i).apply(i, DarkOakTrunkPlacer::new));

   public DarkOakTrunkPlacer(final int baseHeight, final int heightRandA, final int heightRandB) {
      super(baseHeight, heightRandA, heightRandB);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.DARK_OAK_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      final WorldGenLevel level,
      final BiConsumer<BlockPos, BlockState> trunkSetter,
      final RandomSource random,
      final int treeHeight,
      final BlockPos origin,
      final TreeFeature tree
   ) {
      List<FoliagePlacer.FoliageAttachment> attachments = Lists.newArrayList();
      BlockPos below = origin.below();
      placeBelowTrunkBlock(level, trunkSetter, random, below, tree);
      placeBelowTrunkBlock(level, trunkSetter, random, below.east(), tree);
      placeBelowTrunkBlock(level, trunkSetter, random, below.south(), tree);
      placeBelowTrunkBlock(level, trunkSetter, random, below.south().east(), tree);
      Direction leanDirection = Direction.Plane.HORIZONTAL.getRandomDirection(random);
      int leanHeight = treeHeight - random.nextInt(4);
      int leanSteps = 2 - random.nextInt(3);
      int x = origin.getX();
      int y = origin.getY();
      int z = origin.getZ();
      int tx = x;
      int tz = z;
      int ey = y + treeHeight - 1;

      for (int dy = 0; dy < treeHeight; dy++) {
         if (dy >= leanHeight && leanSteps > 0) {
            tx += leanDirection.getStepX();
            tz += leanDirection.getStepZ();
            leanSteps--;
         }

         int yy = y + dy;
         BlockPos blockPos = new BlockPos(tx, yy, tz);
         if (TreeFeature.isAirOrLeaves(level, blockPos)) {
            this.placeLog(level, trunkSetter, random, blockPos, tree);
            this.placeLog(level, trunkSetter, random, blockPos.east(), tree);
            this.placeLog(level, trunkSetter, random, blockPos.south(), tree);
            this.placeLog(level, trunkSetter, random, blockPos.east().south(), tree);
         }
      }

      attachments.add(new FoliagePlacer.FoliageAttachment(new BlockPos(tx, ey, tz), 0, true));

      for (int ox = -1; ox <= 2; ox++) {
         for (int oz = -1; oz <= 2; oz++) {
            if ((ox < 0 || ox > 1 || oz < 0 || oz > 1) && random.nextInt(3) <= 0) {
               int length = random.nextInt(3) + 2;

               for (int branchY = 0; branchY < length; branchY++) {
                  this.placeLog(level, trunkSetter, random, new BlockPos(x + ox, ey - branchY - 1, z + oz), tree);
               }

               attachments.add(new FoliagePlacer.FoliageAttachment(new BlockPos(x + ox, ey, z + oz), 0, false));
            }
         }
      }

      return attachments;
   }
}
