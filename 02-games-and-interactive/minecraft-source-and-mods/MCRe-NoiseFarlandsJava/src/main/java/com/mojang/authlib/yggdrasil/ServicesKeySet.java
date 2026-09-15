package com.mojang.authlib.yggdrasil;

import java.util.function.Supplier;
import java.util.Collection;
import java.util.List;

public interface ServicesKeySet {
    ServicesKeySet EMPTY = type -> List.of();

    static ServicesKeySet lazy(final Supplier<ServicesKeySet> supplier) {
        return type -> supplier.get().keys(type);
    }

    Collection<ServicesKeyInfo> keys(ServicesKeyType type);
}
