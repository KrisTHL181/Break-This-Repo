package com.mojang.blaze3d.pipeline;

import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public class PipelineCache implements AutoCloseable {
   private final GpuDevice device;
   private final ShaderSource shaderSource;
   private final Map<RenderPipeline, CompiledRenderPipeline> cache = new Reference2ReferenceOpenHashMap();

   public PipelineCache(final GpuDevice device, final ShaderSource shaderSource) {
      this.device = device;
      this.shaderSource = shaderSource;
   }

   public @Nullable CompiledRenderPipeline get(final RenderPipeline pipeline) {
      CompiledRenderPipeline cachedPipeline = this.cache.get(pipeline);
      if (cachedPipeline != null) {
         return cachedPipeline;
      }

      CompiledRenderPipeline newPipeline = this.device.compilePipeline(pipeline, this.shaderSource);
      if (newPipeline == null) {
         return null;
      }

      this.cache.put(pipeline, newPipeline);
      return newPipeline;
   }

   public void clear() {
      this.cache.values().forEach(CompiledRenderPipeline::close);
      this.cache.clear();
   }

   @Override
   public void close() {
      this.clear();
   }
}
