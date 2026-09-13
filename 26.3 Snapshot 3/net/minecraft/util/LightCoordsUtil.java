package net.minecraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public class LightCoordsUtil {
   public static final int FULL_BRIGHT = 15728880;
   public static final int FULL_SKY = 15728640;
   private static final int MAX_SMOOTH_LIGHT_LEVEL = 240;

   public static int pack(final int block, final int sky) {
      return block << 4 | sky << 20;
   }

   public static int block(final int packed) {
      return packed >> 4 & 15;
   }

   public static int sky(final int packed) {
      return packed >> 20 & 15;
   }

   public static int withBlock(final int coords, final int block) {
      return coords & 0xFF0000 | block << 4;
   }

   public static int smoothPack(final int block, final int sky) {
      return block & 0xFF | (sky & 0xFF) << 16;
   }

   public static int smoothBlock(final int packed) {
      return packed & 0xFF;
   }

   public static int smoothSky(final int packed) {
      return packed >> 16 & 0xFF;
   }

   public static int addSmoothBlockEmission(final int lightCoords, float blockLightEmission) {
      blockLightEmission = Mth.clamp(blockLightEmission, 0.0F, 1.0F);
      int emittedBlock = (int)(Mth.clamp(blockLightEmission, 0.0F, 1.0F) * 240.0F);
      int block = Math.min(smoothBlock(lightCoords) + emittedBlock, 240);
      return smoothPack(block, smoothSky(lightCoords));
   }

   public static int max(final int coords1, final int coords2) {
      int block1 = block(coords1);
      int block2 = block(coords2);
      int sky1 = sky(coords1);
      int sky2 = sky(coords2);
      return pack(Math.max(block1, block2), Math.max(sky1, sky2));
   }

   public static int lightCoordsWithEmission(final int lightCoords, final int emission) {
      if (emission == 0) {
         return lightCoords;
      }

      int sky = Math.max(sky(lightCoords), emission);
      int block = Math.max(block(lightCoords), emission);
      return pack(block, sky);
   }

   public static int smoothBlend(int neighbor1, int neighbor2, int neighbor3, final int center) {
      if (sky(center) > 2 || block(center) > 2) {
         if (neighbor1 == 0) {
            neighbor1 = center;
         } else if (sky(neighbor1) == 0) {
            neighbor1 |= center & 0xFF0000;
         }

         if (neighbor2 == 0) {
            neighbor2 = center;
         } else if (sky(neighbor2) == 0) {
            neighbor2 |= center & 0xFF0000;
         }

         if (neighbor3 == 0) {
            neighbor3 = center;
         } else if (sky(neighbor3) == 0) {
            neighbor3 |= center & 0xFF0000;
         }
      }

      return neighbor1 + neighbor2 + neighbor3 + center >> 2 & 16711935;
   }

   public static int smoothWeightedBlend(
      final int coords1,
      final int coords2,
      final int coords3,
      final int coords4,
      final float weight1,
      final float weight2,
      final float weight3,
      final float weight4
   ) {
      int sky = (int)(smoothSky(coords1) * weight1 + smoothSky(coords2) * weight2 + smoothSky(coords3) * weight3 + smoothSky(coords4) * weight4);
      int block = (int)(smoothBlock(coords1) * weight1 + smoothBlock(coords2) * weight2 + smoothBlock(coords3) * weight3 + smoothBlock(coords4) * weight4);
      return smoothPack(block, sky);
   }

   public static int getLightCoords(final BlockAndLightGetter level, final BlockPos pos) {
      return getLightCoords(LightCoordsUtil.BrightnessGetter.DEFAULT, level, level.getBlockState(pos), pos);
   }

   public static int getLightCoords(
      final LightCoordsUtil.BrightnessGetter brightnessGetter, final BlockAndLightGetter level, final BlockState state, final BlockPos pos
   ) {
      if (state.emissiveRendering()) {
         return 15728880;
      }

      int packedBrightness = brightnessGetter.packedBrightness(level, pos);
      int block = block(packedBrightness);
      int blockSelfEmission = state.getLightEmission();
      return block < blockSelfEmission ? withBlock(packedBrightness, blockSelfEmission) : packedBrightness;
   }

   @FunctionalInterface
   public interface BrightnessGetter {
      LightCoordsUtil.BrightnessGetter DEFAULT = (level, pos) -> {
         int sky = level.getBrightness(LightLayer.SKY, pos);
         int block = level.getBrightness(LightLayer.BLOCK, pos);
         return LightCoordsUtil.pack(block, sky);
      };

      int packedBrightness(BlockAndLightGetter level, BlockPos pos);
   }
}
