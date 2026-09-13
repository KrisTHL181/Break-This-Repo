package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public abstract class ClientboundMoveEntityPacket implements MovementPacket<ClientGamePacketListener> {
   private static final int ON_GROUND_FLAG = 1;
   private static final int STEP_COUNT_OFFSET = 1;
   protected final int entityId;
   protected final VecDelta delta;
   protected final byte yRot;
   protected final byte xRot;
   protected final boolean onGround;
   protected final boolean hasRot;
   protected final boolean hasPos;

   protected static boolean unpackOnGround(final int properties) {
      return (properties & 1) != 0;
   }

   protected static int unpackStepCount(final int properties) {
      return properties >>> 1;
   }

   protected static int packProperties(final boolean onGround, final int stepCount) {
      return (onGround ? 1 : 0) | stepCount << 1;
   }

   protected ClientboundMoveEntityPacket(
      final int entityId, final VecDelta delta, final byte yRot, final byte xRot, final boolean onGround, final boolean hasRot, final boolean hasPos
   ) {
      this.entityId = entityId;
      this.delta = delta;
      this.yRot = yRot;
      this.xRot = xRot;
      this.onGround = onGround;
      this.hasRot = hasRot;
      this.hasPos = hasPos;
   }

   @Override
   public abstract PacketType<? extends ClientboundMoveEntityPacket> type();

   public void handle(final ClientGamePacketListener listener) {
      listener.handleMoveEntity(this);
   }

   @Override
   public String toString() {
      return "Entity_" + super.toString();
   }

   public @Nullable Entity getEntity(final Level level) {
      return level.getEntity(this.entityId);
   }

   public VecDelta getPositionDelta() {
      return this.delta;
   }

   public float getYRot() {
      return Mth.unpackDegrees(this.yRot);
   }

   public float getXRot() {
      return Mth.unpackDegrees(this.xRot);
   }

   @Override
   public boolean hasRotation() {
      return this.hasRot;
   }

   @Override
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

      public Pos(final int id, final VecDelta delta, final boolean onGround) {
         super(id, delta, (byte)0, (byte)0, onGround, false, true);
      }

      private static ClientboundMoveEntityPacket.Pos read(final FriendlyByteBuf input) {
         int entityId = input.readVarInt();
         int properties = input.readVarInt();
         VecDelta delta = VecDelta.read(input, unpackStepCount(properties));
         return new ClientboundMoveEntityPacket.Pos(entityId, delta, unpackOnGround(properties));
      }

      private void write(final FriendlyByteBuf output) {
         output.writeVarInt(this.entityId);
         output.writeVarInt(packProperties(this.onGround, this.delta.stepCount()));
         VecDelta.write(output, this.delta);
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

      public PosRot(final int id, final VecDelta delta, final byte yRot, final byte xRot, final boolean onGround) {
         super(id, delta, yRot, xRot, onGround, true, true);
      }

      private static ClientboundMoveEntityPacket.PosRot read(final FriendlyByteBuf input) {
         int entityId = input.readVarInt();
         int properties = input.readVarInt();
         VecDelta delta = VecDelta.read(input, unpackStepCount(properties));
         byte yRot = input.readByte();
         byte xRot = input.readByte();
         return new ClientboundMoveEntityPacket.PosRot(entityId, delta, yRot, xRot, unpackOnGround(properties));
      }

      private void write(final FriendlyByteBuf output) {
         output.writeVarInt(this.entityId);
         output.writeVarInt(packProperties(this.onGround, this.delta.stepCount()));
         VecDelta.write(output, this.delta);
         output.writeByte(this.yRot);
         output.writeByte(this.xRot);
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

      public Rot(final int id, final byte yRot, final byte xRot, final boolean onGround) {
         super(id, VecDelta.ZERO, yRot, xRot, onGround, true, false);
      }

      private static ClientboundMoveEntityPacket.Rot read(final FriendlyByteBuf input) {
         int entityId = input.readVarInt();
         boolean onGround = input.readBoolean();
         byte yRot = input.readByte();
         byte xRot = input.readByte();
         return new ClientboundMoveEntityPacket.Rot(entityId, yRot, xRot, onGround);
      }

      private void write(final FriendlyByteBuf output) {
         output.writeVarInt(this.entityId);
         output.writeBoolean(this.onGround);
         output.writeByte(this.yRot);
         output.writeByte(this.xRot);
      }

      @Override
      public PacketType<ClientboundMoveEntityPacket.Rot> type() {
         return GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_ROT;
      }
   }
}
