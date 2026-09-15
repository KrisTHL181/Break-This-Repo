package com.mojang.blaze3d.opengl;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface FrameBufferAttachment {
    int glId();

    int fboMipLevel();

    void addAssociatedFbo(FrameBufferCache.CacheKey fboKey);

    void removeAssociatedFbo(FrameBufferCache.CacheKey fboKey);
}