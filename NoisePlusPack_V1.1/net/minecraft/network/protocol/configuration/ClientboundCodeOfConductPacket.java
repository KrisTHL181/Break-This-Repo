package net.minecraft.network.protocol.configuration;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundCodeOfConductPacket(String codeOfConduct) implements Packet<ClientConfigurationPacketListener> {
   public static final StreamCodec<ByteBuf, ClientboundCodeOfConductPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, ClientboundCodeOfConductPacket::codeOfConduct, ClientboundCodeOfConductPacket::new
   );

   @Override
   public PacketType<ClientboundCodeOfConductPacket> type() {
      return ConfigurationPacketTypes.CLIENTBOUND_CODE_OF_CONDUCT;
   }

   public void handle(ClientConfigurationPacketListener p_424187_) {
      p_424187_.handleCodeOfConduct(this);
   }
}
