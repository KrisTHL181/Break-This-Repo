package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4fc;

@OnlyIn(Dist.CLIENT)
public class MovingBlockFeatureRenderer extends RenderTypeFeatureRenderer<MovingBlockFeatureRenderer.Submit> {
    public static final FeatureRendererType<MovingBlockFeatureRenderer.Submit> TYPE = FeatureRendererType.create("Moving Block");
    private final PoseStack poseStack = new PoseStack();

    @Override
    protected void buildGroup(final FeatureFrameContext context, final List<MovingBlockFeatureRenderer.Submit> submits) {
        boolean ambientOcclusion = context.options().ambientOcclusion;
        boolean cutoutLeaves = context.options().cutoutLeaves;
        ModelBlockRenderer blockRenderer = new ModelBlockRenderer(ambientOcclusion, false, context.blockColors());

        for (MovingBlockFeatureRenderer.Submit submit : submits) {
            MovingBlockRenderState movingBlockRenderState = submit.movingBlockRenderState();
            BlockState blockState = movingBlockRenderState.blockState;
            BlockStateModel model = context.blockStateModelSet().get(blockState);
            this.poseStack.setIdentity();
            this.poseStack.mulPose(submit.pose());
            BlockQuadOutput quadOutput = (x, y, z, quad, instance) -> this.putBakedQuad(
                this.poseStack, x, y, z, quad, instance, quad.materialInfo().layer(), submit.outlineColor()
            );
            BlockQuadOutput solidQuadOutput = (x, y, z, quad, instance) -> this.putBakedQuad(
                this.poseStack, x, y, z, quad, instance, ChunkSectionLayer.SOLID, submit.outlineColor()
            );
            BlockQuadOutput blockOutput = ModelBlockRenderer.forceOpaque(cutoutLeaves, blockState) ? solidQuadOutput : quadOutput;
            long blockSeed = blockState.getSeed(movingBlockRenderState.randomSeedPos);
            blockRenderer.tesselateBlock(blockOutput, 0.0F, 0.0F, 0.0F, movingBlockRenderState, movingBlockRenderState.blockPos, blockState, model, blockSeed);
        }
    }

    private void putBakedQuad(
        final PoseStack poseStack,
        final float x,
        final float y,
        final float z,
        final BakedQuad quad,
        final QuadInstance instance,
        final ChunkSectionLayer layer,
        final int outlineColor
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);

        RenderType renderType = switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
        VertexConsumer buffer;
        if (outlineColor != 0 && renderType.outline().isPresent()) {
            instance.setColor(outlineColor);
            buffer = this.getVertexBuilder(renderType.outline().get());
        } else {
            buffer = this.getVertexBuilder(renderType);
        }

        buffer.putBakedQuad(poseStack.last(), quad, instance);
        poseStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    public record Submit(Matrix4fc pose, MovingBlockRenderState movingBlockRenderState, int outlineColor) implements TranslucentSubmit {
        @Override
        public float distanceToCameraSq() {
            return TranslucentSubmit.computeDistanceToCameraSq(this.pose, 0.5F, 0.5F, 0.5F);
        }

        @Override
        public FeatureRendererType<MovingBlockFeatureRenderer.Submit> featureType() {
            return MovingBlockFeatureRenderer.TYPE;
        }
    }
}