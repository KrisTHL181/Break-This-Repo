package net.minecraft.network.protocol.status;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.ProtocolInfoBuilder;
import net.minecraft.network.protocol.SimpleUnboundProtocol;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.PingPacketTypes;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;

public class StatusProtocols {
   public static final SimpleUnboundProtocol<ServerStatusPacketListener, ByteBuf> SERVERBOUND_TEMPLATE = ProtocolInfoBuilder.serverboundProtocol(
      ConnectionProtocol.STATUS,
      p_332003_ -> p_332003_.addPacket(StatusPacketTypes.SERVERBOUND_STATUS_REQUEST, ServerboundStatusRequestPacket.STREAM_CODEC)
         .addPacket(PingPacketTypes.SERVERBOUND_PING_REQUEST, ServerboundPingRequestPacket.STREAM_CODEC)
   );
   public static final ProtocolInfo<ServerStatusPacketListener> SERVERBOUND = SERVERBOUND_TEMPLATE.bind(p_341105_ -> p_341105_);
   public static final SimpleUnboundProtocol<ClientStatusPacketListener, FriendlyByteBuf> CLIENTBOUND_TEMPLATE = ProtocolInfoBuilder.clientboundProtocol(
      ConnectionProtocol.STATUS,
      p_327697_ -> p_327697_.addPacket(StatusPacketTypes.CLIENTBOUND_STATUS_RESPONSE, ClientboundStatusResponsePacket.STREAM_CODEC)
         .addPacket(PingPacketTypes.CLIENTBOUND_PONG_RESPONSE, ClientboundPongResponsePacket.STREAM_CODEC)
   );
   public static final ProtocolInfo<ClientStatusPacketListener> CLIENTBOUND = CLIENTBOUND_TEMPLATE.bind(FriendlyByteBuf::new);
}
