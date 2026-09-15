package com.mojang.renderpearl.api.device;

public class GpuDeviceLossException extends RuntimeException {
   public GpuDeviceLossException(final String message) {
      super(message);
   }
}
