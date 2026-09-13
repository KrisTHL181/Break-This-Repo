package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundSetChunkCacheRadiusPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetChunkCacheRadiusPacket> STREAM_CODEC = Packet.codec(
      ClientboundSetChunkCacheRadiusPacket::write, ClientboundSetChunkCacheRadiusPacket::new
   );
   private final int radius;

   public ClientboundSetChunkCacheRadiusPacket(final int radius) {
      this.radius = radius;
   }

   private ClientboundSetChunkCacheRadiusPacket(final FriendlyByteBuf input) {
      this.radius = input.readVarInt();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.radius);
   }

   @Override
   public PacketType<ClientboundSetChunkCacheRadiusPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_CHUNK_CACHE_RADIUS;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleSetChunkCacheRadius(this);
   }

   public int getRadius() {
      return this.radius;
   }
}
