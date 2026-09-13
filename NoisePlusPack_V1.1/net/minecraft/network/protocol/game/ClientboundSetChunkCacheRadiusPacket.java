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

   public ClientboundSetChunkCacheRadiusPacket(int p_133101_) {
      this.radius = p_133101_;
   }

   private ClientboundSetChunkCacheRadiusPacket(FriendlyByteBuf p_179284_) {
      this.radius = p_179284_.readVarInt();
   }

   private void write(FriendlyByteBuf p_133110_) {
      p_133110_.writeVarInt(this.radius);
   }

   @Override
   public PacketType<ClientboundSetChunkCacheRadiusPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_CHUNK_CACHE_RADIUS;
   }

   public void handle(ClientGamePacketListener p_133107_) {
      p_133107_.handleSetChunkCacheRadius(this);
   }

   public int getRadius() {
      return this.radius;
   }
}
