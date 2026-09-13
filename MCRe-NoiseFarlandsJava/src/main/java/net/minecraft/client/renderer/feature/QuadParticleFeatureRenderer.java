package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class QuadParticleFeatureRenderer implements FeatureRenderer<QuadParticleFeatureRenderer.Submit> {
    public static final FeatureRendererType<QuadParticleFeatureRenderer.Submit> TYPE = FeatureRendererType.create("Particle");
    private final List<QuadParticleFeatureRenderer.PreparedGroup> groups = new ArrayList<>();
    private @Nullable GpuBufferSlice dynamicTransforms;

    @Override
    public void prepareGroup(final FeatureFrameContext context, final List<QuadParticleFeatureRenderer.Submit> submits, final boolean strictlyOrdered) {
        if (!submits.isEmpty()) {
            StagedVertexBuffer stagedVertexBuffer = context.stagedVertexBuffer();
            Map<SingleQuadParticle.Layer, StagedVertexBuffer.Draw> drawByLayer = new IdentityHashMap<>();

            for (QuadParticleFeatureRenderer.Submit submit : submits) {
                QuadParticleRenderState particles = submit.particles();
                if (!particles.isEmpty()) {
                    for (SingleQuadParticle.Layer layer : particles.layers()) {
                        if (layer.translucent() == submit.translucent()) {
                            StagedVertexBuffer.Draw draw = drawByLayer.computeIfAbsent(
                                layer, var1 -> stagedVertexBuffer.appendDraw(DefaultVertexFormat.PARTICLE, PrimitiveTopology.QUADS, null)
                            );
                            particles.buildLayer(layer, stagedVertexBuffer.getVertexBuilder(draw));
                        }
                    }
                }
            }

            boolean translucent = submits.getFirst().translucent();
            this.groups.add(new QuadParticleFeatureRenderer.PreparedGroup(drawByLayer, translucent));
        }
    }

    @Override
    public void finishPrepare(final FeatureFrameContext context) {
        this.dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy());
    }

    @Override
    public void executeGroup(
        final FeatureFrameContext context, final int groupIndex, final List<QuadParticleFeatureRenderer.Submit> submits, final boolean strictlyOrdered
    ) {
        QuadParticleFeatureRenderer.PreparedGroup group = this.groups.get(groupIndex);
        GpuDevice device = RenderSystem.getDevice();
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.gameRenderer.mainRenderTarget();
        RenderTarget particleTarget = minecraft.levelRenderer.particlesTarget();
        boolean useParticleTarget = particleTarget != null && group.translucent;
        GpuTextureView colorTextureView = useParticleTarget ? particleTarget.getColorTextureView() : mainTarget.getColorTextureView();
        GpuTextureView depthTextureView = useParticleTarget ? particleTarget.getDepthTextureView() : mainTarget.getDepthTextureView();

        try (RenderPass renderPass = device.createCommandEncoder()
                .createRenderPass(
                    () -> "Particles - " + (group.translucent ? "Translucent" : "Solid"),
                    colorTextureView,
                    Optional.empty(),
                    depthTextureView,
                    OptionalDouble.empty()
                )) {
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", Objects.requireNonNull(this.dynamicTransforms));
            renderPass.bindTexture("Sampler2", context.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            drawLayers(context.stagedVertexBuffer(), group.layers, renderPass, context.textureManager());
        }
    }

    private static void drawLayers(
        final StagedVertexBuffer stagedBuffer,
        final Map<SingleQuadParticle.Layer, StagedVertexBuffer.Draw> layers,
        final RenderPass renderPass,
        final TextureManager textureManager
    ) {
        for (Entry<SingleQuadParticle.Layer, StagedVertexBuffer.Draw> entry : layers.entrySet()) {
            StagedVertexBuffer.ExecuteInfo executeInfo = stagedBuffer.getExecuteInfo(entry.getValue());
            if (executeInfo != null) {
                renderPass.setPipeline(entry.getKey().pipeline());
                renderPass.setVertexBuffer(0, executeInfo.vertexBuffer().slice());
                renderPass.setIndexBuffer(executeInfo.indexBuffer(), executeInfo.indexType());
                AbstractTexture texture = textureManager.getTexture(entry.getKey().textureAtlasLocation());
                renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
                renderPass.drawIndexed(executeInfo.indexCount(), 1, executeInfo.firstIndex(), executeInfo.baseVertex(), 0);
            }
        }
    }

    @Override
    public void finishExecute(final FeatureFrameContext context) {
        this.groups.clear();
        this.dynamicTransforms = null;
    }

    @OnlyIn(Dist.CLIENT)
    private record PreparedGroup(Map<SingleQuadParticle.Layer, StagedVertexBuffer.Draw> layers, boolean translucent) {
    }

    @OnlyIn(Dist.CLIENT)
    public record Submit(QuadParticleRenderState particles, boolean translucent) implements SubmitNode {
        @Override
        public FeatureRendererType<QuadParticleFeatureRenderer.Submit> featureType() {
            return QuadParticleFeatureRenderer.TYPE;
        }
    }
}