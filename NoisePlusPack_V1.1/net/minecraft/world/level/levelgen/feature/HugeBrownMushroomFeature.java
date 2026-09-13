package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public class HugeBrownMushroomFeature extends AbstractHugeMushroomFeature {
   public HugeBrownMushroomFeature(Codec<HugeMushroomFeatureConfiguration> p_65879_) {
      super(p_65879_);
   }

   @Override
   protected void makeCap(
      LevelAccessor p_225043_,
      RandomSource p_225044_,
      BlockPos p_225045_,
      int p_225046_,
      BlockPos.MutableBlockPos p_225047_,
      HugeMushroomFeatureConfiguration p_225048_
   ) {
      int i = p_225048_.foliageRadius;

      for (int j = -i; j <= i; j++) {
         for (int k = -i; k <= i; k++) {
            boolean flag = j == -i;
            boolean flag1 = j == i;
            boolean flag2 = k == -i;
            boolean flag3 = k == i;
            boolean flag4 = flag || flag1;
            boolean flag5 = flag2 || flag3;
            if (!flag4 || !flag5) {
               p_225047_.setWithOffset(p_225045_, j, p_225046_, k);
               boolean flag6 = flag || flag5 && j == 1 - i;
               boolean flag7 = flag1 || flag5 && j == i - 1;
               boolean flag8 = flag2 || flag4 && k == 1 - i;
               boolean flag9 = flag3 || flag4 && k == i - 1;
               BlockState blockstate = p_225048_.capProvider.getState(p_225044_, p_225045_);
               if (blockstate.hasProperty(HugeMushroomBlock.WEST)
                  && blockstate.hasProperty(HugeMushroomBlock.EAST)
                  && blockstate.hasProperty(HugeMushroomBlock.NORTH)
                  && blockstate.hasProperty(HugeMushroomBlock.SOUTH)) {
                  blockstate = blockstate.setValue(HugeMushroomBlock.WEST, flag6)
                     .setValue(HugeMushroomBlock.EAST, flag7)
                     .setValue(HugeMushroomBlock.NORTH, flag8)
                     .setValue(HugeMushroomBlock.SOUTH, flag9);
               }

               this.placeMushroomBlock(p_225043_, p_225047_, blockstate);
            }
         }
      }
   }

   @Override
   protected int getTreeRadiusForHeight(int p_65881_, int p_65882_, int p_65883_, int p_65884_) {
      return p_65884_ <= 3 ? 0 : p_65883_;
   }
}
