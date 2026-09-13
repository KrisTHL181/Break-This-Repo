package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class ForkingTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<ForkingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> trunkPlacerParts(i).apply(i, ForkingTrunkPlacer::new));

   public ForkingTrunkPlacer(final int baseHeight, final int heightRandA, final int heightRandB) {
      super(baseHeight, heightRandA, heightRandB);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.FORKING_TRUNK_PLACER;
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
      placeBelowTrunkBlock(level, trunkSetter, random, origin.below(), tree);
      List<FoliagePlacer.FoliageAttachment> attachments = Lists.newArrayList();
      Direction leanDirection = Direction.Plane.HORIZONTAL.getRandomDirection(random);
      int leanHeight = treeHeight - random.nextInt(4) - 1;
      int leanSteps = 3 - random.nextInt(3);
      BlockPos.MutableBlockPos logPos = new BlockPos.MutableBlockPos();
      int tx = origin.getX();
      int tz = origin.getZ();
      OptionalInt ey = OptionalInt.empty();

      for (int yo = 0; yo < treeHeight; yo++) {
         int yy = origin.getY() + yo;
         if (yo >= leanHeight && leanSteps > 0) {
            tx += leanDirection.getStepX();
            tz += leanDirection.getStepZ();
            leanSteps--;
         }

         if (this.placeLog(level, trunkSetter, random, logPos.set(tx, yy, tz), tree)) {
            ey = OptionalInt.of(yy + 1);
         }
      }

      if (ey.isPresent()) {
         attachments.add(new FoliagePlacer.FoliageAttachment(new BlockPos(tx, ey.getAsInt(), tz), 1, false));
      }

      tx = origin.getX();
      tz = origin.getZ();
      Direction branchDirection = Direction.Plane.HORIZONTAL.getRandomDirection(random);
      if (branchDirection != leanDirection) {
         int branchPos = leanHeight - random.nextInt(2) - 1;
         int branchSteps = 1 + random.nextInt(3);
         ey = OptionalInt.empty();

         for (int yo = branchPos; yo < treeHeight && branchSteps > 0; branchSteps--) {
            if (yo >= 1) {
               int yy = origin.getY() + yo;
               tx += branchDirection.getStepX();
               tz += branchDirection.getStepZ();
               if (this.placeLog(level, trunkSetter, random, logPos.set(tx, yy, tz), tree)) {
                  ey = OptionalInt.of(yy + 1);
               }
            }

            yo++;
         }

         if (ey.isPresent()) {
            attachments.add(new FoliagePlacer.FoliageAttachment(new BlockPos(tx, ey.getAsInt(), tz), 0, false));
         }
      }

      return attachments;
   }
}
