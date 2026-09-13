package com.mojang.renderpearl.backend.opengl;

import com.mojang.renderpearl.api.GpuFormat;

public sealed interface Uniform extends AutoCloseable permits Uniform.Sampler, Uniform.Ubo, Uniform.Utb {
   @Override
   default void close() {
   }

   record Sampler(int location, int samplerIndex) implements Uniform {
   }

   record Ubo(int blockBinding) implements Uniform {
   }

   record Utb(int location, int samplerIndex, GpuFormat format, int texture) implements Uniform {
      public Utb(final int location, final int samplerIndex, final GpuFormat format) {
         this(location, samplerIndex, format, GlStateManager._genTexture());
      }

      @Override
      public void close() {
         GlStateManager._deleteTexture(this.texture);
      }
   }
}
