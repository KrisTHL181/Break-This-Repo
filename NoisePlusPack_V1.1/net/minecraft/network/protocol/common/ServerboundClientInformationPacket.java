package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.server.level.ClientInformation;

public record ServerboundClientInformationPacket(ClientInformation information) implements Packet<ServerCommonPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundClientInformationPacket> STREAM_CODEC = Packet.codec(
      ServerboundClientInformationPacket::write, ServerboundClientInformationPacket::new
   );

   private ServerboundClientInformationPacket(FriendlyByteBuf p_299808_) {
      this(new ClientInformation(p_299808_));
   }

   private void write(FriendlyByteBuf p_298054_) {
      this.information.write(p_298054_);
   }

   @Override
   public PacketType<ServerboundClientInformationPacket> type() {
      return CommonPacketTypes.SERVERBOUND_CLIENT_INFORMATION;
   }

   public void handle(ServerCommonPacketListener p_300686_) {
      p_300686_.handleClientInformation(this);
   }
}
