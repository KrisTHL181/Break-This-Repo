package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

/**
 * MCRe NoiseFarlands：26.3 MultiDrawIndirect 移植。
 * 抽象基类 + 双路径：
 *  - DrawIndirect：Vulkan MultiDrawIndirect（drawIndexedIndirect 批量绘制，instanced section 数据）
 *  - DrawSeparate：原版逐 draw 提交（fallback，OpenGL/不支持 MultiDraw 时）
 */
@OnlyIn(Dist.CLIENT)
public abstract class ChunkSectionsToRender {
    // MCRe：Vulkan maxDrawIndirectCount 未在 26.2 DeviceLimits 暴露，用保守分块上限
    private static final int MAX_DRAW_INDIRECT_COUNT = 65536;
    private final int maxIndicesRequired;
    private final GpuTextureView textureView;
    private final GpuBufferSlice terrainTransformUBO;

    private ChunkSectionsToRender(final GpuTextureView textureView, final GpuBufferSlice terrainTransformUBO, final int maxIndicesRequired) {
        this.textureView = textureView;
        this.terrainTransformUBO = terrainTransformUBO;
        this.maxIndicesRequired = maxIndicesRequired;
    }

    protected abstract void render(
        final ChunkSectionLayer layer,
        final RenderPass renderPass,
        @Nullable GpuBuffer defaultIndexBuffer,
        @Nullable IndexType defaultIndexType,
        final @Nullable RenderPipeline renderPipelineOverride,
        final @Nullable RenderPipeline renderPipelineOverrideMultidraw
    );

