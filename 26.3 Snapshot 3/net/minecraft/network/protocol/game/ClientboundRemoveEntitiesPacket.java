package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundRemoveEntitiesPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundRemoveEntitiesPacket> STREAM_CODEC = Packet.codec(
      ClientboundRemoveEntitiesPacket::write, ClientboundRemoveEntitiesPacket::new
   );
   private final IntList entityIds;

   public ClientboundRemoveEntitiesPacket(final IntList ids) {
      this.entityIds = new IntArrayList(ids);
   }

   public ClientboundRemoveEntitiesPacket(final int... ids) {
      this.entityIds = new IntArrayList(ids);
   }

   private ClientboundRemoveEntitiesPacket(final FriendlyByteBuf input) {
      this.entityIds = input.readIntIdList();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeIntIdList(this.entityIds);
   }

   @Override
   public PacketType<ClientboundRemoveEntitiesPacket> type() {
      return GamePacketTypes.CLIENTBOUND_REMOVE_ENTITIES;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleRemoveEntities(this);
   }

   public IntList getEntityIds() {
      return this.entityIds;
   }
}
