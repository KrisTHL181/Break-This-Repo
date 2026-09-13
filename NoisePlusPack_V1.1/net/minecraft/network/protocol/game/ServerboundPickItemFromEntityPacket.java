package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundPickItemFromEntityPacket(int id, boolean includeData) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<ByteBuf, ServerboundPickItemFromEntityPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ServerboundPickItemFromEntityPacket::id,
      ByteBufCodecs.BOOL,
      ServerboundPickItemFromEntityPacket::includeData,
      ServerboundPickItemFromEntityPacket::new
   );

   @Override
   public PacketType<ServerboundPickItemFromEntityPacket> type() {
      return GamePacketTypes.SERVERBOUND_PICK_ITEM_FROM_ENTITY;
   }

   public void handle(ServerGamePacketListener p_375533_) {
      p_375533_.handlePickItemFromEntity(this);
   }
}
