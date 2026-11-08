package com.elfmcys.yesstevemodel.util;

import io.netty.util.internal.ObjectCleaner;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ResourceCleanupHelper {
    public static <T> void registerCleanup(Object owner, T resource, Consumer<T> cleanup) {
        ObjectCleaner.register(owner, () -> cleanup.accept(resource));
    }

    public static <T0, T1> void registerBiCleanup(Object owner, T0 resource0, T1 resource1, BiConsumer<T0, T1> cleanup) {
        ObjectCleaner.register(owner, () -> cleanup.accept(resource0, resource1));
    }
}