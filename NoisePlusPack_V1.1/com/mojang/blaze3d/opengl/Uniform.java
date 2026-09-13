package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public sealed interface Uniform extends AutoCloseable permits Uniform.Ubo, Uniform.Utb, Uniform.Sampler {
   @Override
   default void close() {
   }

   @OnlyIn(Dist.CLIENT)
   record Sampler(int location, int samplerIndex) implements Uniform {
   }

   @OnlyIn(Dist.CLIENT)
   record Ubo(int blockBinding) implements Uniform {
   }

   @OnlyIn(Dist.CLIENT)
   record Utb(int location, int samplerIndex, TextureFormat format, int texture) implements Uniform {
      public Utb(int p_409278_, int p_406836_, TextureFormat p_408072_) {
         this(p_409278_, p_406836_, p_408072_, GlStateManager._genTexture());
      }

      @Override
      public void close() {
         GlStateManager._deleteTexture(this.texture);
      }
   }
}
