package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.Difficulty;

public record ServerboundChangeDifficultyPacket(Difficulty difficulty) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<ByteBuf, ServerboundChangeDifficultyPacket> STREAM_CODEC = StreamCodec.composite(
      Difficulty.STREAM_CODEC, ServerboundChangeDifficultyPacket::difficulty, ServerboundChangeDifficultyPacket::new
   );

   @Override
   public PacketType<ServerboundChangeDifficultyPacket> type() {
      return GamePacketTypes.SERVERBOUND_CHANGE_DIFFICULTY;
   }

   public void handle(ServerGamePacketListener p_133823_) {
      p_133823_.handleChangeDifficulty(this);
   }
}
