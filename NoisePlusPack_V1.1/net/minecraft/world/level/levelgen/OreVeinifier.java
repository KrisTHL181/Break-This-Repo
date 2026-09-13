package net.minecraft.world.level.levelgen;

import net.minecraft.SharedConstants;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class OreVeinifier {
   private static final float VEININESS_THRESHOLD = 0.4F;
   private static final int EDGE_ROUNDOFF_BEGIN = 20;
   private static final double MAX_EDGE_ROUNDOFF = 0.2;
   private static final float VEIN_SOLIDNESS = 0.7F;
   private static final float MIN_RICHNESS = 0.1F;
   private static final float MAX_RICHNESS = 0.3F;
   private static final float MAX_RICHNESS_THRESHOLD = 0.6F;
   private static final float CHANCE_OF_RAW_ORE_BLOCK = 0.02F;
   private static final float SKIP_ORE_IF_GAP_NOISE_IS_BELOW = -0.3F;

   private OreVeinifier() {
   }

   protected static NoiseChunk.BlockStateFiller create(
      DensityFunction p_209668_, DensityFunction p_209669_, DensityFunction p_209670_, PositionalRandomFactory p_209671_
   ) {
      BlockState blockstate = SharedConstants.DEBUG_ORE_VEINS ? Blocks.AIR.defaultBlockState() : null;
      return p_422224_ -> {
         double d0 = p_209668_.compute(p_422224_);
         int i = p_422224_.blockY();
         OreVeinifier.VeinType oreveinifier$veintype = d0 > 0.0 ? OreVeinifier.VeinType.COPPER : OreVeinifier.VeinType.IRON;
         double d1 = Math.abs(d0);
         int j = oreveinifier$veintype.maxY - i;
         int k = i - oreveinifier$veintype.minY;
         if (k >= 0 && j >= 0) {
            int l = Math.min(j, k);
            double d2 = Mth.clampedMap(l, 0.0, 20.0, -0.2, 0.0);
            if (d1 + d2 < 0.4F) {
               return blockstate;
            } else {
               RandomSource randomsource = p_209671_.at(p_422224_.blockX(), i, p_422224_.blockZ());
               if (randomsource.nextFloat() > 0.7F) {
                  return blockstate;
               } else if (p_209669_.compute(p_422224_) >= 0.0) {
                  return blockstate;
               } else {
                  double d3 = Mth.clampedMap(d1, 0.4F, 0.6F, 0.1F, 0.3F);
                  if (randomsource.nextFloat() < d3 && p_209670_.compute(p_422224_) > -0.3F) {
                     return randomsource.nextFloat() < 0.02F ? oreveinifier$veintype.rawOreBlock : oreveinifier$veintype.ore;
                  } else {
                     return SharedConstants.DEBUG_ORE_VEINS ? Blocks.OAK_BUTTON.defaultBlockState() : oreveinifier$veintype.filler;
                  }
               }
            }
         } else {
            return blockstate;
         }
      };
   }

   protected enum VeinType {
      COPPER(Blocks.COPPER_ORE.defaultBlockState(), Blocks.RAW_COPPER_BLOCK.defaultBlockState(), Blocks.GRANITE.defaultBlockState(), 0, 50),
      IRON(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(), Blocks.RAW_IRON_BLOCK.defaultBlockState(), Blocks.TUFF.defaultBlockState(), -60, -8);

      final BlockState ore;
      final BlockState rawOreBlock;
      final BlockState filler;
      protected final int minY;
      protected final int maxY;

      VeinType(final BlockState p_209684_, final BlockState p_209685_, final BlockState p_209686_, final int p_209687_, final int p_209688_) {
         this.ore = p_209684_;
         this.rawOreBlock = p_209685_;
         this.filler = p_209686_;
         this.minY = p_209687_;
         this.maxY = p_209688_;
      }
   }
}
