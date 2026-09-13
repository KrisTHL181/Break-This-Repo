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

   public static ClientboundTrackedWaypointPacket removeWaypoint(final UUID identifier) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.UNTRACK, TrackedWaypoint.empty(identifier));
   }

   public static ClientboundTrackedWaypointPacket addWaypointPosition(final UUID identifier, final Waypoint.Icon icon, final Vec3i position) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.TRACK, TrackedWaypoint.setPosition(identifier, icon, position));
   }

   public static ClientboundTrackedWaypointPacket updateWaypointPosition(final UUID identifier, final Waypoint.Icon icon, final Vec3i position) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.UPDATE, TrackedWaypoint.setPosition(identifier, icon, position));
   }

   public static ClientboundTrackedWaypointPacket addWaypointChunk(final UUID identifier, final Waypoint.Icon icon, final ChunkPos chunk) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.TRACK, TrackedWaypoint.setChunk(identifier, icon, chunk));
   }

   public static ClientboundTrackedWaypointPacket updateWaypointChunk(final UUID identifier, final Waypoint.Icon icon, final ChunkPos chunk) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.UPDATE, TrackedWaypoint.setChunk(identifier, icon, chunk));
   }

   public static ClientboundTrackedWaypointPacket addWaypointAzimuth(final UUID identifier, final Waypoint.Icon icon, final float angle) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.TRACK, TrackedWaypoint.setAzimuth(identifier, icon, angle));
   }

   public static ClientboundTrackedWaypointPacket updateWaypointAzimuth(final UUID identifier, final Waypoint.Icon icon, final float angle) {
      return new ClientboundTrackedWaypointPacket(ClientboundTrackedWaypointPacket.Operation.UPDATE, TrackedWaypoint.setAzimuth(identifier, icon, angle));
   }

   @Override
   public PacketType<ClientboundTrackedWaypointPacket> type() {
      return GamePacketTypes.CLIENTBOUND_WAYPOINT;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleWaypoint(this);
   }

   public void apply(final TrackedWaypointManager manager) {
      this.operation.action.accept(manager, this.waypoint);
   }

   private enum Operation {
      TRACK(WaypointManager::trackWaypoint),
      UNTRACK(WaypointManager::untrackWaypoint),
      UPDATE(WaypointManager::updateWaypoint);

      private final BiConsumer<TrackedWaypointManager, TrackedWaypoint> action;
      public static final IntFunction<ClientboundTrackedWaypointPacket.Operation> BY_ID = ByIdMap.continuous(
         Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP
      );
      public static final StreamCodec<ByteBuf, ClientboundTrackedWaypointPacket.Operation> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

      Operation(final BiConsumer<TrackedWaypointManager, TrackedWaypoint> action) {
         this.action = action;
      }
   }
}
