package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class MissingTextureAtlasSprite {
   private static final int MISSING_IMAGE_WIDTH = 16;
   private static final int MISSING_IMAGE_HEIGHT = 16;
   private static final String MISSING_TEXTURE_NAME = "missingno";
   private static final Identifier MISSING_TEXTURE_LOCATION = Identifier.withDefaultNamespace("missingno");

   public static NativeImage generateMissingImage() {
      return generateMissingImage(16, 16);
   }

   public static NativeImage generateMissingImage(int p_249811_, int p_249362_) {
      NativeImage nativeimage = new NativeImage(p_249811_, p_249362_, false);
      int i = -524040;

      for (int j = 0; j < p_249362_; j++) {
         for (int k = 0; k < p_249811_; k++) {
            if (j < p_249362_ / 2 ^ k < p_249811_ / 2) {
               nativeimage.setPixel(k, j, -524040);
            } else {
               nativeimage.setPixel(k, j, -16777216);
            }
         }
      }

      return nativeimage;
   }

   public static SpriteContents create() {
      NativeImage nativeimage = generateMissingImage(16, 16);
      return new SpriteContents(MISSING_TEXTURE_LOCATION, new FrameSize(16, 16), nativeimage);
   }

   public static Identifier getLocation() {
      return MISSING_TEXTURE_LOCATION;
   }
}
