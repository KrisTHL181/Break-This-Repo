package com.mojang.blaze3d.textures;

import com.mojang.blaze3d.DontObfuscate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public enum TextureFormat {
   RGBA8(4),
   RED8(1),
   RED8I(1),
   DEPTH32(4);

   private final int pixelSize;

   TextureFormat(final int p_391879_) {
      this.pixelSize = p_391879_;
   }

   public int pixelSize() {
      return this.pixelSize;
   }

   public boolean hasColorAspect() {
      return this == RGBA8 || this == RED8;
   }

   public boolean hasDepthAspect() {
      return this == DEPTH32;
   }
}
