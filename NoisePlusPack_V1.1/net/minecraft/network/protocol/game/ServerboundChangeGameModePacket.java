package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.GameType;

public record ServerboundChangeGameModePacket(GameType mode) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<ByteBuf, ServerboundChangeGameModePacket> STREAM_CODEC = StreamCodec.composite(
      GameType.STREAM_CODEC, ServerboundChangeGameModePacket::mode, ServerboundChangeGameModePacket::new
   );

   @Override
   public PacketType<ServerboundChangeGameModePacket> type() {
      return GamePacketTypes.SERVERBOUND_CHANGE_GAME_MODE;
   }

   public void handle(ServerGamePacketListener p_406852_) {
      p_406852_.handleChangeGameMode(this);
   }
}
