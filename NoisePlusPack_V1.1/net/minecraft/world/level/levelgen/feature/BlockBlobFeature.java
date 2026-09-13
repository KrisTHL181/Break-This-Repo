package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;

public class BlockBlobFeature extends Feature<BlockStateConfiguration> {
   public BlockBlobFeature(Codec<BlockStateConfiguration> p_65248_) {
      super(p_65248_);
   }

   @Override
   public boolean place(FeaturePlaceContext<BlockStateConfiguration> p_159471_) {
      BlockPos blockpos = p_159471_.origin();
      WorldGenLevel worldgenlevel = p_159471_.level();
      RandomSource randomsource = p_159471_.random();
      BlockStateConfiguration blockstateconfiguration = p_159471_.config();

      while (blockpos.getY() > worldgenlevel.getMinY() + 3) {
         if (!worldgenlevel.isEmptyBlock(blockpos.below())) {
            BlockState blockstate = worldgenlevel.getBlockState(blockpos.below());
            if (isDirt(blockstate) || isStone(blockstate)) {
               break;
            }
         }

         blockpos = blockpos.below();
      }

      if (blockpos.getY() <= worldgenlevel.getMinY() + 3) {
         return false;
      }

      for (int l = 0; l < 3; l++) {
         int i = randomsource.nextInt(2);
         int j = randomsource.nextInt(2);
         int k = randomsource.nextInt(2);
         float f = (i + j + k) * 0.333F + 0.5F;

         for (BlockPos blockpos1 : BlockPos.betweenClosed(blockpos.offset(-i, -j, -k), blockpos.offset(i, j, k))) {
            if (blockpos1.distSqr(blockpos) <= f * f) {
               worldgenlevel.setBlock(blockpos1, blockstateconfiguration.state, 3);
            }
         }

         blockpos = blockpos.offset(-1 + randomsource.nextInt(2), -randomsource.nextInt(2), -1 + randomsource.nextInt(2));
      }

      return true;
   }
}
