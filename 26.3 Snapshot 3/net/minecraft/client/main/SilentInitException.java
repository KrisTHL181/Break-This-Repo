package net.minecraft.client.main;

public class SilentInitException extends RuntimeException {
   public SilentInitException(final String message) {
      super(message);
   }

   public SilentInitException(final String message, final Throwable cause) {
      super(message, cause);
   }
}
