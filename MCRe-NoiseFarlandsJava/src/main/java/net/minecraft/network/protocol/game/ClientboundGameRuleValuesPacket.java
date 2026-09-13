package net.minecraft.network.protocol.game;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.gamerules.GameRule;

public record ClientboundGameRuleValuesPacket(Map<ResourceKey<GameRule<?>>, String> values) implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundGameRuleValuesPacket> STREAM_CODEC = ByteBufCodecs.<FriendlyByteBuf, ResourceKey<GameRule<?>>, String, Map<ResourceKey<GameRule<?>>, String>>map(
            HashMap::new, ResourceKey.streamCodec(Registries.GAME_RULE), ByteBufCodecs.STRING_UTF8
        )
        .map(ClientboundGameRuleValuesPacket::new, ClientboundGameRuleValuesPacket::values);

    @Override
    public PacketType<ClientboundGameRuleValuesPacket> type() {
        return GamePacketTypes.CLIENTBOUND_GAME_RULE_VALUES;
    }

    public void handle(final ClientGamePacketListener listener) {
        listener.handleGameRuleValues(this);
    }
}
