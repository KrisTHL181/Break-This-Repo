package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.util.ARGB;

public class OverlayTexture implements AutoCloseable {
   private static final int SIZE = 16;
   public static final int NO_WHITE_U = 0;
   public static final int RED_OVERLAY_V = 3;
   public static final int WHITE_OVERLAY_V = 10;
   public static final int NO_OVERLAY = pack(0, 10);
   private final DynamicTexture texture = new DynamicTexture("Entity Color Overlay", 16, 16, false);

   public OverlayTexture() {
      NativeImage pixels = this.texture.getPixels();

      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            if (y < 8) {
               pixels.setPixel(x, y, -1291911168);
            } else {
               int a = (int)((1.0F - x / 15.0F * 0.75F) * 255.0F);
               pixels.setPixel(x, y, ARGB.white(a));
            }
         }
      }

      this.texture.upload();
   }

   @Override
   public void close() {
      this.texture.close();
   }

   public static int u(final float whiteOverlayProgress) {
      return (int)(whiteOverlayProgress * 15.0F);
   }

   public static int v(final boolean hurtOverlay) {
      return hurtOverlay ? 3 : 10;
   }

   public static int pack(final int u, final int v) {
      return u | v << 16;
   }

   public static int pack(final float whiteOverlayProgress, final boolean redOverlay) {
      return pack(u(whiteOverlayProgress), v(redOverlay));
   }

   public GpuTextureView getTextureView() {
      return this.texture.getTextureView();
   }
}
