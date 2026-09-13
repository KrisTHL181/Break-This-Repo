package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class ClientboundSetEntityLinkPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetEntityLinkPacket> STREAM_CODEC = Packet.codec(
      ClientboundSetEntityLinkPacket::write, ClientboundSetEntityLinkPacket::new
   );
   private final int sourceId;
   private final int destId;

   public ClientboundSetEntityLinkPacket(final Entity sourceEntity, final @Nullable Entity destEntity) {
      this.sourceId = sourceEntity.getId();
      this.destId = destEntity != null ? destEntity.getId() : 0;
   }

   private ClientboundSetEntityLinkPacket(final FriendlyByteBuf input) {
      this.sourceId = input.readInt();
      this.destId = input.readInt();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeInt(this.sourceId);
      output.writeInt(this.destId);
   }

   @Override
   public PacketType<ClientboundSetEntityLinkPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_ENTITY_LINK;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleEntityLinkPacket(this);
   }

   public int getSourceId() {
      return this.sourceId;
   }

   public int getDestId() {
      return this.destId;
   }
}
