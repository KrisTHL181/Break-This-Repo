package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ClientboundEntityEventPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundEntityEventPacket> STREAM_CODEC = Packet.codec(
      ClientboundEntityEventPacket::write, ClientboundEntityEventPacket::new
   );
   private final int entityId;
   private final byte eventId;

   public ClientboundEntityEventPacket(final Entity entity, final byte eventId) {
      this.entityId = entity.getId();
      this.eventId = eventId;
   }

   private ClientboundEntityEventPacket(final FriendlyByteBuf input) {
      this.entityId = input.readInt();
      this.eventId = input.readByte();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeInt(this.entityId);
      output.writeByte(this.eventId);
   }

   @Override
   public PacketType<ClientboundEntityEventPacket> type() {
      return GamePacketTypes.CLIENTBOUND_ENTITY_EVENT;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleEntityEvent(this);
   }

   public @Nullable Entity getEntity(final Level level) {
      return level.getEntity(this.entityId);
   }

   public byte getEventId() {
      return this.eventId;
   }
}
