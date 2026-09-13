package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;
import org.lwjgl.vulkan.VkDrawIndirectCommand;

@OnlyIn(Dist.CLIENT)
public class RenderPass implements AutoCloseable {
    public static final int MAX_VERTEX_BUFFERS = 16;
    private final RenderPassBackend backend;
    private final GpuDeviceBackend device;
    private final DeviceFeatures deviceFeatures;
    private final DeviceLimits deviceLimits;
    private final Runnable onFinish;
    private final RenderPass.@Nullable RenderArea renderArea;
    private boolean isClosed;
    private int pushedDebugGroups;
    private final List<RenderPassDescriptor.@Nullable Attachment<Optional<Vector4fc>>> colorAttachments;

    public RenderPass(
        final RenderPassBackend backend,
        final GpuDeviceBackend device,
        final List<RenderPassDescriptor.@Nullable Attachment<Optional<Vector4fc>>> colorAttachments,
        final Runnable onFinish,
        final RenderPass.@Nullable RenderArea renderArea
    ) {
        this.backend = backend;
        this.device = device;
        this.deviceFeatures = device.getDeviceInfo().features();
        this.deviceLimits = device.getDeviceInfo().limits();
        this.colorAttachments = colorAttachments;
        this.onFinish = onFinish;
        this.renderArea = renderArea;
    }

    public void pushDebugGroup(final Supplier<String> label) {
        if (this.isClosed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }

        this.pushedDebugGroups++;
        this.backend.pushDebugGroup(label);
    }

    public void popDebugGroup() {
        if (this.isClosed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }

        if (this.pushedDebugGroups == 0) {
            throw new IllegalStateException("Can't pop more debug groups than was pushed!");
        }

        this.pushedDebugGroups--;
        this.backend.popDebugGroup();
    }

    public void writeTimestamp(final GpuQueryPool pool, final int index) {
        if (index >= 0 && index <= pool.size()) {
            this.backend.writeTimestamp(pool, index);
        } else {
            throw new IllegalStateException("Index " + index + " is out of range for query pool of size " + pool.size());
        }
    }

    public void setPipeline(final RenderPipeline pipeline) {
        ColorTargetState[] colorTargetStates = pipeline.getColorTargetStates();
        if (colorTargetStates.length != this.colorAttachments.size()) {
            throw new IllegalStateException("Render pass color attachment count must match pipeline color target state count.");
        }

        for (int i = 0; i < this.colorAttachments.size(); i++) {
            RenderPassDescriptor.Attachment<Optional<Vector4fc>> attachment = this.colorAttachments.get(i);
            if (attachment != null) {
                ColorTargetState colorTargetState = colorTargetStates[i];
                if (colorTargetState == null || colorTargetState.format() != attachment.textureView().texture().getFormat()) {
                    throw new IllegalStateException("Render pass color attachment " + i + " format doesn't match pipeline format.");
                }
            }
        }

        this.backend.setPipeline(pipeline);
    }

    public void bindTexture(final String name, final @Nullable GpuTextureView textureView, final @Nullable GpuSampler sampler) {
        this.backend.bindTexture(name, textureView, sampler);
    }

    public void setUniform(final String name, final GpuBuffer value) {
        this.backend.setUniform(name, value);
    }

    public void setUniform(final String name, final GpuBufferSlice value) {
        int alignment = this.device.getDeviceInfo().limits().minUniformOffsetAlignment();
        if (value.offset() % alignment > 0L) {
            throw new IllegalArgumentException("Uniform buffer offset must be aligned to " + alignment);
        }

        this.backend.setUniform(name, value);
    }

    public void enableScissor(final int x, final int y, final int width, final int height) {
        if (width > 0 && height > 0) {
            if (x >= this.renderArea.x()
                && y >= this.renderArea.y()
                && x + width <= this.renderArea.x() + this.renderArea.width()
                && y + height <= this.renderArea.height()) {
                this.backend.enableScissor(x, y, width, height);
            } else {
                throw new IllegalArgumentException(
                    "Scissor at " + x + ", " + y + " with size " + width + "x" + height + " is out of bounds for render area " + this.renderArea
                );
            }
        } else {
            throw new IllegalArgumentException("Scissor size must be >0, was " + width + "x" + height);
        }
    }

    public void disableScissor() {
        this.backend.disableScissor();
    }

