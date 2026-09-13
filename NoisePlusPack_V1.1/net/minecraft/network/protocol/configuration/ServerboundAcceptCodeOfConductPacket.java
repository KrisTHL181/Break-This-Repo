package net.minecraft.network.protocol.configuration;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundAcceptCodeOfConductPacket() implements Packet<ServerConfigurationPacketListener> {
   public static final ServerboundAcceptCodeOfConductPacket INSTANCE = new ServerboundAcceptCodeOfConductPacket();
   public static final StreamCodec<ByteBuf, ServerboundAcceptCodeOfConductPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   @Override
   public PacketType<ServerboundAcceptCodeOfConductPacket> type() {
      return ConfigurationPacketTypes.SERVERBOUND_ACCEPT_CODE_OF_CONDUCT;
   }

   public void handle(ServerConfigurationPacketListener p_427437_) {
      p_427437_.handleAcceptCodeOfConduct(this);
   }
}
