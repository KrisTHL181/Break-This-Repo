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

   static NoiseChunk.BlockStateFiller create(
      final DensityFunction veinToggle,
      final DensityFunction veinRidged,
      final DensityFunction veinGap,
      final PositionalRandomFactory oreVeinsPositionalRandomFactory
   ) {
      BlockState defaultState = SharedConstants.DEBUG_ORE_VEINS ? Blocks.AIR.defaultBlockState() : null;
      return context -> {
         double oreVeininessNoiseValue = veinToggle.compute(context);
         int posY = context.blockY();
         OreVeinifier.VeinType veinType = oreVeininessNoiseValue > 0.0 ? OreVeinifier.VeinType.COPPER : OreVeinifier.VeinType.IRON;
         double veininessRidged = Math.abs(oreVeininessNoiseValue);
         int distanceFromTop = veinType.maxY - posY;
         int distanceFromBottom = posY - veinType.minY;
         if (distanceFromBottom >= 0 && distanceFromTop >= 0) {
            int distanceFromEdge = Math.min(distanceFromTop, distanceFromBottom);
            double edgeRoundoff = Mth.clampedMap(distanceFromEdge, 0.0, 20.0, -0.2, 0.0);
            if (veininessRidged + edgeRoundoff < 0.4F) {
               return defaultState;
            } else {
               RandomSource positionalRandom = oreVeinsPositionalRandomFactory.at(context.blockX(), posY, context.blockZ());
               if (positionalRandom.nextFloat() > 0.7F) {
                  return defaultState;
               } else if (veinRidged.compute(context) >= 0.0) {
                  return defaultState;
               } else {
                  double richness = Mth.clampedMap(veininessRidged, 0.4F, 0.6F, 0.1F, 0.3F);
                  if (positionalRandom.nextFloat() < richness && veinGap.compute(context) > -0.3F) {
                     return positionalRandom.nextFloat() < 0.02F ? veinType.rawOreBlock : veinType.ore;
                  } else {
                     return SharedConstants.DEBUG_ORE_VEINS ? Blocks.OAK_BUTTON.defaultBlockState() : veinType.filler;
                  }
               }
            }
         } else {
            return defaultState;
         }
      };
   }

   protected enum VeinType {
      COPPER(Blocks.COPPER_ORE.defaultBlockState(), Blocks.RAW_COPPER_BLOCK.defaultBlockState(), Blocks.GRANITE.defaultBlockState(), 0, 50),
      IRON(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(), Blocks.RAW_IRON_BLOCK.defaultBlockState(), Blocks.TUFF.defaultBlockState(), -60, -8);

      private final BlockState ore;
      private final BlockState rawOreBlock;
      private final BlockState filler;
      protected final int minY;
      protected final int maxY;

      VeinType(final BlockState ore, final BlockState rawOreBlock, final BlockState filler, final int minY, final int maxY) {
         this.ore = ore;
         this.rawOreBlock = rawOreBlock;
         this.filler = filler;
         this.minY = minY;
         this.maxY = maxY;
      }
   }
}
