package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.properties.Property;

import java.security.Signature;

public interface ServicesKeyInfo {
    int keyBitCount();

    default int signatureBitCount() {
        return keyBitCount();
    }

    Signature signature();

    boolean validateProperty(Property property);
}
