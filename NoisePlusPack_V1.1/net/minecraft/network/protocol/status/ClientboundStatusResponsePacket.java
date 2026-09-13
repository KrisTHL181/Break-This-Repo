package net.minecraft.network.protocol.status;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.RegistryOps;

public record ClientboundStatusResponsePacket(ServerStatus status) implements Packet<ClientStatusPacketListener> {
   private static final RegistryOps<JsonElement> OPS = RegistryAccess.EMPTY.createSerializationContext(JsonOps.INSTANCE);
   public static final StreamCodec<ByteBuf, ClientboundStatusResponsePacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.lenientJson(32767).apply(ByteBufCodecs.fromCodec(OPS, ServerStatus.CODEC)),
      ClientboundStatusResponsePacket::status,
      ClientboundStatusResponsePacket::new
   );

   @Override
   public PacketType<ClientboundStatusResponsePacket> type() {
      return StatusPacketTypes.CLIENTBOUND_STATUS_RESPONSE;
   }

   public void handle(ClientStatusPacketListener p_134896_) {
      p_134896_.handleStatusResponse(this);
   }
}
