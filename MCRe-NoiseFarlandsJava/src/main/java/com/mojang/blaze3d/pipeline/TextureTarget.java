package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TextureTarget extends RenderTarget {
    public TextureTarget(final @Nullable String label, final int width, final int height, final boolean useDepth, final GpuFormat format) {
        super(label, useDepth, format);
        RenderSystem.assertOnRenderThread();
        this.resize(width, height);
    }
}