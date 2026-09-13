package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GpuFormat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public sealed interface Uniform extends AutoCloseable permits Uniform.Sampler, Uniform.Ubo, Uniform.Utb {
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