    public void setVertexBuffer(final int slot, final @Nullable GpuBufferSlice vertexBuffer) {
        if (slot < 0 || slot >= 16) {
            throw new IllegalArgumentException("Vertex buffer slot is out of range: " + slot);
        }

        if (vertexBuffer != null && vertexBuffer.buffer().isClosed()) {
            throw new IllegalStateException("Vertex buffer at slot " + slot + " has been closed!");
        }

        if (vertexBuffer != null && (vertexBuffer.buffer().usage() & 32) == 0) {
            throw new IllegalStateException("Vertex buffer at slot " + slot + " doesn't have GpuBuffer.USAGE_VERTEX flag!");
        }

        this.backend.setVertexBuffer(slot, vertexBuffer);
    }

    public void setIndexBuffer(final GpuBuffer indexBuffer, final IndexType indexType) {
        this.backend.setIndexBuffer(indexBuffer, indexType);
    }

    public void drawIndexed(final int indexCount, final int instanceCount, final int firstIndex, final int vertexOffset, final int firstInstance) {
        if (this.isClosed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }

        if (firstInstance != 0 && !this.deviceFeatures.nonZeroFirstInstance()) {
            throw new UnsupportedOperationException("firstInstance must be zero on when device does not support nonZeroFirstInstance");
        }

        this.backend.drawIndexed(indexCount, instanceCount, firstIndex, vertexOffset, firstInstance);
    }

    public void multiDrawIndexed(final IntBuffer drawParameters, final int instanceCount, final int firstInstance, final int drawCount) {
        if (this.isClosed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }

        if (!this.deviceFeatures.multiDrawDirectInterleaved()) {
            throw new UnsupportedOperationException("device does not support multiDrawDirectInterleaved");
        }

        if (firstInstance != 0 && !this.deviceFeatures.nonZeroFirstInstance()) {
            throw new UnsupportedOperationException("firstInstance must be zero on when device does not support nonZeroFirstInstance");
        }

        if (drawCount > this.deviceLimits.maxMultiDrawDirectInterleavedDrawCount()) {
            throw new IllegalArgumentException("May not exceed maxMultiDrawDirectInterleavedDrawCount draws in a single multiDrawDirectInterleaved call");
        }

        if (drawParameters.remaining() < drawCount * 3) {
            throw new IllegalArgumentException("Not enough elements in drawParameters for drawCount draws");
        }

        this.backend.multiDrawIndexed(drawParameters, instanceCount, firstInstance, drawCount);
    }

    public void multiDrawIndexed(final PointerBuffer firstIndexOffsets, final IntBuffer indexCounts, final IntBuffer vertexOffsets, final int drawCount) {
        if (this.isClosed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }

        if (!this.deviceFeatures.multiDrawDirectSeparate()) {
            throw new UnsupportedOperationException("device does not support multiDrawDirectSeparate");
        }

        if (firstIndexOffsets.remaining() < drawCount) {
            throw new IllegalArgumentException("firstIndexOffsets does not contain enough elements for drawCount draws");
        }

        if (indexCounts.remaining() < drawCount) {
            throw new IllegalArgumentException("indexCounts does not contain enough elements for drawCount draws");
        }

        if (vertexOffsets.remaining() < drawCount) {
            throw new IllegalArgumentException("vertexOffsets does not contain enough elements for drawCount draws");
        }

        this.backend.multiDrawIndexed(firstIndexOffsets, indexCounts, vertexOffsets, drawCount);
    }

    public void drawIndexedIndirect(final GpuBufferSlice commands, final int drawCount) {
        if (this.isClosed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }

        if (!this.deviceFeatures.drawIndirect()) {
            throw new UnsupportedOperationException("device does not support drawIndirect");
        }

        if (drawCount > 1 && !this.deviceFeatures.multiDrawIndirect()) {
            throw new UnsupportedOperationException("drawCount must be one when device does not support multiDrawIndirect");
        }

        if ((commands.buffer().usage() & 512) == 0) {
            throw new IllegalArgumentException("Indirect commands buffer must have GpuBuffer.USAGE_INDIRECT_PARAMETERS flag");
        }

        if (commands.length() < (long)drawCount * VkDrawIndexedIndirectCommand.SIZEOF) {
            throw new IllegalArgumentException("Commands buffer is not large enough to hold requested draw count at the given offset");
        }

        if (commands.offset() % 4L != 0L) {
            throw new IllegalArgumentException("Commands offset must be multiple of 4");
        }

        this.backend.drawIndexedIndirect(commands, drawCount);
    }

    public <T> void drawMultipleIndexed(
        final Collection<RenderPass.Draw<T>> draws,
        final @Nullable GpuBuffer defaultIndexBuffer,
        final @Nullable IndexType defaultIndexType,
        final Collection<String> dynamicUniforms,
        final T uniformArgument
    ) {
        if (this.isClosed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }

        this.backend.drawMultipleIndexed(draws, defaultIndexBuffer, defaultIndexType, dynamicUniforms, uniformArgument);
    }

