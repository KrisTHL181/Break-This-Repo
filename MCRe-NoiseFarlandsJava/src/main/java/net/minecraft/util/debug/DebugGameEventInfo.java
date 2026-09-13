package net.minecraft.util.debug;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public record DebugGameEventInfo(Holder<GameEvent> event, Vec3 pos) {
    public static final StreamCodec<RegistryFriendlyByteBuf, DebugGameEventInfo> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.holderRegistry(Registries.GAME_EVENT), DebugGameEventInfo::event, Vec3.STREAM_CODEC, DebugGameEventInfo::pos, DebugGameEventInfo::new
    );
}