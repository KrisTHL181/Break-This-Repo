package com.mojang.realmsclient.client;

import java.net.Proxy;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RealmsClientConfig {
    private static @Nullable Proxy proxy;

    public static @Nullable Proxy getProxy() {
        return proxy;
    }

    public static void setProxy(final Proxy proxy) {
        if (RealmsClientConfig.proxy == null) {
            RealmsClientConfig.proxy = proxy;
        }
    }
}