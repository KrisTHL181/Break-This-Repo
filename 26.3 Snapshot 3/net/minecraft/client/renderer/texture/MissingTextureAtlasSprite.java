package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;

public final class MissingTextureAtlasSprite {
   private static final int MISSING_IMAGE_WIDTH = 16;
   private static final int MISSING_IMAGE_HEIGHT = 16;
   private static final String MISSING_TEXTURE_NAME = "missingno";
   private static final Identifier MISSING_TEXTURE_LOCATION = Identifier.withDefaultNamespace("missingno");

   public static NativeImage generateMissingImage() {
      return generateMissingImage(16, 16);
   }

   public static NativeImage generateMissingImage(final int width, final int height) {
      NativeImage result = new NativeImage(width, height, false);
      int pink = -524040;

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            if (y < height / 2 ^ x < width / 2) {
               result.setPixel(x, y, -524040);
            } else {
               result.setPixel(x, y, -16777216);
            }
         }
      }

      return result;
   }

   public static SpriteContents create() {
      NativeImage contents = generateMissingImage(16, 16);
      return new SpriteContents(MISSING_TEXTURE_LOCATION, new FrameSize(16, 16), contents);
   }

   public static Identifier getLocation() {
      return MISSING_TEXTURE_LOCATION;
   }
}
