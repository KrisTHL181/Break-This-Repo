package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;
import java.util.function.Supplier;
import net.minecraft.client.gui.font.GlyphStitcher;
import org.jspecify.annotations.Nullable;

public enum SpecialGlyphs implements GlyphInfo {
   WHITE(() -> generate(5, 8, (x, y) -> -1)),
   MISSING(() -> {
      int width = 5;
      int height = 8;
      return generate(5, 8, (x, y) -> {
         boolean edge = x == 0 || x + 1 == 5 || y == 0 || y + 1 == 8;
         return edge ? -1 : 0;
      });
   });

   private final NativeImage image;

   private static NativeImage generate(final int width, final int height, final SpecialGlyphs.PixelProvider pixelProvider) {
      NativeImage result = new NativeImage(NativeImage.Format.RGBA, width, height, false);

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            result.setPixel(x, y, pixelProvider.getColor(x, y));
         }
      }

      result.untrack();
      return result;
   }

   SpecialGlyphs(final Supplier<NativeImage> image) {
      this.image = image.get();
   }

   @Override
   public float getAdvance() {
      return this.image.getWidth() + 1;
   }

   public @Nullable BakedSheetGlyph bake(final GlyphStitcher stitcher) {
      return stitcher.stitch(this, new GlyphBitmap() {
         @Override
         public int getPixelWidth() {
            return SpecialGlyphs.this.image.getWidth();
         }

         @Override
         public int getPixelHeight() {
            return SpecialGlyphs.this.image.getHeight();
         }

         @Override
         public float getOversample() {
            return 1.0F;
         }

         @Override
         public void upload(final int x, final int y, final GpuTexture texture) {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, SpecialGlyphs.this.image, 0, 0, x, y);
         }

         @Override
         public boolean isColored() {
            return true;
         }
      });
   }

   @FunctionalInterface
   private interface PixelProvider {
      int getColor(int x, int y);
   }
}
