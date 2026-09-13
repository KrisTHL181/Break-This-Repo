package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.security.PrivateKey;
import java.security.Signature;
import org.slf4j.Logger;

public interface Signer {
   Logger LOGGER = LogUtils.getLogger();

   byte[] sign(SignatureUpdater var1);

   default byte[] sign(byte[] p_216391_) {
      return this.sign(p_216394_ -> p_216394_.update(p_216391_));
   }

   static Signer from(PrivateKey p_216388_, String p_216389_) {
      return p_216386_ -> {
         try {
            Signature signature = Signature.getInstance(p_216389_);
            signature.initSign(p_216388_);
            p_216386_.update(signature::update);
            return signature.sign();
         } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign message", exception);
         }
      };
   }
}
