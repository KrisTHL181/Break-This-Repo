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

   private static int packFlags(final boolean onGround, final boolean horizontalCollision) {
      int flags = 0;
      if (onGround) {
         flags |= 1;
      }

      if (horizontalCollision) {
         flags |= 2;
      }

      return flags;
   }

   private static boolean unpackOnGround(final int flags) {
      return (flags & 1) != 0;
   }

   private static boolean unpackHorizontalCollision(final int flags) {
      return (flags & 2) != 0;
   }

   protected ServerboundMovePlayerPacket(
      final double x,
      final double y,
      final double z,
      final float yRot,
      final float xRot,
      final boolean onGround,
      final boolean horizontalCollision,
      final boolean hasPos,
      final boolean hasRot
   ) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.yRot = yRot;
      this.xRot = xRot;
      this.onGround = onGround;
      this.horizontalCollision = horizontalCollision;
      this.hasPos = hasPos;
      this.hasRot = hasRot;
   }

   @Override
   public abstract PacketType<? extends ServerboundMovePlayerPacket> type();

   public void handle(final ServerGamePacketListener listener) {
      listener.handleMovePlayer(this);
   }

   public double getX(final double fallback) {
      return this.hasPos ? this.x : fallback;
   }

   public double getY(final double fallback) {
      return this.hasPos ? this.y : fallback;
   }

   public double getZ(final double fallback) {
      return this.hasPos ? this.z : fallback;
   }

   public float getYRot(final float fallback) {
      return this.hasRot ? this.yRot : fallback;
   }

   public float getXRot(final float fallback) {
      return this.hasRot ? this.xRot : fallback;
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

      public Pos(final Vec3 pos, final boolean onGround, final boolean horizontalCollision) {
         super(pos.x, pos.y, pos.z, 0.0F, 0.0F, onGround, horizontalCollision, true, false);
      }

      public Pos(final double x, final double y, final double z, final boolean onGround, final boolean horizontalCollision) {
         super(x, y, z, 0.0F, 0.0F, onGround, horizontalCollision, true, false);
      }

      private static ServerboundMovePlayerPacket.Pos read(final FriendlyByteBuf input) {
         double x = input.readDouble();
         double y = input.readDouble();
         double z = input.readDouble();
         short flags = input.readUnsignedByte();
         boolean onGround = ServerboundMovePlayerPacket.unpackOnGround(flags);
         boolean horizontalCollision = ServerboundMovePlayerPacket.unpackHorizontalCollision(flags);
         return new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, horizontalCollision);
      }

      private void write(final FriendlyByteBuf output) {
         output.writeDouble(this.x);
         output.writeDouble(this.y);
         output.writeDouble(this.z);
         output.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
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

      public PosRot(final Vec3 pos, final float yRot, final float xRot, final boolean onGround, final boolean horizontalCollision) {
         super(pos.x, pos.y, pos.z, yRot, xRot, onGround, horizontalCollision, true, true);
      }

      public PosRot(
         final double x, final double y, final double z, final float yRot, final float xRot, final boolean onGround, final boolean horizontalCollision
      ) {
         super(x, y, z, yRot, xRot, onGround, horizontalCollision, true, true);
      }

      private static ServerboundMovePlayerPacket.PosRot read(final FriendlyByteBuf input) {
         double x = input.readDouble();
         double y = input.readDouble();
         double z = input.readDouble();
         float yRot = input.readFloat();
         float xRot = input.readFloat();
         short flags = input.readUnsignedByte();
         boolean onGround = ServerboundMovePlayerPacket.unpackOnGround(flags);
         boolean horizontalCollision = ServerboundMovePlayerPacket.unpackHorizontalCollision(flags);
         return new ServerboundMovePlayerPacket.PosRot(x, y, z, yRot, xRot, onGround, horizontalCollision);
      }

      private void write(final FriendlyByteBuf output) {
         output.writeDouble(this.x);
         output.writeDouble(this.y);
         output.writeDouble(this.z);
         output.writeFloat(this.yRot);
         output.writeFloat(this.xRot);
         output.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
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

      public Rot(final float yRot, final float xRot, final boolean onGround, final boolean horizontalCollision) {
         super(0.0, 0.0, 0.0, yRot, xRot, onGround, horizontalCollision, false, true);
      }

      private static ServerboundMovePlayerPacket.Rot read(final FriendlyByteBuf input) {
         float yRot = input.readFloat();
         float xRot = input.readFloat();
         short flags = input.readUnsignedByte();
         boolean onGround = ServerboundMovePlayerPacket.unpackOnGround(flags);
         boolean horizontalCollision = ServerboundMovePlayerPacket.unpackHorizontalCollision(flags);
         return new ServerboundMovePlayerPacket.Rot(yRot, xRot, onGround, horizontalCollision);
      }

      private void write(final FriendlyByteBuf output) {
         output.writeFloat(this.yRot);
         output.writeFloat(this.xRot);
         output.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
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

      public StatusOnly(final boolean onGround, final boolean horizontalCollision) {
         super(0.0, 0.0, 0.0, 0.0F, 0.0F, onGround, horizontalCollision, false, false);
      }

      private static ServerboundMovePlayerPacket.StatusOnly read(final FriendlyByteBuf input) {
         short flags = input.readUnsignedByte();
         boolean onGround = ServerboundMovePlayerPacket.unpackOnGround(flags);
         boolean horizontalCollision = ServerboundMovePlayerPacket.unpackHorizontalCollision(flags);
         return new ServerboundMovePlayerPacket.StatusOnly(onGround, horizontalCollision);
      }

      private void write(final FriendlyByteBuf output) {
         output.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
      }

      @Override
      public PacketType<ServerboundMovePlayerPacket.StatusOnly> type() {
         return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_STATUS_ONLY;
      }
   }
}
