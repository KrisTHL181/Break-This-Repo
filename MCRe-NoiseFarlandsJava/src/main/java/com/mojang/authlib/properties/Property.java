package com.mojang.authlib.properties;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.yggdrasil.YggdrasilServicesKeyInfo;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

public record Property(
    @SerializedName("name")
    String name,
    @SerializedName("value")
    String value,
    @SerializedName("signature")
    @Nullable String signature
) {
    public Property(final String name, final String value) {
        this(name, value, null);
    }

    public boolean hasSignature() {
        return signature != null;
    }

    /**
     * @deprecated Use {@link YggdrasilServicesKeyInfo#validateProperty(Property)}
     */
    @Deprecated
    public boolean isSignatureValid(final PublicKey publicKey) {
        try {
            final Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(publicKey);
            signature.update(value.getBytes(StandardCharsets.US_ASCII));
            return signature.verify(Base64.getDecoder().decode(this.signature));
        } catch (final NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        return false;
    }
}
