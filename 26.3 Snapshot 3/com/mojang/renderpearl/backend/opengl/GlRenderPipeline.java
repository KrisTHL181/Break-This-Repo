package com.mojang.renderpearl.backend.opengl;

import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

public final class GlRenderPipeline implements CompiledRenderPipeline {
   private final GlDevice device;
   private final RenderPipeline info;
   private final GlProgram program;
   private boolean closed = false;

   GlRenderPipeline(final GlDevice device, final RenderPipeline info, final GlProgram program) {
      this.device = device;
      this.info = info;
      this.program = program;
   }

   @Override
   public boolean isClosed() {
      return this.closed;
   }

   @Override
   public void close() {
      if (!this.closed) {
         this.closed = true;
         this.program.close();
         this.device.markAmdShaderCompilerAngry();
      }
   }

   @Override
   public RenderPipeline info() {
      return this.info;
   }

   public GlProgram program() {
      return this.program;
   }
}
