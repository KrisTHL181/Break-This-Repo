package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.security.PrivateKey;
import java.security.Signature;
import org.slf4j.Logger;

public interface Signer {
   Logger LOGGER = LogUtils.getLogger();

   byte[] sign(SignatureUpdater updater);

   default byte[] sign(final byte[] payload) {
      return this.sign(output -> output.update(payload));
   }

   static Signer from(final PrivateKey privateKey, final String algorithm) {
      return updater -> {
         try {
            Signature signer = Signature.getInstance(algorithm);
            signer.initSign(privateKey);
            updater.update(signer::update);
            return signer.sign();
         } catch (Exception e) {
            throw new IllegalStateException("Failed to sign message", e);
         }
      };
   }
}
