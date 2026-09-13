package net.minecraft.util.debug;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DebugGameEventListenerInfo(int listenerRadius) {
    public static final StreamCodec<RegistryFriendlyByteBuf, DebugGameEventListenerInfo> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, DebugGameEventListenerInfo::listenerRadius, DebugGameEventListenerInfo::new
    );
}