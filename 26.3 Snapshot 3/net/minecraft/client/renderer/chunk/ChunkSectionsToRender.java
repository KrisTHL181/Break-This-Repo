package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitRenderPassProvider;
import net.minecraft.client.renderer.oit.OitStage;
import org.jspecify.annotations.Nullable;

public record ChunkSectionsToRender(
   GpuTextureView textureView,
   EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> drawGroupsPerLayer,
   int maxIndicesRequired,
   GpuBufferSlice[] chunkSectionInfos
) {
   public void renderGroup(final ChunkSectionLayerGroup group, final RenderPass renderPass, final GpuSampler sampler, final boolean renderWireframeTerrain) {
      GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
      GpuTextureView lightmap = gameRenderer.lightmap();
      this.renderLayers(group.layers(), sampler, renderPass, lightmap, renderWireframeTerrain ? RenderPipelines.WIREFRAME : null, true);
   }

   public void renderOit(final GpuSampler sampler, final OitStage stage, final OitRenderPassProvider.Parameters params, final GpuTextureView lightmap) {
      try (RenderPass renderPass = OitRenderPassProvider.createRenderPass(stage, () -> "Terrain", params)) {
         this.renderLayers(ChunkSectionLayerGroup.TRANSLUCENT.layers(), sampler, renderPass, lightmap, RenderPipelines.OIT_TERRAIN.getPipeline(stage), false);
      }
   }

   private void renderLayers(
      final ChunkSectionLayer[] layers,
      final GpuSampler sampler,
      final RenderPass renderPass,
      final GpuTextureView lightmap,
      final @Nullable RenderPipeline renderPipelineOverride,
      final boolean shouldReverseTranslucent
   ) {
      RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
      GpuBuffer defaultIndexBuffer = this.maxIndicesRequired == 0 ? null : autoIndices.getBuffer();
      IndexType defaultIndexType = this.maxIndicesRequired == 0 ? null : autoIndices.type();
      renderPass.bindTexture("Sampler0", this.textureView, sampler);
      renderPass.bindTexture("Sampler2", lightmap, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));

      for (ChunkSectionLayer layer : layers) {
         renderPass.pushDebugGroup(() -> "Terrain layer: " + layer.label());
         renderPass.setPipeline(RenderSystem.getCompiledPipeline(renderPipelineOverride != null ? renderPipelineOverride : layer.pipeline()));
         Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>> drawGroup = this.drawGroupsPerLayer.get(layer);
         ObjectIterator var15 = drawGroup.values().iterator();

         while (var15.hasNext()) {
            List<RenderPass.Draw<GpuBufferSlice[]>> draws = (List<RenderPass.Draw<GpuBufferSlice[]>>)var15.next();
            if (!draws.isEmpty()) {
               if (shouldReverseTranslucent && layer == ChunkSectionLayer.TRANSLUCENT) {
                  draws = draws.reversed();
               }

               renderPass.drawMultipleIndexed(draws, defaultIndexBuffer, defaultIndexType, List.of("ChunkSection"), this.chunkSectionInfos);
            }
         }

         renderPass.popDebugGroup();
      }
   }
}
