package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.phys.Vec3;

public abstract class ServerboundMovePlayerPacket implements Packet<ServerGamePacketListener> {
   private static final int FLAG_ON_GROUND = 1;
   private static final int FLAG_HORIZONTAL_COLLISION = 2;
   protected final double x;
   protected final double y;
   protected final double z;
   protected final float yRot;
   protected final float xRot;
   protected final boolean onGround;
   protected final boolean horizontalCollision;
   protected final boolean hasPos;
   protected final boolean hasRot;

   static int packFlags(boolean p_362343_, boolean p_368893_) {
      int i = 0;
      if (p_362343_) {
         i |= 1;
      }

      if (p_368893_) {
         i |= 2;
      }

      return i;
   }

   static boolean unpackOnGround(int p_362368_) {
      return (p_362368_ & 1) != 0;
   }

   static boolean unpackHorizontalCollision(int p_366014_) {
      return (p_366014_ & 2) != 0;
   }

   protected ServerboundMovePlayerPacket(
      double p_179675_,
      double p_179676_,
      double p_179677_,
      float p_179678_,
      float p_179679_,
      boolean p_179680_,
      boolean p_179681_,
      boolean p_179682_,
      boolean p_365300_
   ) {
      this.x = p_179675_;
      this.y = p_179676_;
      this.z = p_179677_;
      this.yRot = p_179678_;
      this.xRot = p_179679_;
      this.onGround = p_179680_;
      this.horizontalCollision = p_179681_;
      this.hasPos = p_179682_;
      this.hasRot = p_365300_;
   }

   @Override
   public abstract PacketType<? extends ServerboundMovePlayerPacket> type();

   public void handle(ServerGamePacketListener p_134138_) {
      p_134138_.handleMovePlayer(this);
   }

   public double getX(double p_134130_) {
      return this.hasPos ? this.x : p_134130_;
   }

   public double getY(double p_134141_) {
      return this.hasPos ? this.y : p_134141_;
   }

   public double getZ(double p_134147_) {
      return this.hasPos ? this.z : p_134147_;
   }

   public float getYRot(float p_134132_) {
      return this.hasRot ? this.yRot : p_134132_;
   }

   public float getXRot(float p_134143_) {
      return this.hasRot ? this.xRot : p_134143_;
   }

   public boolean isOnGround() {
      return this.onGround;
   }

   public boolean horizontalCollision() {
      return this.horizontalCollision;
   }

   public boolean hasPosition() {
      return this.hasPos;
   }

   public boolean hasRotation() {
      return this.hasRot;
   }

   public static class Pos extends ServerboundMovePlayerPacket {
      public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.Pos> STREAM_CODEC = Packet.codec(
         ServerboundMovePlayerPacket.Pos::write, ServerboundMovePlayerPacket.Pos::read
      );

      public Pos(Vec3 p_393140_, boolean p_394015_, boolean p_396568_) {
         super(p_393140_.x, p_393140_.y, p_393140_.z, 0.0F, 0.0F, p_394015_, p_396568_, true, false);
      }

      public Pos(double p_134150_, double p_134151_, double p_134152_, boolean p_134153_, boolean p_362762_) {
         super(p_134150_, p_134151_, p_134152_, 0.0F, 0.0F, p_134153_, p_362762_, true, false);
      }

      private static ServerboundMovePlayerPacket.Pos read(FriendlyByteBuf p_179686_) {
         double d0 = p_179686_.readDouble();
         double d1 = p_179686_.readDouble();
         double d2 = p_179686_.readDouble();
         short short1 = p_179686_.readUnsignedByte();
         boolean flag = ServerboundMovePlayerPacket.unpackOnGround(short1);
         boolean flag1 = ServerboundMovePlayerPacket.unpackHorizontalCollision(short1);
         return new ServerboundMovePlayerPacket.Pos(d0, d1, d2, flag, flag1);
      }

      private void write(FriendlyByteBuf p_134159_) {
         p_134159_.writeDouble(this.x);
         p_134159_.writeDouble(this.y);
         p_134159_.writeDouble(this.z);
         p_134159_.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
      }

      @Override
      public PacketType<ServerboundMovePlayerPacket.Pos> type() {
         return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_POS;
      }
   }

