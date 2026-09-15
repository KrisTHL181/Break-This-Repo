package net.minecraft.util;

import java.security.SignatureException;

@FunctionalInterface
public interface SignatureUpdater {
   void update(SignatureUpdater.Output var1) throws SignatureException;

   @FunctionalInterface
   interface Output {
      void update(byte[] var1) throws SignatureException;
   }
}
