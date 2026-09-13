package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class BendingTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<BendingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      i -> trunkPlacerParts(i)
         .and(
            i.group(
               ExtraCodecs.POSITIVE_INT.optionalFieldOf("min_height_for_leaves", 1).forGetter(c -> c.minHeightForLeaves),
               IntProviders.codec(1, 64).fieldOf("bend_length").forGetter(c -> c.bendLength)
            )
         )
         .apply(i, BendingTrunkPlacer::new)
   );
   private final int minHeightForLeaves;
   private final IntProvider bendLength;

   public BendingTrunkPlacer(final int baseHeight, final int heightRandA, final int heightRandB, final int minHeightForLeaves, final IntProvider bendLength) {
      super(baseHeight, heightRandA, heightRandB);
      this.minHeightForLeaves = minHeightForLeaves;
      this.bendLength = bendLength;
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.BENDING_TRUNK_PLACER;
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
      Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
      int logHeight = treeHeight - 1;
      BlockPos.MutableBlockPos pos = origin.mutable();
      BlockPos belowPos = pos.below();
      placeBelowTrunkBlock(level, trunkSetter, random, belowPos, tree);
      List<FoliagePlacer.FoliageAttachment> foliagePoints = Lists.newArrayList();

      for (int i = 0; i <= logHeight; i++) {
         if (i + 1 >= logHeight + random.nextInt(2)) {
            pos.move(direction);
         }

         if (TreeFeature.validTreePos(level, pos)) {
            this.placeLog(level, trunkSetter, random, pos, tree);
         }

         if (i >= this.minHeightForLeaves) {
            foliagePoints.add(new FoliagePlacer.FoliageAttachment(pos.immutable(), 0, false));
         }

         pos.move(Direction.UP);
      }

      int dirLength = this.bendLength.sample(random);

      for (int i = 0; i <= dirLength; i++) {
         if (TreeFeature.validTreePos(level, pos)) {
            this.placeLog(level, trunkSetter, random, pos, tree);
         }

         foliagePoints.add(new FoliagePlacer.FoliageAttachment(pos.immutable(), 0, false));
         pos.move(direction);
      }

      return foliagePoints;
   }
}
