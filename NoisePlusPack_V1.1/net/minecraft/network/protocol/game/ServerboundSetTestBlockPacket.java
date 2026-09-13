package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.block.state.properties.TestBlockMode;

public record ServerboundSetTestBlockPacket(BlockPos position, TestBlockMode mode, String message) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundSetTestBlockPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      ServerboundSetTestBlockPacket::position,
      TestBlockMode.STREAM_CODEC,
      ServerboundSetTestBlockPacket::mode,
      ByteBufCodecs.STRING_UTF8,
      ServerboundSetTestBlockPacket::message,
      ServerboundSetTestBlockPacket::new
   );

   @Override
   public PacketType<ServerboundSetTestBlockPacket> type() {
      return GamePacketTypes.SERVERBOUND_SET_TEST_BLOCK;
   }

   public void handle(ServerGamePacketListener p_394720_) {
      p_394720_.handleSetTestBlock(this);
   }
}
