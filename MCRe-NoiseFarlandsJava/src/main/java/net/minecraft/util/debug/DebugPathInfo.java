package net.minecraft.util.debug;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.pathfinder.Path;

public record DebugPathInfo(Path path, float maxNodeDistance) {
    public static final StreamCodec<FriendlyByteBuf, DebugPathInfo> STREAM_CODEC = StreamCodec.composite(
        Path.STREAM_CODEC, DebugPathInfo::path, ByteBufCodecs.FLOAT, DebugPathInfo::maxNodeDistance, DebugPathInfo::new
    );
}