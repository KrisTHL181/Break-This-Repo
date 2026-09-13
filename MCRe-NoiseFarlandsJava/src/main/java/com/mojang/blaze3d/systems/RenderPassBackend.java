package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;

@OnlyIn(Dist.CLIENT)
public interface RenderPassBackend {
    void pushDebugGroup(final Supplier<String> label);

    void popDebugGroup();

    void setPipeline(final RenderPipeline pipeline);

    void bindTexture(final String name, final @Nullable GpuTextureView textureView, final @Nullable GpuSampler sampler);

    void setUniform(final String name, final GpuBuffer value);

    void setUniform(final String name, final GpuBufferSlice value);

    void enableScissor(final int x, final int y, final int width, final int height);

    void disableScissor();

    void setVertexBuffer(final int slot, final @Nullable GpuBufferSlice vertexBuffer);

    void setIndexBuffer(final GpuBuffer indexBuffer, final IndexType indexType);

    void drawIndexed(final int indexCount, final int instanceCount, final int firstIndex, final int vertexOffset, final int firstInstance);

    void multiDrawIndexed(final IntBuffer drawParameters, final int instanceCount, final int firstInstance, final int drawCount);

    void multiDrawIndexed(final PointerBuffer firstIndexOffsets, final IntBuffer indexCounts, final IntBuffer vertexOffsets, final int drawCount);

    void drawIndexedIndirect(final GpuBufferSlice commands, final int drawCount);

    <T> void drawMultipleIndexed(
        final Collection<RenderPass.Draw<T>> draws,
        final @Nullable GpuBuffer defaultIndexBuffer,
        final @Nullable IndexType defaultIndexType,
        final Collection<String> dynamicUniforms,
        final T uniformArgument
    );

    void draw(final int vertexCount, final int instanceCount, final int firstVertex, final int firstInstance);

    void multiDraw(final IntBuffer drawParameters, final int instanceCount, final int firstInstance, final int drawCount);

    void multiDraw(final IntBuffer firstVertices, final IntBuffer vertexCounts, final int drawCount);

    void drawIndirect(final GpuBufferSlice commands, final int drawCount);

    void writeTimestamp(GpuQueryPool pool, int index);
}