package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.EnumMap;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ChunkSectionsToRender(
   GpuTextureView textureView,
   EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> drawsPerLayer,
   int maxIndicesRequired,
   GpuBufferSlice[] chunkSectionInfos
) {
   public void renderGroup(ChunkSectionLayerGroup p_406533_, GpuSampler p_455406_) {
      RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
      GpuBuffer gpubuffer = this.maxIndicesRequired == 0 ? null : rendersystem$autostorageindexbuffer.getBuffer(this.maxIndicesRequired);
      VertexFormat.IndexType vertexformat$indextype = this.maxIndicesRequired == 0 ? null : rendersystem$autostorageindexbuffer.type();
      ChunkSectionLayer[] achunksectionlayer = p_406533_.layers();
      Minecraft minecraft = Minecraft.getInstance();
      boolean flag = SharedConstants.DEBUG_HOTKEYS && minecraft.wireframe;
      RenderTarget rendertarget = p_406533_.outputTarget();

      try (RenderPass renderpass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(
               () -> "Section layers for " + p_406533_.label(),
               rendertarget.getColorTextureView(),
               OptionalInt.empty(),
               rendertarget.getDepthTextureView(),
               OptionalDouble.empty()
            )) {
         RenderSystem.bindDefaultUniforms(renderpass);
         renderpass.bindTexture(
            "Sampler2", minecraft.gameRenderer.lightTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
         );

         for (ChunkSectionLayer chunksectionlayer : achunksectionlayer) {
            List<RenderPass.Draw<GpuBufferSlice[]>> list = this.drawsPerLayer.get(chunksectionlayer);
            if (!list.isEmpty()) {
               if (chunksectionlayer == ChunkSectionLayer.TRANSLUCENT) {
                  list = list.reversed();
               }

               renderpass.setPipeline(flag ? RenderPipelines.WIREFRAME : chunksectionlayer.pipeline());
               renderpass.bindTexture("Sampler0", this.textureView, p_455406_);
               renderpass.drawMultipleIndexed(list, gpubuffer, vertexformat$indextype, List.of("ChunkSection"), this.chunkSectionInfos);
            }
         }
      }
   }
}
