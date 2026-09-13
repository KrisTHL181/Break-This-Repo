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

   public ClientboundAddEntityPacket(Entity p_131481_, ServerEntity p_343528_) {
      this(p_131481_, p_343528_, 0);
   }

   public ClientboundAddEntityPacket(Entity p_131483_, ServerEntity p_342757_, int p_131484_) {
      this(
         p_131483_.getId(),
         p_131483_.getUUID(),
         p_342757_.getPositionBase().x(),
         p_342757_.getPositionBase().y(),
         p_342757_.getPositionBase().z(),
         p_342757_.getLastSentXRot(),
         p_342757_.getLastSentYRot(),
         p_131483_.getType(),
         p_131484_,
         p_342757_.getLastSentMovement(),
         p_342757_.getLastSentYHeadRot()
      );
   }

   public ClientboundAddEntityPacket(Entity p_237558_, int p_237559_, BlockPos p_237560_) {
      this(
         p_237558_.getId(),
         p_237558_.getUUID(),
         p_237560_.getX(),
         p_237560_.getY(),
         p_237560_.getZ(),
         p_237558_.getXRot(),
         p_237558_.getYRot(),
         p_237558_.getType(),
         p_237559_,
         p_237558_.getDeltaMovement(),
         p_237558_.getYHeadRot()
      );
   }

   public ClientboundAddEntityPacket(
      int p_237546_,
      UUID p_237547_,
      double p_237548_,
      double p_237549_,
      double p_237550_,
      float p_237551_,
      float p_237552_,
      EntityType<?> p_237553_,
      int p_237554_,
      Vec3 p_237555_,
      double p_237556_
   ) {
      this.id = p_237546_;
      this.uuid = p_237547_;
      this.x = p_237548_;
      this.y = p_237549_;
      this.z = p_237550_;
      this.movement = p_237555_;
      this.xRot = Mth.packDegrees(p_237551_);
      this.yRot = Mth.packDegrees(p_237552_);
      this.yHeadRot = Mth.packDegrees((float)p_237556_);
      this.type = p_237553_;
      this.data = p_237554_;
   }

   private ClientboundAddEntityPacket(RegistryFriendlyByteBuf p_327810_) {
      this.id = p_327810_.readVarInt();
      this.uuid = p_327810_.readUUID();
      this.type = ByteBufCodecs.registry(Registries.ENTITY_TYPE).decode(p_327810_);
      this.x = p_327810_.readDouble();
      this.y = p_327810_.readDouble();
      this.z = p_327810_.readDouble();
      this.movement = p_327810_.readLpVec3();
      this.xRot = p_327810_.readByte();
      this.yRot = p_327810_.readByte();
      this.yHeadRot = p_327810_.readByte();
      this.data = p_327810_.readVarInt();
   }

   private void write(RegistryFriendlyByteBuf p_332393_) {
      p_332393_.writeVarInt(this.id);
      p_332393_.writeUUID(this.uuid);
      ByteBufCodecs.registry(Registries.ENTITY_TYPE).encode(p_332393_, this.type);
      p_332393_.writeDouble(this.x);
      p_332393_.writeDouble(this.y);
      p_332393_.writeDouble(this.z);
      p_332393_.writeLpVec3(this.movement);
      p_332393_.writeByte(this.xRot);
      p_332393_.writeByte(this.yRot);
      p_332393_.writeByte(this.yHeadRot);
      p_332393_.writeVarInt(this.data);
   }

   @Override
   public PacketType<ClientboundAddEntityPacket> type() {
      return GamePacketTypes.CLIENTBOUND_ADD_ENTITY;
   }

   public void handle(ClientGamePacketListener p_131495_) {
      p_131495_.handleAddEntity(this);
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
