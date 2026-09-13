package net.minecraft.util;

import com.mojang.authlib.yggdrasil.ServicesKeyInfo;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.logging.LogUtils;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Collection;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public interface SignatureValidator {
   SignatureValidator NO_VALIDATION = (p_216352_, p_216353_) -> true;
   Logger LOGGER = LogUtils.getLogger();

   boolean validate(SignatureUpdater var1, byte[] var2);

   default boolean validate(byte[] p_216376_, byte[] p_216377_) {
      return this.validate(p_216374_ -> p_216374_.update(p_216376_), p_216377_);
   }

   private static boolean verifySignature(SignatureUpdater p_216355_, byte[] p_216356_, Signature p_216357_) throws SignatureException {
      p_216355_.update(p_216357_::update);
      return p_216357_.verify(p_216356_);
   }

   static SignatureValidator from(PublicKey p_216370_, String p_216371_) {
      return (p_216367_, p_216368_) -> {
         try {
            Signature signature = Signature.getInstance(p_216371_);
            signature.initVerify(p_216370_);
            return verifySignature(p_216367_, p_216368_, signature);
         } catch (Exception exception) {
            LOGGER.error("Failed to verify signature", exception);
            return false;
         }
      };
   }

   static @Nullable SignatureValidator from(ServicesKeySet p_285388_, ServicesKeyType p_285383_) {
      Collection<ServicesKeyInfo> collection = p_285388_.keys(p_285383_);
      return collection.isEmpty() ? null : (p_284690_, p_284691_) -> collection.stream().anyMatch(p_216361_ -> {
         Signature signature = p_216361_.signature();

         try {
            return verifySignature(p_284690_, p_284691_, signature);
         } catch (SignatureException signatureexception) {
            LOGGER.error("Failed to verify Services signature", signatureexception);
            return false;
         }
      });
   }
}
