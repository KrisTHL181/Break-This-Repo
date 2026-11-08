package com.mojang.renderpearl.backend.opengl;

public interface FrameBufferAttachment {
   int glId();

   int fboMipLevel();

   void addAssociatedFbo(FrameBufferCache.CacheKey fboKey);

   void removeAssociatedFbo(FrameBufferCache.CacheKey fboKey);
}
