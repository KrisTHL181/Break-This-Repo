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

   public ClientboundEntityEventPacket(Entity p_132092_, byte p_132093_) {
      this.entityId = p_132092_.getId();
      this.eventId = p_132093_;
   }

   private ClientboundEntityEventPacket(FriendlyByteBuf p_178843_) {
      this.entityId = p_178843_.readInt();
      this.eventId = p_178843_.readByte();
   }

   private void write(FriendlyByteBuf p_132104_) {
      p_132104_.writeInt(this.entityId);
      p_132104_.writeByte(this.eventId);
   }

   @Override
   public PacketType<ClientboundEntityEventPacket> type() {
      return GamePacketTypes.CLIENTBOUND_ENTITY_EVENT;
   }

   public void handle(ClientGamePacketListener p_132101_) {
      p_132101_.handleEntityEvent(this);
   }

   public @Nullable Entity getEntity(Level p_132095_) {
      return p_132095_.getEntity(this.entityId);
   }

   public byte getEventId() {
      return this.eventId;
   }
}