    public void renderGroup(final ChunkSectionLayerGroup group, final GpuSampler sampler) {
        RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer defaultIndexBuffer = this.maxIndicesRequired == 0 ? null : autoIndices.getBuffer(this.maxIndicesRequired);
        IndexType defaultIndexType = this.maxIndicesRequired == 0 ? null : autoIndices.type();
        ChunkSectionLayer[] layers = group.layers();
        Minecraft minecraft = Minecraft.getInstance();
        boolean wireframe = SharedConstants.DEBUG_HOTKEYS && minecraft.wireframe;
        RenderTarget renderTarget = group.outputTarget();

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                    () -> "Section layers for " + group.label(),
                    renderTarget.getColorTextureView(),
                    Optional.empty(),
                    renderTarget.getDepthTextureView(),
                    OptionalDouble.empty()
                )) {
            RenderSystem.bindDefaultUniforms(renderPass);
            // MCRe：26.3 MultiDrawIndirect——共享 TerrainUniform（ModelView 矩阵 + 纹理尺寸）
            renderPass.setUniform("TerrainUniform", this.terrainTransformUBO);
            renderPass.bindTexture("Sampler0", this.textureView, sampler);
            renderPass.bindTexture("Sampler2", minecraft.gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));

            for (ChunkSectionLayer layer : layers) {
                this.render(
                    layer,
                    renderPass,
                    defaultIndexBuffer,
                    defaultIndexType,
                    wireframe ? RenderPipelines.WIREFRAME : null,
                    wireframe ? RenderPipelines.WIREFRAME_MULTIDRAW : null
                );
            }
        }
    }

    /** MultiDrawIndirect 路径：一个 indirect command buffer 批量绘制整组区块 */
    public static final class DrawIndirect extends ChunkSectionsToRender {
        private final EnumMap<ChunkSectionLayer, List<ChunkSectionsToRender.GpuMultiDrawIndexedIndirect>> drawGroupsPerLayer;
        private final GpuBufferSlice chunkSectionInfos;

        public DrawIndirect(
            final GpuTextureView textureView,
            final GpuBufferSlice terrainTransformUBO,
            final EnumMap<ChunkSectionLayer, List<ChunkSectionsToRender.GpuMultiDrawIndexedIndirect>> drawGroupsPerLayer,
            final int maxIndicesRequired,
            final GpuBufferSlice chunkSectionInfos
        ) {
            super(textureView, terrainTransformUBO, maxIndicesRequired);
            this.drawGroupsPerLayer = drawGroupsPerLayer;
            this.chunkSectionInfos = chunkSectionInfos;
        }

        @Override
        protected void render(
            final ChunkSectionLayer layer,
            final RenderPass renderPass,
            final @Nullable GpuBuffer defaultIndexBuffer,
            final @Nullable IndexType defaultIndexType,
            final @Nullable RenderPipeline renderPipelineOverride,
            final @Nullable RenderPipeline renderPipelineOverrideMultidraw
        ) {
            renderPass.setPipeline(renderPipelineOverrideMultidraw != null ? renderPipelineOverrideMultidraw : layer.pipelineMultiDraw());
            List<ChunkSectionsToRender.GpuMultiDrawIndexedIndirect> drawGroups = this.drawGroupsPerLayer.get(layer);
            if (drawGroups == null) {
                return;
            }

            if (!drawGroups.isEmpty()) {
                renderPass.setVertexBuffer(1, this.chunkSectionInfos);
            }

            for (ChunkSectionsToRender.GpuMultiDrawIndexedIndirect indirectDraw : drawGroups) {
                if (indirectDraw.drawCount() > 0) {
                    renderPass.setVertexBuffer(0, indirectDraw.vertexBuffer());
                    IndexType indexType = indirectDraw.indexType() == null ? defaultIndexType : indirectDraw.indexType();
                    renderPass.setIndexBuffer(indirectDraw.indexBuffer() == null ? defaultIndexBuffer : indirectDraw.indexBuffer().buffer(), indexType);
                    GpuBuffer buffer = indirectDraw.indirectCommandBuffer().buffer();
                    long startOffset = indirectDraw.indirectCommandBuffer().offset();
                    int remainingDrawCount = indirectDraw.drawCount();

                    while (remainingDrawCount > 0) {
                        int passDrawCount = Integer.min(remainingDrawCount, MAX_DRAW_INDIRECT_COUNT);
                        long length = passDrawCount * 20L;
                        GpuBufferSlice passSlice = buffer.slice(startOffset, length);
                        renderPass.drawIndexedIndirect(passSlice, passDrawCount);
                        remainingDrawCount -= passDrawCount;
                        startOffset += length;
                    }
                }
            }
        }
    }

    /** 原版逐 draw 提交路径（fallback） */
    public static final class DrawSeparate extends ChunkSectionsToRender {
        private final Map<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> drawsPerLayer;
        private final GpuBufferSlice[] chunkSectionInfos;

        public DrawSeparate(
            final GpuTextureView textureView,
            final GpuBufferSlice terrainTransformUBO,
            final Map<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> drawsPerLayer,
            final int maxIndicesRequired,
            final GpuBufferSlice[] chunkSectionInfos
        ) {
            super(textureView, terrainTransformUBO, maxIndicesRequired);
            this.drawsPerLayer = drawsPerLayer;
            this.chunkSectionInfos = chunkSectionInfos;
        }

        @Override
        protected void render(
            final ChunkSectionLayer layer,
            final RenderPass renderPass,
            final @Nullable GpuBuffer defaultIndexBuffer,
            final @Nullable IndexType defaultIndexType,
            final @Nullable RenderPipeline renderPipelineOverride,
            final @Nullable RenderPipeline renderPipelineOverrideMultidraw
        ) {
            renderPass.setPipeline(renderPipelineOverride != null ? renderPipelineOverride : layer.pipeline());
            List<RenderPass.Draw<GpuBufferSlice[]>> draws = this.drawsPerLayer.get(layer);
            if (draws == null || draws.isEmpty()) {
                return;
            }

            if (layer == ChunkSectionLayer.TRANSLUCENT) {
                draws = draws.reversed();
            }

            renderPass.drawMultipleIndexed(draws, defaultIndexBuffer, defaultIndexType, List.of("ChunkSection"), this.chunkSectionInfos);
        }
    }

    public record GpuMultiDrawIndexedIndirect(
        GpuBufferSlice vertexBuffer, @Nullable GpuBufferSlice indexBuffer, @Nullable IndexType indexType, GpuBufferSlice indirectCommandBuffer, int drawCount
    ) {
    }
}
