package net.minecraft.client.resources;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LegacyStuffWrapper {
   @Deprecated
   public static int[] getPixels(ResourceManager p_118727_, Identifier p_458530_) throws IOException {
      try (
         InputStream inputstream = p_118727_.open(p_458530_);
         NativeImage nativeimage = NativeImage.read(inputstream);
      ) {
         return nativeimage.makePixelArray();
      }
   }
}
