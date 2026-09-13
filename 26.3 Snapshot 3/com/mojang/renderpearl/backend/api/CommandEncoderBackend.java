package com.mojang.renderpearl.backend.api;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.buffers.TransientMemory;
import com.mojang.renderpearl.api.commands.GpuFence;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.textures.GpuTexture;
import java.nio.ByteBuffer;
import org.joml.Vector4fc;

public interface CommandEncoderBackend {
   void submit();

   TransientMemory transientMemory();

   RenderPassBackend createRenderPass(RenderPassDescriptor descriptor);

   void submitRenderPass();

   void clearColorTexture(GpuTexture colorTexture, Vector4fc clearColor);

   void clearColorAndDepthTextures(GpuTexture colorTexture, Vector4fc clearColor, GpuTexture depthTexture, double clearDepth);

   void clearColorAndDepthTextures(
      GpuTexture colorTexture, Vector4fc clearColor, GpuTexture depthTexture, double clearDepth, int regionX, int regionY, int regionWidth, int regionHeight
   );

   void clearDepthTexture(GpuTexture depthTexture, double clearDepth);

   void writeToBuffer(GpuBufferSlice destination, ByteBuffer data);

   void copyToBuffer(GpuBufferSlice source, GpuBufferSlice target);

   void writeToTexture(GpuTexture destination, ByteBuffer source, int mipLevel, int depthOrLayer, int destX, int destY, int width, int height);

   void copyBufferToTexture(
      GpuBufferSlice source,
      int sourceX,
      int sourceY,
      int sourceWidth,
      int sourceHeight,
      GpuTexture destination,
      int destinationX,
      int destinationY,
      int copyWidth,
      int copyHeight,
      int mipLevel,
      int arrayLayer
   );

   void copyTextureToBuffer(GpuTexture source, GpuBuffer destination, long offset, Runnable callback, int mipLevel);

   void copyTextureToBuffer(GpuTexture source, GpuBuffer destination, long offset, Runnable callback, int mipLevel, int x, int y, int width, int height);

   void copyTextureToTexture(GpuTexture source, GpuTexture destination, int mipLevel, int destX, int destY, int sourceX, int sourceY, int width, int height);

   GpuFence createFence();

   void writeTimestamp(GpuQueryPool pool, int index);
}
