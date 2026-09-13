package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class DarkOakTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<DarkOakTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_70090_ -> trunkPlacerParts(p_70090_).apply(p_70090_, DarkOakTrunkPlacer::new)
   );

   public DarkOakTrunkPlacer(int p_70077_, int p_70078_, int p_70079_) {
      super(p_70077_, p_70078_, p_70079_);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.DARK_OAK_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader p_226086_,
      BiConsumer<BlockPos, BlockState> p_226087_,
      RandomSource p_226088_,
      int p_226089_,
      BlockPos p_226090_,
      TreeConfiguration p_226091_
   ) {
      List<FoliagePlacer.FoliageAttachment> list = Lists.newArrayList();
      BlockPos blockpos = p_226090_.below();
      setDirtAt(p_226086_, p_226087_, p_226088_, blockpos, p_226091_);
      setDirtAt(p_226086_, p_226087_, p_226088_, blockpos.east(), p_226091_);
      setDirtAt(p_226086_, p_226087_, p_226088_, blockpos.south(), p_226091_);
      setDirtAt(p_226086_, p_226087_, p_226088_, blockpos.south().east(), p_226091_);
      Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(p_226088_);
      int i = p_226089_ - p_226088_.nextInt(4);
      int j = 2 - p_226088_.nextInt(3);
      int k = p_226090_.getX();
      int l = p_226090_.getY();
      int i1 = p_226090_.getZ();
      int j1 = k;
      int k1 = i1;
      int l1 = l + p_226089_ - 1;

      for (int i2 = 0; i2 < p_226089_; i2++) {
         if (i2 >= i && j > 0) {
            j1 += direction.getStepX();
            k1 += direction.getStepZ();
            j--;
         }

         int j2 = l + i2;
         BlockPos blockpos1 = new BlockPos(j1, j2, k1);
         if (TreeFeature.isAirOrLeaves(p_226086_, blockpos1)) {
            this.placeLog(p_226086_, p_226087_, p_226088_, blockpos1, p_226091_);
            this.placeLog(p_226086_, p_226087_, p_226088_, blockpos1.east(), p_226091_);
            this.placeLog(p_226086_, p_226087_, p_226088_, blockpos1.south(), p_226091_);
            this.placeLog(p_226086_, p_226087_, p_226088_, blockpos1.east().south(), p_226091_);
         }
      }

      list.add(new FoliagePlacer.FoliageAttachment(new BlockPos(j1, l1, k1), 0, true));

      for (int l2 = -1; l2 <= 2; l2++) {
         for (int i3 = -1; i3 <= 2; i3++) {
            if ((l2 < 0 || l2 > 1 || i3 < 0 || i3 > 1) && p_226088_.nextInt(3) <= 0) {
               int j3 = p_226088_.nextInt(3) + 2;

               for (int k2 = 0; k2 < j3; k2++) {
                  this.placeLog(p_226086_, p_226087_, p_226088_, new BlockPos(k + l2, l1 - k2 - 1, i1 + i3), p_226091_);
               }

               list.add(new FoliagePlacer.FoliageAttachment(new BlockPos(k + l2, l1, i1 + i3), 0, false));
            }
         }
      }

      return list;
   }
}
