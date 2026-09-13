package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public class HugeRedMushroomFeature extends AbstractHugeMushroomFeature {
   public HugeRedMushroomFeature(Codec<HugeMushroomFeatureConfiguration> p_65975_) {
      super(p_65975_);
   }

   @Override
   protected void makeCap(
      LevelAccessor p_225082_,
      RandomSource p_225083_,
      BlockPos p_225084_,
      int p_225085_,
      BlockPos.MutableBlockPos p_225086_,
      HugeMushroomFeatureConfiguration p_225087_
   ) {
      for (int i = p_225085_ - 3; i <= p_225085_; i++) {
         int j = i < p_225085_ ? p_225087_.foliageRadius : p_225087_.foliageRadius - 1;
         int k = p_225087_.foliageRadius - 2;

         for (int l = -j; l <= j; l++) {
            for (int i1 = -j; i1 <= j; i1++) {
               boolean flag = l == -j;
               boolean flag1 = l == j;
               boolean flag2 = i1 == -j;
               boolean flag3 = i1 == j;
               boolean flag4 = flag || flag1;
               boolean flag5 = flag2 || flag3;
               if (i >= p_225085_ || flag4 != flag5) {
                  p_225086_.setWithOffset(p_225084_, l, i, i1);
                  BlockState blockstate = p_225087_.capProvider.getState(p_225083_, p_225084_);
                  if (blockstate.hasProperty(HugeMushroomBlock.WEST)
                     && blockstate.hasProperty(HugeMushroomBlock.EAST)
                     && blockstate.hasProperty(HugeMushroomBlock.NORTH)
                     && blockstate.hasProperty(HugeMushroomBlock.SOUTH)
                     && blockstate.hasProperty(HugeMushroomBlock.UP)) {
                     blockstate = blockstate.setValue(HugeMushroomBlock.UP, i >= p_225085_ - 1)
                        .setValue(HugeMushroomBlock.WEST, l < -k)
                        .setValue(HugeMushroomBlock.EAST, l > k)
                        .setValue(HugeMushroomBlock.NORTH, i1 < -k)
                        .setValue(HugeMushroomBlock.SOUTH, i1 > k);
                  }

                  this.placeMushroomBlock(p_225082_, p_225086_, blockstate);
               }
            }
         }
      }
   }

   @Override
   protected int getTreeRadiusForHeight(int p_65977_, int p_65978_, int p_65979_, int p_65980_) {
      int i = 0;
      if (p_65980_ < p_65978_ && p_65980_ >= p_65978_ - 3) {
         i = p_65979_;
      } else if (p_65980_ == p_65978_) {
         i = p_65979_;
      }

      return i;
   }
}
