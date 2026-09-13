package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.TrackedWaypointManager;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointManager;

public record ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation operation, TrackedWaypoint waypoint)
   implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundTrackedWaypointPacket> STREAM_CODEC = StreamCodec.composite(
      ClientboundTrackedWaypointPacket.Operation.STREAM_CODEC,
      ClientboundTrackedWaypointPacket::operation,
      TrackedWaypoint.STREAM_CODEC,
      ClientboundTrackedWaypointPacket::waypoint,
      ClientboundTrackedWaypointPacket::new
   );

   public static ClientboundTrackedWaypointPacket removeWaypoint(UUID p_407468_) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.UNTRACK, TrackedWaypoint.empty(p_407468_));
   }

   public static ClientboundTrackedWaypointPacket addWaypointPosition(UUID p_409639_, Waypoint.Icon p_406602_, Vec3i p_406647_) {
      return new ClientboundTrackedWaypointPacket(
         ClientboundTrackedWaypointPacket.Operation.TRACK, TrackedWaypoint.setPosition(p_409639_, p_406602_, p_406647_)
      );
   }

   public static ClientboundTrackedWaypointPacket updateWaypointPosition(UUID p_406779_, Waypoint.Icon p_409017_, Vec3i p_410381_) {
      return new ClientboundTrackedWaypointPacket(
         ClientboundTrackedWaypointPacket.Operation.UPDATE, TrackedWaypoint.setPosition(p_406779_, p_409017_, p_410381_)
      );
   }

   public static ClientboundTrackedWaypointPacket addWaypointChunk(UUID p_406980_, Waypoint.Icon p_409670_, ChunkPos p_409757_) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.TRACK, TrackedWaypoint.setChunk(p_406980_, p_409670_, p_409757_));
   }

   public static ClientboundTrackedWaypointPacket updateWaypointChunk(UUID p_410486_, Waypoint.Icon p_406151_, ChunkPos p_406833_) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.UPDATE, TrackedWaypoint.setChunk(p_410486_, p_406151_, p_406833_));
   }

   public static ClientboundTrackedWaypointPacket addWaypointAzimuth(UUID p_410634_, Waypoint.Icon p_405845_, float p_409276_) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.TRACK, TrackedWaypoint.setAzimuth(p_410634_, p_405845_, p_409276_));
   }

   public static ClientboundTrackedWaypointPacket updateWaypointAzimuth(UUID p_410533_, Waypoint.Icon p_406758_, float p_408384_) {
      return new ClientboundTrackedWaypointPacket(
         ClientboundTrackedWaypointPacket.Operation.UPDATE, TrackedWaypoint.setAzimuth(p_410533_, p_406758_, p_408384_)
      );
   }

   @Override
   public PacketType<ClientboundTrackedWaypointPacket> type() {
      return GamePacketTypes.CLIENTBOUND_WAYPOINT;
   }

   public void handle(ClientGamePacketListener p_406728_) {
      p_406728_.handleWaypoint(this);
   }

   public void apply(TrackedWaypointManager p_406383_) {
      this.operation.action.accept(p_406383_, this.waypoint);
   }

   enum Operation {
      TRACK(WaypointManager::trackWaypoint),
      UNTRACK(WaypointManager::untrackWaypoint),
      UPDATE(WaypointManager::updateWaypoint);

      final BiConsumer<TrackedWaypointManager, TrackedWaypoint> action;
      public static final IntFunction<ClientboundTrackedWaypointPacket.Operation> BY_ID = ByIdMap.continuous(
         Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP
      );
      public static final StreamCodec<ByteBuf, ClientboundTrackedWaypointPacket.Operation> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

      Operation(final BiConsumer<TrackedWaypointManager, TrackedWaypoint> p_408699_) {
         this.action = p_408699_;
      }
   }
}
