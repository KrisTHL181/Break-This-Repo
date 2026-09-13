package net.minecraft.network.protocol.common;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundResourcePackPacket(UUID id, ServerboundResourcePackPacket.Action action) implements Packet<ServerCommonPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundResourcePackPacket> STREAM_CODEC = Packet.codec(
      ServerboundResourcePackPacket::write, ServerboundResourcePackPacket::new
   );

   private ServerboundResourcePackPacket(FriendlyByteBuf p_299426_) {
      this(p_299426_.readUUID(), p_299426_.readEnum(ServerboundResourcePackPacket.Action.class));
   }

   private void write(FriendlyByteBuf p_298279_) {
      p_298279_.writeUUID(this.id);
      p_298279_.writeEnum(this.action);
   }

   @Override
   public PacketType<ServerboundResourcePackPacket> type() {
      return CommonPacketTypes.SERVERBOUND_RESOURCE_PACK;
   }

   public void handle(ServerCommonPacketListener p_298138_) {
      p_298138_.handleResourcePackResponse(this);
   }

   public enum Action {
      SUCCESSFULLY_LOADED,
      DECLINED,
      FAILED_DOWNLOAD,
      ACCEPTED,
      DOWNLOADED,
      INVALID_URL,
      FAILED_RELOAD,
      DISCARDED;

      public boolean isTerminal() {
         return this != ACCEPTED && this != DOWNLOADED;
      }
   }
}
