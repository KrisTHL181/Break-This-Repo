package com.mojang.blaze3d.buffers;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface GpuFence extends AutoCloseable {
    @Override
    void close();

    boolean awaitCompletion(final long timeoutNS);
}