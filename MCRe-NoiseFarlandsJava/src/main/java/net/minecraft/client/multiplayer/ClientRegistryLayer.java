package net.minecraft.client.multiplayer;

import java.util.List;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum ClientRegistryLayer {
    STATIC,
    REMOTE;

    private static final List<ClientRegistryLayer> VALUES = List.of(values());
    private static final RegistryAccess.Frozen STATIC_ACCESS = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    public static LayeredRegistryAccess<ClientRegistryLayer> createRegistryAccess() {
        return new LayeredRegistryAccess<>(VALUES).replaceFrom(STATIC, STATIC_ACCESS);
    }
}