package net.minecraft.client.renderer;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

@OnlyIn(Dist.CLIENT)
public class DebugCrosshairRenderer implements AutoCloseable {
    private static final float CROSSHAIR_SCALE = 0.01F;
    private static final int CROSSHAIR_INDEX_COUNT = 36;
    private final GpuBuffer crosshairBuffer;
    private final RenderSystem.AutoStorageIndexBuffer crosshairIndicies = RenderSystem.getSequentialBuffer(PrimitiveTopology.LINES);

    public DebugCrosshairRenderer() {
        try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH.getVertexSize() * 12 * 2)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH);
            bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(1.0F, 0.0F, 0.0F).setLineWidth(4.0F);
            bufferBuilder.addVertex(1.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(1.0F, 0.0F, 0.0F).setLineWidth(4.0F);
            bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(0.0F, 1.0F, 0.0F).setLineWidth(4.0F);
            bufferBuilder.addVertex(0.0F, 1.0F, 0.0F).setColor(-16777216).setNormal(0.0F, 1.0F, 0.0F).setLineWidth(4.0F);
            bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(0.0F, 0.0F, 1.0F).setLineWidth(4.0F);
            bufferBuilder.addVertex(0.0F, 0.0F, 1.0F).setColor(-16777216).setNormal(0.0F, 0.0F, 1.0F).setLineWidth(4.0F);
            bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-65536).setNormal(1.0F, 0.0F, 0.0F).setLineWidth(2.0F);
            bufferBuilder.addVertex(1.0F, 0.0F, 0.0F).setColor(-65536).setNormal(1.0F, 0.0F, 0.0F).setLineWidth(2.0F);
            bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-16711936).setNormal(0.0F, 1.0F, 0.0F).setLineWidth(2.0F);
            bufferBuilder.addVertex(0.0F, 1.0F, 0.0F).setColor(-16711936).setNormal(0.0F, 1.0F, 0.0F).setLineWidth(2.0F);
            bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-8421377).setNormal(0.0F, 0.0F, 1.0F).setLineWidth(2.0F);
            bufferBuilder.addVertex(0.0F, 0.0F, 1.0F).setColor(-8421377).setNormal(0.0F, 0.0F, 1.0F).setLineWidth(2.0F);

            try (MeshData meshData = bufferBuilder.buildOrThrow()) {
                this.crosshairBuffer = RenderSystem.getDevice().createBuffer(() -> "Crosshair vertex buffer", 32, meshData.vertexBuffer());
            }
        }
    }

    @Override
    public void close() {
        this.crosshairBuffer.close();
    }

    public void render(final CameraRenderState cameraState, final int guiScale) {
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.translate(0.0F, 0.0F, -1.0F);
        modelViewStack.rotateX(cameraState.xRot * (float) (Math.PI / 180.0));
        modelViewStack.rotateY(cameraState.yRot * (float) (Math.PI / 180.0));
        float crosshairScale = 0.01F * guiScale;
        modelViewStack.scale(-crosshairScale, crosshairScale, -crosshairScale);
        RenderPipeline renderPipelineOutline = RenderPipelines.LINES;
        RenderPipeline renderPipelineFill = RenderPipelines.LINES_DEPTH_BIAS;
        RenderTarget mainRenderTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainRenderTarget.getColorTextureView();
        GpuTextureView depthTexture = mainRenderTarget.getDepthTextureView();
        GpuBuffer indexBuffer = this.crosshairIndicies.getBuffer(36);
        GpuBufferSlice dynamicTransform = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(modelViewStack));

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "3d crosshair", colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty())) {
            renderPass.setPipeline(renderPipelineOutline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setVertexBuffer(0, this.crosshairBuffer.slice());
            renderPass.setIndexBuffer(indexBuffer, this.crosshairIndicies.type());
            renderPass.setUniform("DynamicTransforms", dynamicTransform);
            renderPass.drawIndexed(18, 1, 0, 0, 0);
            renderPass.setPipeline(renderPipelineFill);
            renderPass.drawIndexed(18, 1, 18, 0, 0);
        }

        modelViewStack.popMatrix();
    }
}