package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.ChunkPos;

public record ClientboundForgetLevelChunkPacket(ChunkPos pos) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundForgetLevelChunkPacket> STREAM_CODEC = Packet.codec(
      ClientboundForgetLevelChunkPacket::write, ClientboundForgetLevelChunkPacket::new
   );

   private ClientboundForgetLevelChunkPacket(final FriendlyByteBuf input) {
      this(input.readChunkPos());
   }

   private void write(final FriendlyByteBuf output) {
      output.writeChunkPos(this.pos);
   }

   @Override
   public PacketType<ClientboundForgetLevelChunkPacket> type() {
      return GamePacketTypes.CLIENTBOUND_FORGET_LEVEL_CHUNK;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleForgetLevelChunk(this);
   }
}
