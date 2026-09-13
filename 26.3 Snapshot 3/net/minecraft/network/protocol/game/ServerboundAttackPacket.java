package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundAttackPacket(int entityId) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<ByteBuf, ServerboundAttackPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, ServerboundAttackPacket::entityId, ServerboundAttackPacket::new
   );

   @Override
   public PacketType<ServerboundAttackPacket> type() {
      return GamePacketTypes.SERVERBOUND_ATTACK;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleAttack(this);
   }
}
