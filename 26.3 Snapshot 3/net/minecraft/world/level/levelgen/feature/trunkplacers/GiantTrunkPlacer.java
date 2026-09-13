package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class GiantTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<GiantTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i -> trunkPlacerParts(i).apply(i, GiantTrunkPlacer::new));

   public GiantTrunkPlacer(final int baseHeight, final int heightRandA, final int heightRandB) {
      super(baseHeight, heightRandA, heightRandB);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.GIANT_TRUNK_PLACER;
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
      BlockPos below = origin.below();
      placeBelowTrunkBlock(level, trunkSetter, random, below, tree);
      placeBelowTrunkBlock(level, trunkSetter, random, below.east(), tree);
      placeBelowTrunkBlock(level, trunkSetter, random, below.south(), tree);
      placeBelowTrunkBlock(level, trunkSetter, random, below.south().east(), tree);
      BlockPos.MutableBlockPos trunkPos = new BlockPos.MutableBlockPos();

      for (int hh = 0; hh < treeHeight; hh++) {
         this.placeLogIfFreeWithOffset(level, trunkSetter, random, trunkPos, tree, origin, 0, hh, 0);
         if (hh < treeHeight - 1) {
            this.placeLogIfFreeWithOffset(level, trunkSetter, random, trunkPos, tree, origin, 1, hh, 0);
            this.placeLogIfFreeWithOffset(level, trunkSetter, random, trunkPos, tree, origin, 1, hh, 1);
            this.placeLogIfFreeWithOffset(level, trunkSetter, random, trunkPos, tree, origin, 0, hh, 1);
         }
      }

      return ImmutableList.of(new FoliagePlacer.FoliageAttachment(origin.above(treeHeight), 0, true));
   }

   private void placeLogIfFreeWithOffset(
      final WorldGenLevel level,
      final BiConsumer<BlockPos, BlockState> trunkSetter,
      final RandomSource random,
      final BlockPos.MutableBlockPos trunkPos,
      final TreeFeature tree,
      final BlockPos treePos,
      final int x,
      final int y,
      final int z
   ) {
      trunkPos.setWithOffset(treePos, x, y, z);
      this.placeLogIfFree(level, trunkSetter, random, trunkPos, tree);
   }
}
