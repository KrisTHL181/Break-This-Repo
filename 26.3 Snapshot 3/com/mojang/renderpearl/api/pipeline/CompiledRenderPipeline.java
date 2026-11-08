package com.mojang.renderpearl.api.pipeline;

public interface CompiledRenderPipeline extends AutoCloseable {
   RenderPipeline info();

   boolean isClosed();

   @Override
   void close();
}
