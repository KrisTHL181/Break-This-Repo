package com.mojang.renderpearl.api.commands;

public interface GpuFence extends AutoCloseable {
   @Override
   void close();

   boolean awaitCompletion(final long timeoutNS);
}
