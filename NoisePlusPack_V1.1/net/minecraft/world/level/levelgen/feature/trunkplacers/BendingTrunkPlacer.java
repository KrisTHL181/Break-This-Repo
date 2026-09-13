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
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class BendingTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<BendingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_161786_ -> trunkPlacerParts(p_161786_)
         .and(
            p_161786_.group(
               ExtraCodecs.POSITIVE_INT.optionalFieldOf("min_height_for_leaves", 1).forGetter(p_161788_ -> p_161788_.minHeightForLeaves),
               IntProvider.codec(1, 64).fieldOf("bend_length").forGetter(p_161784_ -> p_161784_.bendLength)
            )
         )
         .apply(p_161786_, BendingTrunkPlacer::new)
   );
   private final int minHeightForLeaves;
   private final IntProvider bendLength;

   public BendingTrunkPlacer(int p_161770_, int p_161771_, int p_161772_, int p_161773_, IntProvider p_161774_) {
      super(p_161770_, p_161771_, p_161772_);
      this.minHeightForLeaves = p_161773_;
      this.bendLength = p_161774_;
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.BENDING_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader p_226079_,
      BiConsumer<BlockPos, BlockState> p_226080_,
      RandomSource p_226081_,
      int p_226082_,
      BlockPos p_226083_,
      TreeConfiguration p_226084_
   ) {
      Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(p_226081_);
      int i = p_226082_ - 1;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_226083_.mutable();
      BlockPos blockpos = blockpos$mutableblockpos.below();
      setDirtAt(p_226079_, p_226080_, p_226081_, blockpos, p_226084_);
      List<FoliagePlacer.FoliageAttachment> list = Lists.newArrayList();

      for (int j = 0; j <= i; j++) {
         if (j + 1 >= i + p_226081_.nextInt(2)) {
            blockpos$mutableblockpos.move(direction);
         }

         if (TreeFeature.validTreePos(p_226079_, blockpos$mutableblockpos)) {
            this.placeLog(p_226079_, p_226080_, p_226081_, blockpos$mutableblockpos, p_226084_);
         }

         if (j >= this.minHeightForLeaves) {
            list.add(new FoliagePlacer.FoliageAttachment(blockpos$mutableblockpos.immutable(), 0, false));
         }

         blockpos$mutableblockpos.move(Direction.UP);
      }

      int l = this.bendLength.sample(p_226081_);

      for (int k = 0; k <= l; k++) {
         if (TreeFeature.validTreePos(p_226079_, blockpos$mutableblockpos)) {
            this.placeLog(p_226079_, p_226080_, p_226081_, blockpos$mutableblockpos, p_226084_);
         }

         list.add(new FoliagePlacer.FoliageAttachment(blockpos$mutableblockpos.immutable(), 0, false));
         blockpos$mutableblockpos.move(direction);
      }

      return list;
   }
}
