package net.minecraft.client.renderer.rendertype;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class RenderType {
    private static final int MEGABYTE = 1048576;
    public static final int BIG_BUFFER_SIZE = 4194304;
    public static final int SMALL_BUFFER_SIZE = 786432;
    public static final int TRANSIENT_BUFFER_SIZE = 1536;
    private final RenderSetup state;
    private final Optional<RenderType> outline;
    protected final String name;

    private RenderType(final String name, final RenderSetup state) {
        this.name = name;
        this.state = state;
        this.outline = state.outlineProperty == RenderSetup.OutlineProperty.AFFECTS_OUTLINE
            ? state.textures.values().stream().findFirst().map(texture -> RenderTypes.OUTLINE.apply(texture.location(), state.pipeline.isCull()))
            : Optional.empty();
    }

    static RenderType create(final String name, final RenderSetup state) {
        return new RenderType(name, state);
    }

    @Override
    public String toString() {
        return "RenderType[" + this.name + ":" + this.state + "]";
    }

    public boolean hasBlending() {
        return this.state.pipeline.getColorTargetState().blendFunction().isPresent();
    }

    public OutputTarget outputTarget() {
        return this.state.outputTarget;
    }

    public PreparedRenderType prepare() {
        Minecraft minecraft = Minecraft.getInstance();
        List<PreparedRenderType.Texture> textures = this.state
            .prepareTextures(
                minecraft.getTextureManager(),
                RenderSystem.getSamplerCache(),
                minecraft.gameRenderer.overlayTexture().getTextureView(),
                minecraft.gameRenderer.lightmap()
            );
        return new PreparedRenderType(
            this.state.pipeline,
            this.state.outputTarget,
            this.writeDynamicTransforms(RenderSystem.getModelViewMatrixCopy()),
            new ScissorState(RenderSystem.getScissorStateForRenderTypeDraws()),
            textures
        );
    }

    private GpuBufferSlice writeDynamicTransforms(final Matrix4f modelViewMatrix) {
        Consumer<Matrix4f> modelViewModifier = this.state.layeringTransform.getModifier();
        if (modelViewModifier != null) {
            modelViewModifier.accept(modelViewMatrix);
        }

        return RenderSystem.getDynamicUniforms().writeTransform(modelViewMatrix, this.state.textureTransform.createMatrix());
    }

    public VertexFormat format() {
        return this.state.pipeline.getVertexFormatBinding(0);
    }

    public PrimitiveTopology primitiveTopology() {
        return this.state.pipeline.getPrimitiveTopology();
    }

    public Optional<RenderType> outline() {
        return this.outline;
    }

    public boolean isOutline() {
        return this.state.outlineProperty == RenderSetup.OutlineProperty.IS_OUTLINE;
    }

    public RenderPipeline pipeline() {
        return this.state.pipeline;
    }

    public boolean affectsCrumbling() {
        return this.state.affectsCrumbling;
    }

    public boolean canConsolidateConsecutiveGeometry() {
        return !this.primitiveTopology().connectedPrimitives;
    }

    public boolean sortOnUpload() {
        return this.state.sortOnUpload;
    }
}