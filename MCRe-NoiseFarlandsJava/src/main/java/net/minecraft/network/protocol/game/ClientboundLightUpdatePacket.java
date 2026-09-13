package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

public class ClientboundLightUpdatePacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundLightUpdatePacket> STREAM_CODEC = Packet.codec(
        ClientboundLightUpdatePacket::write, ClientboundLightUpdatePacket::new
    );
    private final int x;
    private final int z;
    private final ClientboundLightUpdatePacketData lightData;

    // 🔧 MCRe P4b：光照增量改绝对 sectionY 集合（LongOpenHashSet），全量模式传 null + 窗口锚定
    public ClientboundLightUpdatePacket(
        final ChunkPos pos,
        final LevelLightEngine lightEngine,
        final @Nullable LongOpenHashSet skyChangedLightSections,
        final @Nullable LongOpenHashSet blockChangedLightSections,
        final int windowMinSection,
        final int windowMaxSection
    ) {
        this.x = (int)pos.x();
        this.z = (int)pos.z();
        this.lightData = new ClientboundLightUpdatePacketData(
            pos, lightEngine, skyChangedLightSections, blockChangedLightSections, windowMinSection, windowMaxSection
        );
    }

    private ClientboundLightUpdatePacket(final FriendlyByteBuf input) {
        this.x = input.readVarInt();
        this.z = input.readVarInt();
        this.lightData = new ClientboundLightUpdatePacketData(input);
    }

    private void write(final FriendlyByteBuf output) {
        output.writeVarInt(this.x);
        output.writeVarInt(this.z);
        this.lightData.write(output);
    }

    @Override
    public PacketType<ClientboundLightUpdatePacket> type() {
        return GamePacketTypes.CLIENTBOUND_LIGHT_UPDATE;
    }

    public void handle(final ClientGamePacketListener listener) {
        listener.handleLightUpdatePacket(this);
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public ClientboundLightUpdatePacketData getLightData() {
        return this.lightData;
    }
}