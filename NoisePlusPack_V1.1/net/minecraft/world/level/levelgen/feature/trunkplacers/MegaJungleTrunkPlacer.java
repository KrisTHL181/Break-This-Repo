package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class MegaJungleTrunkPlacer extends GiantTrunkPlacer {
   public static final MapCodec<MegaJungleTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_70206_ -> trunkPlacerParts(p_70206_).apply(p_70206_, MegaJungleTrunkPlacer::new)
   );

   public MegaJungleTrunkPlacer(int p_70193_, int p_70194_, int p_70195_) {
      super(p_70193_, p_70194_, p_70195_);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.MEGA_JUNGLE_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader p_226140_,
      BiConsumer<BlockPos, BlockState> p_226141_,
      RandomSource p_226142_,
      int p_226143_,
      BlockPos p_226144_,
      TreeConfiguration p_226145_
   ) {
      List<FoliagePlacer.FoliageAttachment> list = Lists.newArrayList();
      list.addAll(super.placeTrunk(p_226140_, p_226141_, p_226142_, p_226143_, p_226144_, p_226145_));

      for (int i = p_226143_ - 2 - p_226142_.nextInt(4); i > p_226143_ / 2; i -= 2 + p_226142_.nextInt(4)) {
         float f = p_226142_.nextFloat() * (float) (Math.PI * 2);
         int j = 0;
         int k = 0;

         for (int l = 0; l < 5; l++) {
            j = (int)(1.5F + Mth.cos(f) * l);
            k = (int)(1.5F + Mth.sin(f) * l);
            BlockPos blockpos = p_226144_.offset(j, i - 3 + l / 2, k);
            this.placeLog(p_226140_, p_226141_, p_226142_, blockpos, p_226145_);
         }

         list.add(new FoliagePlacer.FoliageAttachment(p_226144_.offset(j, i, k), -2, false));
      }

      return list;
   }
}