    public void draw(final int vertexCount, final int instanceCount, final int firstVertex, final int firstInstance) {
        if (this.isClosed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }

        if (firstInstance != 0 && !this.deviceFeatures.nonZeroFirstInstance()) {
            throw new UnsupportedOperationException("firstInstance must be zero on when device does not support nonZeroFirstInstance");
        }

        this.backend.draw(vertexCount, instanceCount, firstVertex, firstInstance);
    }

    public void multiDraw(final IntBuffer drawParameters, final int instanceCount, final int firstInstance, final int drawCount) {
        if (this.isClosed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }

        if (!this.deviceFeatures.multiDrawDirectInterleaved()) {
            throw new UnsupportedOperationException("device does not support multiDrawDirectInterleaved");
        }

        if (firstInstance != 0 && !this.deviceFeatures.nonZeroFirstInstance()) {
            throw new UnsupportedOperationException("firstInstance must be zero on when device does not support nonZeroFirstInstance");
        }

        if (drawCount > this.deviceLimits.maxMultiDrawDirectInterleavedDrawCount()) {
            throw new IllegalArgumentException("May not exceed maxMultiDrawDirectInterleavedDrawCount draws in a single multiDrawDirectInterleaved call");
        }

        if (drawParameters.remaining() < drawCount * 2) {
            throw new IllegalArgumentException("Not enough elements in drawParameters for drawCount draws");
        }

        this.backend.multiDraw(drawParameters, instanceCount, firstInstance, drawCount);
    }

    public void multiDraw(final IntBuffer firstVertices, final IntBuffer vertexCounts, final int drawCount) {
        if (this.isClosed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }

        if (!this.deviceFeatures.multiDrawDirectSeparate()) {
            throw new UnsupportedOperationException("device does not support multiDrawDirectSeparate");
        }

        if (firstVertices.remaining() < drawCount) {
            throw new IllegalArgumentException("firstVertices does not contain enough elements for drawCount draws");
        }

        if (vertexCounts.remaining() < drawCount) {
            throw new IllegalArgumentException("vertexCounts does not contain enough elements for drawCount draws");
        }

        this.backend.multiDraw(firstVertices, vertexCounts, drawCount);
    }

    public void drawIndirect(final GpuBufferSlice commands, final int drawCount) {
        if (this.isClosed) {
            throw new IllegalStateException("Can't use a closed render pass");
        }

        if (!this.deviceFeatures.drawIndirect()) {
            throw new UnsupportedOperationException("device does not support drawIndirect");
        }

        if (drawCount > 1 && !this.deviceFeatures.multiDrawIndirect()) {
            throw new UnsupportedOperationException("drawCount must be one when device does not support multiDrawIndirect");
        }

        if ((commands.buffer().usage() & 512) == 0) {
            throw new IllegalArgumentException("Indirect commands buffer must have GpuBuffer.USAGE_INDIRECT_PARAMETERS flag");
        }

        if (commands.length() < (long)drawCount * VkDrawIndirectCommand.SIZEOF) {
            throw new IllegalArgumentException("Commands buffer is not large enough to hold requested draw count at the given offset");
        }

        if (commands.offset() % 4L != 0L) {
            throw new IllegalArgumentException("Commands offset must be multiple of 4");
        }

        this.backend.drawIndirect(commands, drawCount);
    }

    @Override
    public void close() {
        if (!this.isClosed) {
            this.isClosed = true;
            if (this.pushedDebugGroups > 0) {
                throw new IllegalStateException("Render pass had debug groups left open!");
            }

            this.onFinish.run();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Draw<T>(
        int slot,
        GpuBuffer vertexBuffer,
        @Nullable GpuBuffer indexBuffer,
        @Nullable IndexType indexType,
        int firstIndex,
        int indexCount,
        int baseVertex,
        @Nullable BiConsumer<T, RenderPass.UniformUploader> uniformUploaderConsumer
    ) {
        public Draw(
            final int slot,
            final GpuBuffer vertexBuffer,
            final GpuBuffer indexBuffer,
            final IndexType indexType,
            final int firstIndex,
            final int indexCount,
            final int baseVertex
        ) {
            this(slot, vertexBuffer, indexBuffer, indexType, firstIndex, indexCount, baseVertex, null);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record RenderArea(int x, int y, int width, int height) {
        public boolean fillsTexture(final GpuTextureView texture) {
            return this.x == 0 && this.y == 0 && this.width == texture.getWidth(0) && this.height == texture.getHeight(0);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface UniformUploader {
        void upload(String name, GpuBufferSlice buffer);
    }
}