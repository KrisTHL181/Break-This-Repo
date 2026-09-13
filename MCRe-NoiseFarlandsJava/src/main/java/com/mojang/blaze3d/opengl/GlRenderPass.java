package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;

@OnlyIn(Dist.CLIENT)
class GlRenderPass implements RenderPassBackend {
    public static final boolean VALIDATION = SharedConstants.IS_RUNNING_IN_IDE;
    private final GlCommandEncoder encoder;
    private final GlDevice device;
    private final boolean hasDepthTexture;
    private final ScissorState defaultScissorState;
    protected @Nullable GlRenderPipeline pipeline;
    protected final @Nullable GpuBufferSlice[] vertexBuffers = new GpuBufferSlice[16];
    protected boolean vertexBufferDirty = true;
    protected @Nullable GpuBuffer indexBuffer;
    protected IndexType indexType = IndexType.INT;
    private final ScissorState scissorState = new ScissorState();
    protected final HashMap<String, GpuBufferSlice> uniforms = new HashMap<>();
    protected final HashMap<String, GlRenderPass.TextureViewAndSampler> samplers = new HashMap<>();
    protected final Set<String> dirtyUniforms = new HashSet<>();
    protected final int colorAttachmentCount;

    public GlRenderPass(
        final GlCommandEncoder encoder,
        final GlDevice device,
        final boolean hasDepthTexture,
        final int colorAttachmentCount,
        final ScissorState defaultScissorState
    ) {
        this.encoder = encoder;
        this.device = device;
        this.hasDepthTexture = hasDepthTexture;
        this.colorAttachmentCount = colorAttachmentCount;
        this.defaultScissorState = defaultScissorState;
        this.scissorState.setFrom(defaultScissorState);
    }

    public boolean hasDepthTexture() {
        return this.hasDepthTexture;
    }

    @Override
    public void pushDebugGroup(final Supplier<String> label) {
        this.device.debugLabels().pushDebugGroup(label);
    }

    @Override
    public void popDebugGroup() {
        this.device.debugLabels().popDebugGroup();
    }

    @Override
    public void setPipeline(final RenderPipeline pipeline) {
        if (this.pipeline == null || this.pipeline.info() != pipeline) {
            this.dirtyUniforms.addAll(this.uniforms.keySet());
            this.dirtyUniforms.addAll(this.samplers.keySet());
        }

        this.pipeline = this.device.getOrCompilePipeline(pipeline);
    }

    @Override
    public void bindTexture(final String name, final @Nullable GpuTextureView textureView, final @Nullable GpuSampler sampler) {
        if (sampler == null) {
            this.samplers.remove(name);
        } else {
            this.samplers.put(name, new GlRenderPass.TextureViewAndSampler((GlTextureView)textureView, (GlSampler)sampler));
        }

        this.dirtyUniforms.add(name);
    }

    @Override
    public void setUniform(final String name, final GpuBuffer value) {
        this.uniforms.put(name, value.slice());
        this.dirtyUniforms.add(name);
    }

    @Override
    public void setUniform(final String name, final GpuBufferSlice value) {
        this.uniforms.put(name, value);
        this.dirtyUniforms.add(name);
    }

    @Override
    public void enableScissor(final int x, final int y, final int width, final int height) {
        this.scissorState.enable(x, y, width, height);
    }

    @Override
    public void disableScissor() {
        this.scissorState.setFrom(this.defaultScissorState);
    }

    public boolean isScissorEnabled() {
        return this.scissorState.enabled();
    }

    public int getScissorX() {
        return this.scissorState.x();
    }

    public int getScissorY() {
        return this.scissorState.y();
    }

    public int getScissorWidth() {
        return this.scissorState.width();
    }

    public int getScissorHeight() {
        return this.scissorState.height();
    }

    @Override
    public void setVertexBuffer(final int slot, final @Nullable GpuBufferSlice vertexBuffer) {
        GpuBuffer inputBuffer = vertexBuffer != null ? vertexBuffer.buffer() : null;
        GpuBuffer existingBuffer = this.vertexBuffers[slot] != null ? this.vertexBuffers[slot].buffer() : null;
        long inputOffset = vertexBuffer != null ? vertexBuffer.offset() : 0L;
        long exitingOffset = this.vertexBuffers[slot] != null ? this.vertexBuffers[slot].offset() : 0L;
        this.vertexBufferDirty |= inputBuffer != existingBuffer || inputOffset != exitingOffset;
        this.vertexBuffers[slot] = vertexBuffer;
    }

    @Override
    public void setIndexBuffer(final @Nullable GpuBuffer indexBuffer, final IndexType indexType) {
        this.indexBuffer = indexBuffer;
        this.indexType = indexType;
    }

    @Override
    public void drawIndexed(final int indexCount, final int instanceCount, final int firstIndex, final int vertexOffset, final int firstInstance) {
        this.encoder.executeDraw(this, vertexOffset, firstIndex, indexCount, this.indexType, instanceCount, firstInstance);
    }

    @Override
    public void multiDrawIndexed(final IntBuffer drawParameters, final int instanceCount, final int firstInstance, final int drawCount) {
        throw new UnsupportedOperationException("OpenGL does not support the multiDrawDirectInterleaved device feature");
    }

    @Override
    public void multiDrawIndexed(final PointerBuffer firstIndexOffsets, final IntBuffer indexCounts, final IntBuffer vertexOffsets, final int drawCount) {
        this.encoder.executeDraws(this, this.indexType, firstIndexOffsets, indexCounts, vertexOffsets, drawCount);
    }

    @Override
    public void drawIndexedIndirect(final GpuBufferSlice commands, final int drawCount) {
        this.encoder.executeDrawIndirect(this, this.indexType, (GlBuffer)commands.buffer(), commands.offset(), drawCount);
    }

    @Override
    public <T> void drawMultipleIndexed(
        final Collection<RenderPass.Draw<T>> draws,
        final @Nullable GpuBuffer defaultIndexBuffer,
        final @Nullable IndexType defaultIndexType,
        final Collection<String> dynamicUniforms,
        final T uniformArgument
    ) {
        this.encoder.executeDrawMultiple(this, draws, defaultIndexBuffer, defaultIndexType, dynamicUniforms, uniformArgument);
    }

    @Override
    public void draw(final int vertexCount, final int instanceCount, final int firstVertex, final int firstInstance) {
        this.encoder.executeDraw(this, firstVertex, 0, vertexCount, null, instanceCount, firstInstance);
    }

    @Override
    public void multiDraw(final IntBuffer drawParameters, final int instanceCount, final int firstInstance, final int drawCount) {
        throw new UnsupportedOperationException("OpenGL does not support the multiDrawDirectInterleaved device feature");
    }

    @Override
    public void multiDraw(final IntBuffer firstVertices, final IntBuffer vertexCounts, final int drawCount) {
        this.encoder.executeDraws(this, null, null, vertexCounts, firstVertices, drawCount);
    }

    @Override
    public void drawIndirect(final GpuBufferSlice commands, final int drawCount) {
        this.encoder.executeDrawIndirect(this, null, (GlBuffer)commands.buffer(), commands.offset(), drawCount);
    }

    @Override
    public void writeTimestamp(final GpuQueryPool pool, final int index) {
        ((GlQueryPool)pool).writeTimestamp(index);
    }

    @OnlyIn(Dist.CLIENT)
    protected record TextureViewAndSampler(GlTextureView view, GlSampler sampler) {
    }
}