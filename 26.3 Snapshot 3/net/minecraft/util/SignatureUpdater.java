package net.minecraft.util;

import java.security.SignatureException;

@FunctionalInterface
public interface SignatureUpdater {
   void update(SignatureUpdater.Output output) throws SignatureException;

   @FunctionalInterface
   interface Output {
      void update(byte[] payload) throws SignatureException;
   }
}
