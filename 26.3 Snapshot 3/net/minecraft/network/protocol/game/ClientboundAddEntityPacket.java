package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

public class ClientboundAddEntityPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundAddEntityPacket> STREAM_CODEC = Packet.codec(
      ClientboundAddEntityPacket::write, ClientboundAddEntityPacket::new
   );
   private final int id;
   private final UUID uuid;
   private final EntityType<?> type;
   private final double x;
   private final double y;
   private final double z;
   private final Vec3 movement;
   private final byte xRot;
   private final byte yRot;
   private final byte yHeadRot;
   private final int data;

   public ClientboundAddEntityPacket(final Entity entity, final ServerEntity serverEntity) {
      this(entity, serverEntity, 0);
   }

   public ClientboundAddEntityPacket(final Entity entity, final ServerEntity serverEntity, final int data) {
      this(
         entity.getId(),
         entity.getUUID(),
         serverEntity.getPositionBase().x(),
         serverEntity.getPositionBase().y(),
         serverEntity.getPositionBase().z(),
         serverEntity.getLastSentXRot(),
         serverEntity.getLastSentYRot(),
         entity.getType(),
         data,
         serverEntity.getLastSentMovement(),
         serverEntity.getLastSentYHeadRot()
      );
   }

   public ClientboundAddEntityPacket(final Entity entity, final int data, final BlockPos pos) {
      this(
         entity.getId(),
         entity.getUUID(),
         pos.getX(),
         pos.getY(),
         pos.getZ(),
         entity.getXRot(),
         entity.getYRot(),
         entity.getType(),
         data,
         entity.getDeltaMovement(),
         entity.getYHeadRot()
      );
   }

   public ClientboundAddEntityPacket(
      final int id,
      final UUID uuid,
      final double x,
      final double y,
      final double z,
      final float xRot,
      final float yRot,
      final EntityType<?> type,
      final int data,
      final Vec3 movement,
      final double yHeadRot
   ) {
      this.id = id;
      this.uuid = uuid;
      this.x = x;
      this.y = y;
      this.z = z;
      this.movement = movement;
      this.xRot = Mth.packDegrees(xRot);
      this.yRot = Mth.packDegrees(yRot);
      this.yHeadRot = Mth.packDegrees((float)yHeadRot);
      this.type = type;
      this.data = data;
   }

   private ClientboundAddEntityPacket(final RegistryFriendlyByteBuf input) {
      this.id = input.readVarInt();
      this.uuid = input.readUUID();
      this.type = ByteBufCodecs.registry(Registries.ENTITY_TYPE).decode(input);
      this.x = input.readDouble();
      this.y = input.readDouble();
      this.z = input.readDouble();
      this.movement = Vec3.LP_STREAM_CODEC.decode(input);
      this.xRot = input.readByte();
      this.yRot = input.readByte();
      this.yHeadRot = input.readByte();
      this.data = input.readVarInt();
   }

   private void write(final RegistryFriendlyByteBuf output) {
      output.writeVarInt(this.id);
      output.writeUUID(this.uuid);
      ByteBufCodecs.registry(Registries.ENTITY_TYPE).encode(output, this.type);
      output.writeDouble(this.x);
      output.writeDouble(this.y);
      output.writeDouble(this.z);
      Vec3.LP_STREAM_CODEC.encode(output, this.movement);
      output.writeByte(this.xRot);
      output.writeByte(this.yRot);
      output.writeByte(this.yHeadRot);
      output.writeVarInt(this.data);
   }

   @Override
   public PacketType<ClientboundAddEntityPacket> type() {
      return GamePacketTypes.CLIENTBOUND_ADD_ENTITY;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleAddEntity(this);
   }

   public int getId() {
      return this.id;
   }

   public UUID getUUID() {
      return this.uuid;
   }

   public EntityType<?> getType() {
      return this.type;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public Vec3 getMovement() {
      return this.movement;
   }

   public float getXRot() {
      return Mth.unpackDegrees(this.xRot);
   }

   public float getYRot() {
      return Mth.unpackDegrees(this.yRot);
   }

   public float getYHeadRot() {
      return Mth.unpackDegrees(this.yHeadRot);
   }

   public int getData() {
      return this.data;
   }
}
