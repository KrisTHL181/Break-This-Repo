package com.mojang.renderpearl.api.device;

public class GpuOutOfMemoryException extends RuntimeException {
   public GpuOutOfMemoryException(final String message) {
      super(message);
   }
}
