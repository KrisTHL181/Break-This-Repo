package net.minecraft.network.protocol.game;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ClientboundPlayerLookAtPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundPlayerLookAtPacket> STREAM_CODEC = Packet.codec(
      ClientboundPlayerLookAtPacket::write, ClientboundPlayerLookAtPacket::new
   );
   private final double x;
   private final double y;
   private final double z;
   private final int entity;
   private final EntityAnchorArgument.Anchor fromAnchor;
   private final EntityAnchorArgument.Anchor toAnchor;
   private final boolean atEntity;

   public ClientboundPlayerLookAtPacket(final EntityAnchorArgument.Anchor fromAnchor, final double x, final double y, final double z) {
      this.fromAnchor = fromAnchor;
      this.x = x;
      this.y = y;
      this.z = z;
      this.entity = 0;
      this.atEntity = false;
      this.toAnchor = null;
   }

   public ClientboundPlayerLookAtPacket(final EntityAnchorArgument.Anchor fromAnchor, final Entity entity, final EntityAnchorArgument.Anchor toAnchor) {
      this.fromAnchor = fromAnchor;
      this.entity = entity.getId();
      this.toAnchor = toAnchor;
      Vec3 pos = toAnchor.apply(entity);
      this.x = pos.x;
      this.y = pos.y;
      this.z = pos.z;
      this.atEntity = true;
   }

   private ClientboundPlayerLookAtPacket(final FriendlyByteBuf input) {
      this.fromAnchor = input.readEnum(EntityAnchorArgument.Anchor.class);
      this.x = input.readDouble();
      this.y = input.readDouble();
      this.z = input.readDouble();
      this.atEntity = input.readBoolean();
      if (this.atEntity) {
         this.entity = input.readVarInt();
         this.toAnchor = input.readEnum(EntityAnchorArgument.Anchor.class);
      } else {
         this.entity = 0;
         this.toAnchor = null;
      }
   }

   private void write(final FriendlyByteBuf output) {
      output.writeEnum(this.fromAnchor);
      output.writeDouble(this.x);
      output.writeDouble(this.y);
      output.writeDouble(this.z);
      output.writeBoolean(this.atEntity);
      if (this.atEntity) {
         output.writeVarInt(this.entity);
         output.writeEnum(this.toAnchor);
      }
   }

   @Override
   public PacketType<ClientboundPlayerLookAtPacket> type() {
      return GamePacketTypes.CLIENTBOUND_PLAYER_LOOK_AT;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleLookAt(this);
   }

   public EntityAnchorArgument.Anchor getFromAnchor() {
      return this.fromAnchor;
   }

   public @Nullable Vec3 getPosition(final Level level) {
      if (this.atEntity) {
         Entity entity = level.getEntity(this.entity);
         return entity == null ? new Vec3(this.x, this.y, this.z) : this.toAnchor.apply(entity);
      } else {
         return new Vec3(this.x, this.y, this.z);
      }
   }
}
