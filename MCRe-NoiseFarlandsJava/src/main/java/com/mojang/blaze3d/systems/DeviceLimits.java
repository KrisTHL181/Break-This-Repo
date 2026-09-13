package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.GpuFormat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record DeviceLimits(
    int maxAnisotropy,
    int minUniformOffsetAlignment,
    int maxTextureSize,
    long maxMemoryAllocationSize,
    int maxMultiDrawDirectInterleavedDrawCount,
    int maxColorAttachments
) {
    public int maxTextureSizeForFormat(final GpuFormat format) {
        return Integer.highestOneBit(Math.min(this.maxTextureSize, (int)Math.sqrt((double)this.maxMemoryAllocationSize / format.blockSize())));
    }
}