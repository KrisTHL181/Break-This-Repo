package net.minecraft.client.resources;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public class LegacyStuffWrapper {
   @Deprecated
   public static int[] getPixels(final ResourceManager resourceManager, final Identifier location) throws IOException {
      try (
         InputStream resource = resourceManager.open(location);
         NativeImage image = NativeImage.read(resource);
      ) {
         return image.makePixelArray();
      }
   }
}
