package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class GiantTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<GiantTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_70189_ -> trunkPlacerParts(p_70189_).apply(p_70189_, GiantTrunkPlacer::new)
   );

   public GiantTrunkPlacer(int p_70165_, int p_70166_, int p_70167_) {
      super(p_70165_, p_70166_, p_70167_);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.GIANT_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader p_226123_,
      BiConsumer<BlockPos, BlockState> p_226124_,
      RandomSource p_226125_,
      int p_226126_,
      BlockPos p_226127_,
      TreeConfiguration p_226128_
   ) {
      BlockPos blockpos = p_226127_.below();
      setDirtAt(p_226123_, p_226124_, p_226125_, blockpos, p_226128_);
      setDirtAt(p_226123_, p_226124_, p_226125_, blockpos.east(), p_226128_);
      setDirtAt(p_226123_, p_226124_, p_226125_, blockpos.south(), p_226128_);
      setDirtAt(p_226123_, p_226124_, p_226125_, blockpos.south().east(), p_226128_);
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (int i = 0; i < p_226126_; i++) {
         this.placeLogIfFreeWithOffset(p_226123_, p_226124_, p_226125_, blockpos$mutableblockpos, p_226128_, p_226127_, 0, i, 0);
         if (i < p_226126_ - 1) {
            this.placeLogIfFreeWithOffset(p_226123_, p_226124_, p_226125_, blockpos$mutableblockpos, p_226128_, p_226127_, 1, i, 0);
            this.placeLogIfFreeWithOffset(p_226123_, p_226124_, p_226125_, blockpos$mutableblockpos, p_226128_, p_226127_, 1, i, 1);
            this.placeLogIfFreeWithOffset(p_226123_, p_226124_, p_226125_, blockpos$mutableblockpos, p_226128_, p_226127_, 0, i, 1);
         }
      }

      return ImmutableList.of(new FoliagePlacer.FoliageAttachment(p_226127_.above(p_226126_), 0, true));
   }

   private void placeLogIfFreeWithOffset(
      LevelSimulatedReader p_226130_,
      BiConsumer<BlockPos, BlockState> p_226131_,
      RandomSource p_226132_,
      BlockPos.MutableBlockPos p_226133_,
      TreeConfiguration p_226134_,
      BlockPos p_226135_,
      int p_226136_,
      int p_226137_,
      int p_226138_
   ) {
      p_226133_.setWithOffset(p_226135_, p_226136_, p_226137_, p_226138_);
      this.placeLogIfFree(p_226130_, p_226131_, p_226132_, p_226133_, p_226134_);
   }
}
