package net.minecraft.client.renderer.rendertype;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record PreparedRenderType(
    RenderPipeline pipeline, OutputTarget outputTarget, GpuBufferSlice dynamicTransforms, ScissorState scissorState, List<PreparedRenderType.Texture> textures
) {
    public void drawFromBuffer(final StagedVertexBuffer.ExecuteInfo info) {
        this.drawFromBuffer(info.vertexBuffer(), info.indexBuffer(), info.indexType(), info.baseVertex(), info.firstIndex(), info.indexCount());
    }

    public void drawFromBuffer(
        final GpuBuffer vertexBuffer, final GpuBuffer indexBuffer, final IndexType indexType, final int baseVertex, final int firstIndex, final int indexCount
    ) {
        RenderTarget renderTarget = this.outputTarget.getRenderTarget();
        GpuTextureView colorTexture = RenderSystem.outputColorTextureOverride != null
            ? RenderSystem.outputColorTextureOverride
            : renderTarget.getColorTextureView();
        GpuTextureView depthTexture = renderTarget.useDepth
            ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : renderTarget.getDepthTextureView())
            : null;

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "Immediate draw with " + this.pipeline, colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty())) {
            renderPass.setPipeline(this.pipeline);
            if (this.scissorState.enabled()) {
                renderPass.enableScissor(this.scissorState.x(), this.scissorState.y(), this.scissorState.width(), this.scissorState.height());
            }

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", this.dynamicTransforms);
            renderPass.setVertexBuffer(0, vertexBuffer.slice());

            for (PreparedRenderType.Texture texture : this.textures) {
                renderPass.bindTexture(texture.name, texture.textureView, texture.sampler);
            }

            renderPass.setIndexBuffer(indexBuffer, indexType);
            renderPass.drawIndexed(indexCount, 1, firstIndex, baseVertex, 0);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Texture(String name, GpuTextureView textureView, GpuSampler sampler) {
    }
}