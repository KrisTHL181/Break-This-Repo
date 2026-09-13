package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public class MipmapGenerator {
   private static final String ITEM_PREFIX = "item/";
   private static final float ALPHA_CUTOFF = 0.5F;
   private static final float STRICT_ALPHA_CUTOFF = 0.3F;

   private MipmapGenerator() {
   }

   private static float alphaTestCoverage(final NativeImage image, final float alphaRef, final float alphaScale) {
      int width = image.getWidth();
      int height = image.getHeight();
      float coverage = 0.0F;
      int subsample_count = 4;

      for (int y = 0; y < height - 1; y++) {
         for (int x = 0; x < width - 1; x++) {
            float alpha00 = Math.clamp(ARGB.alphaFloat(image.getPixel(x, y)) * alphaScale, 0.0F, 1.0F);
            float alpha10 = Math.clamp(ARGB.alphaFloat(image.getPixel(x + 1, y)) * alphaScale, 0.0F, 1.0F);
            float alpha01 = Math.clamp(ARGB.alphaFloat(image.getPixel(x, y + 1)) * alphaScale, 0.0F, 1.0F);
            float alpha11 = Math.clamp(ARGB.alphaFloat(image.getPixel(x + 1, y + 1)) * alphaScale, 0.0F, 1.0F);
            float texelCoverage = 0.0F;

            for (int subsample_y = 0; subsample_y < 4; subsample_y++) {
               float fy = (subsample_y + 0.5F) / 4.0F;

               for (int subsample_x = 0; subsample_x < 4; subsample_x++) {
                  float fx = (subsample_x + 0.5F) / 4.0F;
                  float alpha = alpha00 * (1.0F - fx) * (1.0F - fy) + alpha10 * fx * (1.0F - fy) + alpha01 * (1.0F - fx) * fy + alpha11 * fx * fy;
                  if (alpha > alphaRef) {
                     texelCoverage++;
                  }
               }
            }

            coverage += texelCoverage / 16.0F;
         }
      }

      return coverage / ((width - 1) * (height - 1));
   }

   private static void scaleAlphaToCoverage(final NativeImage image, final float desiredCoverage, final float alphaRef, final float alphaCutoffBias) {
      float minAlphaScale = 0.0F;
      float maxAlphaScale = 4.0F;
      float alphaScale = 1.0F;
      float bestAlphaScale = 1.0F;
      float bestError = Float.MAX_VALUE;
      int width = image.getWidth();
      int height = image.getHeight();

      for (int i = 0; i < 5; i++) {
         float currentCoverage = alphaTestCoverage(image, alphaRef, alphaScale);
         float error = Math.abs(currentCoverage - desiredCoverage);
         if (error < bestError) {
            bestError = error;
            bestAlphaScale = alphaScale;
         }

         if (currentCoverage < desiredCoverage) {
            minAlphaScale = alphaScale;
         } else {
            if (!(currentCoverage > desiredCoverage)) {
               break;
            }

            maxAlphaScale = alphaScale;
         }

         alphaScale = (minAlphaScale + maxAlphaScale) * 0.5F;
      }

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            int pixel = image.getPixel(x, y);
            float alpha = ARGB.alphaFloat(pixel);
            alpha = alpha * bestAlphaScale + alphaCutoffBias + 0.025F;
            alpha = Math.clamp(alpha, 0.0F, 1.0F);
            image.setPixel(x, y, ARGB.color(alpha, pixel));
         }
      }
   }

   public static NativeImage[] generateMipLevels(
      final Identifier name,
      final NativeImage[] currentMips,
      final int newMipLevel,
      MipmapStrategy mipmapStrategy,
      final float alphaCutoffBias,
      final Transparency transparency
   ) {
      if (mipmapStrategy == MipmapStrategy.AUTO) {
         mipmapStrategy = transparency.hasTransparent() ? MipmapStrategy.CUTOUT : MipmapStrategy.MEAN;
      }

      if (currentMips.length == 1 && !name.getPath().startsWith("item/")) {
         if (mipmapStrategy == MipmapStrategy.CUTOUT || mipmapStrategy == MipmapStrategy.STRICT_CUTOUT) {
            TextureUtil.solidify(currentMips[0]);
         } else if (mipmapStrategy == MipmapStrategy.DARK_CUTOUT) {
            TextureUtil.fillEmptyAreasWithDarkColor(currentMips[0]);
         }
      }

      if (newMipLevel + 1 <= currentMips.length) {
         return currentMips;
      }

      NativeImage[] result = new NativeImage[newMipLevel + 1];
      result[0] = currentMips[0];
      boolean isCutoutMip = mipmapStrategy == MipmapStrategy.CUTOUT
         || mipmapStrategy == MipmapStrategy.STRICT_CUTOUT
         || mipmapStrategy == MipmapStrategy.DARK_CUTOUT;
      float cutoutRef = mipmapStrategy == MipmapStrategy.STRICT_CUTOUT ? 0.3F : 0.5F;
      float originalCoverage = isCutoutMip ? alphaTestCoverage(currentMips[0], cutoutRef, 1.0F) : 0.0F;

      for (int level = 1; level <= newMipLevel; level++) {
         if (level < currentMips.length) {
            result[level] = currentMips[level];
         } else {
            NativeImage lastData = result[level - 1];
            NativeImage data = new NativeImage(lastData.getWidth() >> 1, lastData.getHeight() >> 1, false);
            int width = data.getWidth();
            int height = data.getHeight();

            for (int x = 0; x < width; x++) {
               for (int y = 0; y < height; y++) {
                  int color1 = lastData.getPixel(x * 2 + 0, y * 2 + 0);
                  int color2 = lastData.getPixel(x * 2 + 1, y * 2 + 0);
                  int color3 = lastData.getPixel(x * 2 + 0, y * 2 + 1);
                  int color4 = lastData.getPixel(x * 2 + 1, y * 2 + 1);
                  int color;
                  if (mipmapStrategy == MipmapStrategy.DARK_CUTOUT) {
                     color = darkenedAlphaBlend(color1, color2, color3, color4);
                  } else {
                     color = ARGB.meanLinear(color1, color2, color3, color4);
                  }

                  data.setPixel(x, y, color);
               }
            }

            result[level] = data;
         }

         if (isCutoutMip) {
            scaleAlphaToCoverage(result[level], originalCoverage, cutoutRef, alphaCutoffBias);
         }
      }

      return result;
   }

   private static int darkenedAlphaBlend(final int color1, final int color2, final int color3, final int color4) {
      float aTotal = 0.0F;
      float rTotal = 0.0F;
      float gTotal = 0.0F;
      float bTotal = 0.0F;
      if (ARGB.alpha(color1) != 0) {
         aTotal += ARGB.srgbToLinearChannel(ARGB.alpha(color1));
         rTotal += ARGB.srgbToLinearChannel(ARGB.red(color1));
         gTotal += ARGB.srgbToLinearChannel(ARGB.green(color1));
         bTotal += ARGB.srgbToLinearChannel(ARGB.blue(color1));
      }

      if (ARGB.alpha(color2) != 0) {
         aTotal += ARGB.srgbToLinearChannel(ARGB.alpha(color2));
         rTotal += ARGB.srgbToLinearChannel(ARGB.red(color2));
         gTotal += ARGB.srgbToLinearChannel(ARGB.green(color2));
         bTotal += ARGB.srgbToLinearChannel(ARGB.blue(color2));
      }

      if (ARGB.alpha(color3) != 0) {
         aTotal += ARGB.srgbToLinearChannel(ARGB.alpha(color3));
         rTotal += ARGB.srgbToLinearChannel(ARGB.red(color3));
         gTotal += ARGB.srgbToLinearChannel(ARGB.green(color3));
         bTotal += ARGB.srgbToLinearChannel(ARGB.blue(color3));
      }

      if (ARGB.alpha(color4) != 0) {
         aTotal += ARGB.srgbToLinearChannel(ARGB.alpha(color4));
         rTotal += ARGB.srgbToLinearChannel(ARGB.red(color4));
         gTotal += ARGB.srgbToLinearChannel(ARGB.green(color4));
         bTotal += ARGB.srgbToLinearChannel(ARGB.blue(color4));
      }

      aTotal /= 4.0F;
      rTotal /= 4.0F;
      gTotal /= 4.0F;
      bTotal /= 4.0F;
      return ARGB.color(ARGB.linearToSrgbChannel(aTotal), ARGB.linearToSrgbChannel(rTotal), ARGB.linearToSrgbChannel(gTotal), ARGB.linearToSrgbChannel(bTotal));
   }
}
