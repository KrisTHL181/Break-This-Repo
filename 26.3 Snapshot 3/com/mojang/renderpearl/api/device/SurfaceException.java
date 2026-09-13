package com.mojang.renderpearl.api.device;

public class SurfaceException extends Exception {
   public SurfaceException(final String message) {
      super(message);
   }

   public SurfaceException(final Throwable cause) {
      super(cause);
   }
}
