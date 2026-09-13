package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CoralMushroomFeature extends CoralFeature {
   public CoralMushroomFeature(Codec<NoneFeatureConfiguration> p_65452_) {
      super(p_65452_);
   }

   @Override
   protected boolean placeFeature(LevelAccessor p_224982_, RandomSource p_224983_, BlockPos p_224984_, BlockState p_224985_) {
      int i = p_224983_.nextInt(3) + 3;
      int j = p_224983_.nextInt(3) + 3;
      int k = p_224983_.nextInt(3) + 3;
      int l = p_224983_.nextInt(3) + 1;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_224984_.mutable();

      for (int i1 = 0; i1 <= j; i1++) {
         for (int j1 = 0; j1 <= i; j1++) {
            for (int k1 = 0; k1 <= k; k1++) {
               blockpos$mutableblockpos.set(i1 + p_224984_.getX(), j1 + p_224984_.getY(), k1 + p_224984_.getZ());
               blockpos$mutableblockpos.move(Direction.DOWN, l);
               if ((i1 != 0 && i1 != j || j1 != 0 && j1 != i)
                  && (k1 != 0 && k1 != k || j1 != 0 && j1 != i)
                  && (i1 != 0 && i1 != j || k1 != 0 && k1 != k)
                  && (i1 == 0 || i1 == j || j1 == 0 || j1 == i || k1 == 0 || k1 == k)
                  && !(p_224983_.nextFloat() < 0.1F)
                  && !this.placeCoralBlock(p_224982_, p_224983_, blockpos$mutableblockpos, p_224985_)) {
               }
            }
         }
      }

      return true;
   }
}
