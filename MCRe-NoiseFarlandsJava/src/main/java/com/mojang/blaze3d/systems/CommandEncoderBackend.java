package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.textures.GpuTexture;
import java.nio.ByteBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector4fc;

@OnlyIn(Dist.CLIENT)
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