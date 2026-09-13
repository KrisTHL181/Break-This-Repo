package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundPickItemFromBlockPacket(BlockPos pos, boolean includeData) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<ByteBuf, ServerboundPickItemFromBlockPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      ServerboundPickItemFromBlockPacket::pos,
      ByteBufCodecs.BOOL,
      ServerboundPickItemFromBlockPacket::includeData,
      ServerboundPickItemFromBlockPacket::new
   );

   @Override
   public PacketType<ServerboundPickItemFromBlockPacket> type() {
      return GamePacketTypes.SERVERBOUND_PICK_ITEM_FROM_BLOCK;
   }

   public void handle(ServerGamePacketListener p_377743_) {
      p_377743_.handlePickItemFromBlock(this);
   }
}
