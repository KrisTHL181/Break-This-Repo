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
    SignatureValidator NO_VALIDATION = (payload, signature) -> true;
    Logger LOGGER = LogUtils.getLogger();

    boolean validate(SignatureUpdater updater, byte[] signature);

    default boolean validate(final byte[] payload, final byte[] signature) {
        return this.validate(output -> output.update(payload), signature);
    }

    private static boolean verifySignature(final SignatureUpdater updater, final byte[] signature, final Signature verifier) throws SignatureException {
        updater.update(verifier::update);
        return verifier.verify(signature);
    }

    static SignatureValidator from(final PublicKey publicKey, final String algorithm) {
        return (updater, signature) -> {
            try {
                Signature verifier = Signature.getInstance(algorithm);
                verifier.initVerify(publicKey);
                return verifySignature(updater, signature, verifier);
            } catch (Exception e) {
                LOGGER.error("Failed to verify signature", e);
                return false;
            }
        };
    }

    static @Nullable SignatureValidator from(final ServicesKeySet keySet, final ServicesKeyType type) {
        Collection<ServicesKeyInfo> keys = keySet.keys(type);
        return keys.isEmpty() ? null : (updater, signature) -> keys.stream().anyMatch(key -> {
            Signature verifier = key.signature();

            try {
                return verifySignature(updater, signature, verifier);
            } catch (SignatureException e) {
                LOGGER.error("Failed to verify Services signature", e);
                return false;
            }
        });
    }
}