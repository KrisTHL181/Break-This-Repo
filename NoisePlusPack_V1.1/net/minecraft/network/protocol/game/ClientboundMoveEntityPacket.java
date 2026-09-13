package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public abstract class ClientboundMoveEntityPacket implements Packet<ClientGamePacketListener> {
   protected final int entityId;
   protected final short xa;
   protected final short ya;
   protected final short za;
   protected final byte yRot;
   protected final byte xRot;
   protected final boolean onGround;
   protected final boolean hasRot;
   protected final boolean hasPos;

   protected ClientboundMoveEntityPacket(
      int p_178988_, short p_178989_, short p_178990_, short p_178991_, byte p_178992_, byte p_178993_, boolean p_178994_, boolean p_178995_, boolean p_178996_
   ) {
      this.entityId = p_178988_;
      this.xa = p_178989_;
      this.ya = p_178990_;
      this.za = p_178991_;
      this.yRot = p_178992_;
      this.xRot = p_178993_;
      this.onGround = p_178994_;
      this.hasRot = p_178995_;
      this.hasPos = p_178996_;
   }

   @Override
   public abstract PacketType<? extends ClientboundMoveEntityPacket> type();

   public void handle(ClientGamePacketListener p_132528_) {
      p_132528_.handleMoveEntity(this);
   }

   @Override
   public String toString() {
      return "Entity_" + super.toString();
   }

   public @Nullable Entity getEntity(Level p_132520_) {
      return p_132520_.getEntity(this.entityId);
   }

   public short getXa() {
      return this.xa;
   }

   public short getYa() {
      return this.ya;
   }

   public short getZa() {
      return this.za;
   }

   public float getYRot() {
      return Mth.unpackDegrees(this.yRot);
   }

   public float getXRot() {
      return Mth.unpackDegrees(this.xRot);
   }

   public boolean hasRotation() {
      return this.hasRot;
   }

   public boolean hasPosition() {
      return this.hasPos;
   }

   public boolean isOnGround() {
      return this.onGround;
   }

   public static class Pos extends ClientboundMoveEntityPacket {
      public static final StreamCodec<FriendlyByteBuf, ClientboundMoveEntityPacket.Pos> STREAM_CODEC = Packet.codec(
         ClientboundMoveEntityPacket.Pos::write, ClientboundMoveEntityPacket.Pos::read
      );

      public Pos(int p_132539_, short p_132540_, short p_132541_, short p_132542_, boolean p_132543_) {
         super(p_132539_, p_132540_, p_132541_, p_132542_, (byte)0, (byte)0, p_132543_, false, true);
      }

      private static ClientboundMoveEntityPacket.Pos read(FriendlyByteBuf p_179001_) {
         int i = p_179001_.readVarInt();
         short short1 = p_179001_.readShort();
         short short2 = p_179001_.readShort();
         short short3 = p_179001_.readShort();
         boolean flag = p_179001_.readBoolean();
         return new ClientboundMoveEntityPacket.Pos(i, short1, short2, short3, flag);
      }

      private void write(FriendlyByteBuf p_132549_) {
         p_132549_.writeVarInt(this.entityId);
         p_132549_.writeShort(this.xa);
         p_132549_.writeShort(this.ya);
         p_132549_.writeShort(this.za);
         p_132549_.writeBoolean(this.onGround);
      }

      @Override
      public PacketType<ClientboundMoveEntityPacket.Pos> type() {
         return GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_POS;
      }
   }

   public static class PosRot extends ClientboundMoveEntityPacket {
      public static final StreamCodec<FriendlyByteBuf, ClientboundMoveEntityPacket.PosRot> STREAM_CODEC = Packet.codec(
         ClientboundMoveEntityPacket.PosRot::write, ClientboundMoveEntityPacket.PosRot::read
      );

      public PosRot(int p_132552_, short p_132553_, short p_132554_, short p_132555_, byte p_132556_, byte p_132557_, boolean p_132558_) {
         super(p_132552_, p_132553_, p_132554_, p_132555_, p_132556_, p_132557_, p_132558_, true, true);
      }

      private static ClientboundMoveEntityPacket.PosRot read(FriendlyByteBuf p_179003_) {
         int i = p_179003_.readVarInt();
         short short1 = p_179003_.readShort();
         short short2 = p_179003_.readShort();
         short short3 = p_179003_.readShort();
         byte b0 = p_179003_.readByte();
         byte b1 = p_179003_.readByte();
         boolean flag = p_179003_.readBoolean();
         return new ClientboundMoveEntityPacket.PosRot(i, short1, short2, short3, b0, b1, flag);
      }

      private void write(FriendlyByteBuf p_132564_) {
         p_132564_.writeVarInt(this.entityId);
         p_132564_.writeShort(this.xa);
         p_132564_.writeShort(this.ya);
         p_132564_.writeShort(this.za);
         p_132564_.writeByte(this.yRot);
         p_132564_.writeByte(this.xRot);
         p_132564_.writeBoolean(this.onGround);
      }

      @Override
      public PacketType<ClientboundMoveEntityPacket.PosRot> type() {
         return GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_POS_ROT;
      }
   }

   public static class Rot extends ClientboundMoveEntityPacket {
      public static final StreamCodec<FriendlyByteBuf, ClientboundMoveEntityPacket.Rot> STREAM_CODEC = Packet.codec(
         ClientboundMoveEntityPacket.Rot::write, ClientboundMoveEntityPacket.Rot::read
      );

      public Rot(int p_132567_, byte p_132568_, byte p_132569_, boolean p_132570_) {
         super(p_132567_, (short)0, (short)0, (short)0, p_132568_, p_132569_, p_132570_, true, false);
      }

      private static ClientboundMoveEntityPacket.Rot read(FriendlyByteBuf p_179005_) {
         int i = p_179005_.readVarInt();
         byte b0 = p_179005_.readByte();
         byte b1 = p_179005_.readByte();
         boolean flag = p_179005_.readBoolean();
         return new ClientboundMoveEntityPacket.Rot(i, b0, b1, flag);
      }

      private void write(FriendlyByteBuf p_132576_) {
         p_132576_.writeVarInt(this.entityId);
         p_132576_.writeByte(this.yRot);
         p_132576_.writeByte(this.xRot);
         p_132576_.writeBoolean(this.onGround);
      }

      @Override
      public PacketType<ClientboundMoveEntityPacket.Rot> type() {
         return GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_ROT;
      }
   }
}
