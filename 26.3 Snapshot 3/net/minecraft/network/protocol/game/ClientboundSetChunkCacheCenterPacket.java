package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundSetChunkCacheCenterPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetChunkCacheCenterPacket> STREAM_CODEC = Packet.codec(
      ClientboundSetChunkCacheCenterPacket::write, ClientboundSetChunkCacheCenterPacket::new
   );
   private final int x;
   private final int z;

   public ClientboundSetChunkCacheCenterPacket(final int x, final int z) {
      this.x = x;
      this.z = z;
   }

   private ClientboundSetChunkCacheCenterPacket(final FriendlyByteBuf input) {
      this.x = input.readVarInt();
      this.z = input.readVarInt();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.x);
      output.writeVarInt(this.z);
   }

   @Override
   public PacketType<ClientboundSetChunkCacheCenterPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_CHUNK_CACHE_CENTER;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleSetChunkCacheCenter(this);
   }

   public int getX() {
      return this.x;
   }

   public int getZ() {
      return this.z;
   }
}