   public static class PosRot extends ServerboundMovePlayerPacket {
      public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.PosRot> STREAM_CODEC = Packet.codec(
         ServerboundMovePlayerPacket.PosRot::write, ServerboundMovePlayerPacket.PosRot::read
      );

      public PosRot(Vec3 p_393983_, float p_394492_, float p_397302_, boolean p_396653_, boolean p_394182_) {
         super(p_393983_.x, p_393983_.y, p_393983_.z, p_394492_, p_397302_, p_396653_, p_394182_, true, true);
      }

      public PosRot(double p_134162_, double p_134163_, double p_134164_, float p_134165_, float p_134166_, boolean p_134167_, boolean p_367562_) {
         super(p_134162_, p_134163_, p_134164_, p_134165_, p_134166_, p_134167_, p_367562_, true, true);
      }

      private static ServerboundMovePlayerPacket.PosRot read(FriendlyByteBuf p_179688_) {
         double d0 = p_179688_.readDouble();
         double d1 = p_179688_.readDouble();
         double d2 = p_179688_.readDouble();
         float f = p_179688_.readFloat();
         float f1 = p_179688_.readFloat();
         short short1 = p_179688_.readUnsignedByte();
         boolean flag = ServerboundMovePlayerPacket.unpackOnGround(short1);
         boolean flag1 = ServerboundMovePlayerPacket.unpackHorizontalCollision(short1);
         return new ServerboundMovePlayerPacket.PosRot(d0, d1, d2, f, f1, flag, flag1);
      }

      private void write(FriendlyByteBuf p_134173_) {
         p_134173_.writeDouble(this.x);
         p_134173_.writeDouble(this.y);
         p_134173_.writeDouble(this.z);
         p_134173_.writeFloat(this.yRot);
         p_134173_.writeFloat(this.xRot);
         p_134173_.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
      }

      @Override
      public PacketType<ServerboundMovePlayerPacket.PosRot> type() {
         return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_POS_ROT;
      }
   }

   public static class Rot extends ServerboundMovePlayerPacket {
      public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.Rot> STREAM_CODEC = Packet.codec(
         ServerboundMovePlayerPacket.Rot::write, ServerboundMovePlayerPacket.Rot::read
      );

      public Rot(float p_134176_, float p_134177_, boolean p_134178_, boolean p_360889_) {
         super(0.0, 0.0, 0.0, p_134176_, p_134177_, p_134178_, p_360889_, false, true);
      }

      private static ServerboundMovePlayerPacket.Rot read(FriendlyByteBuf p_179690_) {
         float f = p_179690_.readFloat();
         float f1 = p_179690_.readFloat();
         short short1 = p_179690_.readUnsignedByte();
         boolean flag = ServerboundMovePlayerPacket.unpackOnGround(short1);
         boolean flag1 = ServerboundMovePlayerPacket.unpackHorizontalCollision(short1);
         return new ServerboundMovePlayerPacket.Rot(f, f1, flag, flag1);
      }

      private void write(FriendlyByteBuf p_134184_) {
         p_134184_.writeFloat(this.yRot);
         p_134184_.writeFloat(this.xRot);
         p_134184_.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
      }

      @Override
      public PacketType<ServerboundMovePlayerPacket.Rot> type() {
         return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_ROT;
      }
   }

   public static class StatusOnly extends ServerboundMovePlayerPacket {
      public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.StatusOnly> STREAM_CODEC = Packet.codec(
         ServerboundMovePlayerPacket.StatusOnly::write, ServerboundMovePlayerPacket.StatusOnly::read
      );

      public StatusOnly(boolean p_179692_, boolean p_365335_) {
         super(0.0, 0.0, 0.0, 0.0F, 0.0F, p_179692_, p_365335_, false, false);
      }

      private static ServerboundMovePlayerPacket.StatusOnly read(FriendlyByteBuf p_179698_) {
         short short1 = p_179698_.readUnsignedByte();
         boolean flag = ServerboundMovePlayerPacket.unpackOnGround(short1);
         boolean flag1 = ServerboundMovePlayerPacket.unpackHorizontalCollision(short1);
         return new ServerboundMovePlayerPacket.StatusOnly(flag, flag1);
      }

      private void write(FriendlyByteBuf p_179694_) {
         p_179694_.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
      }

      @Override
      public PacketType<ServerboundMovePlayerPacket.StatusOnly> type() {
         return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_STATUS_ONLY;
      }
   }
}
