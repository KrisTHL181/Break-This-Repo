package net.minecraft.client.renderer.rendertype;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.oit.OitStage;
import org.jspecify.annotations.Nullable;

public record PreparedRenderType(
   String name,
   RenderPipeline pipeline,
   @Nullable OitPipelineSet oitPipelineSet,
   @Nullable RenderPipeline opaquePartsPipeline,
   GpuBufferSlice dynamicTransforms,
   ScissorState scissorState,
   List<PreparedRenderType.Texture> textures
) {
   public void drawFromBuffer(final StagedVertexBuffer.ExecuteInfo info, final RenderPass renderPass) {
      boolean useImprovedTransparency = Minecraft.getInstance().gameRenderer.useImprovedTransparency();
      RenderPipeline renderPipeline = useImprovedTransparency && RenderSystem.isRenderingLevel && this.opaquePartsPipeline != null
         ? this.opaquePartsPipeline
         : this.pipeline;
      this.draw(info, renderPass, renderPipeline);
   }

   public void drawFromBufferOit(final StagedVertexBuffer.ExecuteInfo info, final OitStage stage, final RenderPass renderPass) {
      if (this.oitPipelineSet == null) {
         throw new IllegalStateException("Render type " + this.name + " does not have OIT pipelines set up.");
      }

      this.draw(info, renderPass, this.oitPipelineSet.getPipeline(stage));
   }

   private void draw(final StagedVertexBuffer.ExecuteInfo info, final RenderPass renderPass, final RenderPipeline renderPipeline) {
      renderPass.pushDebugGroup(() -> "Render Type " + this.name);
      GpuBuffer indexBuffer = info.indexBuffer();
      renderPass.setPipeline(RenderSystem.getCompiledPipeline(renderPipeline));
      if (this.scissorState.enabled()) {
         renderPass.enableScissor(this.scissorState.x(), this.scissorState.y(), this.scissorState.width(), this.scissorState.height());
      }

      RenderSystem.bindDefaultUniforms(renderPass);
      renderPass.setUniform("DynamicTransforms", this.dynamicTransforms);
      renderPass.setVertexBuffer(0, info.vertexBuffer().slice());

      for (PreparedRenderType.Texture texture : this.textures) {
         renderPass.bindTexture(texture.name, texture.textureView, texture.sampler);
      }

      renderPass.setIndexBuffer(indexBuffer, info.indexType());
      renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
      renderPass.popDebugGroup();
   }

   public record Texture(String name, GpuTextureView textureView, GpuSampler sampler) {
   }
}
