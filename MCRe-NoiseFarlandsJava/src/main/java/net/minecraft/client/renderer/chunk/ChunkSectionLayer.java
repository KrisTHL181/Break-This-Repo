package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Locale;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum ChunkSectionLayer {
    // MCRe：26.3 MultiDrawIndirect 移植——每个 layer 增加 MultiDraw pipeline 变体（instanced section 数据）
    SOLID(RenderPipelines.SOLID_TERRAIN, RenderPipelines.SOLID_TERRAIN_MULTIDRAW, 4194304, false),
    CUTOUT(RenderPipelines.CUTOUT_TERRAIN, RenderPipelines.CUTOUT_TERRAIN_MULTIDRAW, 4194304, false),
    TRANSLUCENT(RenderPipelines.TRANSLUCENT_TERRAIN, RenderPipelines.TRANSLUCENT_TERRAIN_MULTIDRAW, 786432, true);

    private final RenderPipeline pipeline;
    private final RenderPipeline pipelineMultiDraw;
    private final int bufferSize;
    private final boolean translucent;
    private final String label;

    ChunkSectionLayer(final RenderPipeline pipeline, final RenderPipeline pipelineMultiDraw, final int bufferSize, final boolean translucent) {
        this.pipeline = pipeline;
        this.pipelineMultiDraw = pipelineMultiDraw;
        this.bufferSize = bufferSize;
        this.translucent = translucent;
        this.label = this.toString().toLowerCase(Locale.ROOT);
    }

    public static ChunkSectionLayer byTransparency(final Transparency transparency) {
        if (transparency.hasTranslucent()) {
            return TRANSLUCENT;
        } else {
            return transparency.hasTransparent() ? CUTOUT : SOLID;
        }
    }

    public RenderPipeline pipeline() {
        return this.pipeline;
    }

    public RenderPipeline pipelineMultiDraw() {
        return this.pipelineMultiDraw;
    }

    public int bufferSize() {
        return this.bufferSize;
    }

    public String label() {
        return this.label;
    }

    public boolean translucent() {
        return this.translucent;
    }

    public VertexFormat vertexFormat() {
        return this.pipeline.getVertexFormatBinding(0);
    }
}