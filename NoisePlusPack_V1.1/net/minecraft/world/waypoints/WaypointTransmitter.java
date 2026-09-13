package net.minecraft.world.waypoints;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public interface WaypointTransmitter extends Waypoint {
   int REALLY_FAR_DISTANCE = 332;

   boolean isTransmittingWaypoint();

   Optional<WaypointTransmitter.Connection> makeWaypointConnectionWith(ServerPlayer var1);

   Waypoint.Icon waypointIcon();

   static boolean doesSourceIgnoreReceiver(LivingEntity p_408236_, ServerPlayer p_409708_) {
      if (p_409708_.isSpectator()) {
         return false;
      } else if (!p_408236_.isSpectator() && !p_408236_.hasIndirectPassenger(p_409708_)) {
         double d0 = Math.min(p_408236_.getAttributeValue(Attributes.WAYPOINT_TRANSMIT_RANGE), p_409708_.getAttributeValue(Attributes.WAYPOINT_RECEIVE_RANGE));
         return p_408236_.distanceTo(p_409708_) >= d0;
      } else {
         return true;
      }
   }

   static boolean isChunkVisible(ChunkPos p_406457_, ServerPlayer p_410685_) {
      return p_410685_.getChunkTrackingView().isInViewDistance(p_406457_.x, p_406457_.z);
   }

   static boolean isReallyFar(LivingEntity p_409321_, ServerPlayer p_410655_) {
      return p_409321_.distanceTo(p_410655_) > 332.0F;
   }

   interface BlockConnection extends WaypointTransmitter.Connection {
      int distanceManhattan();

      @Override
      default boolean isBroken() {
         return this.distanceManhattan() > 1;
      }
   }

   interface ChunkConnection extends WaypointTransmitter.Connection {
      int distanceChessboard();

      @Override
      default boolean isBroken() {
         return this.distanceChessboard() > 1;
      }
   }

   interface Connection {
      void connect();

      void disconnect();

      void update();

      boolean isBroken();
   }

   class EntityAzimuthConnection implements WaypointTransmitter.Connection {
      private final LivingEntity source;
      private final Waypoint.Icon icon;
      private final ServerPlayer receiver;
      private float lastAngle;

      public EntityAzimuthConnection(LivingEntity p_409514_, Waypoint.Icon p_406306_, ServerPlayer p_406643_) {
         this.source = p_409514_;
         this.icon = p_406306_;
         this.receiver = p_406643_;
         Vec3 vec3 = p_406643_.position().subtract(p_409514_.position()).rotateClockwise90();
         this.lastAngle = (float)Mth.atan2(vec3.z(), vec3.x());
      }

      @Override
      public boolean isBroken() {
         return WaypointTransmitter.doesSourceIgnoreReceiver(this.source, this.receiver)
            || WaypointTransmitter.isChunkVisible(this.source.chunkPosition(), this.receiver)
            || !WaypointTransmitter.isReallyFar(this.source, this.receiver);
      }

      @Override
      public void connect() {
         this.receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointAzimuth(this.source.getUUID(), this.icon, this.lastAngle));
      }

      @Override
      public void disconnect() {
         this.receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(this.source.getUUID()));
      }

      @Override
      public void update() {
         Vec3 vec3 = this.receiver.position().subtract(this.source.position()).rotateClockwise90();
         float f = (float)Mth.atan2(vec3.z(), vec3.x());
         if (Mth.abs(f - this.lastAngle) > 0.008726646F) {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.updateWaypointAzimuth(this.source.getUUID(), this.icon, f));
            this.lastAngle = f;
         }
      }
   }

   class EntityBlockConnection implements WaypointTransmitter.BlockConnection {
      private final LivingEntity source;
      private final Waypoint.Icon icon;
      private final ServerPlayer receiver;
      private BlockPos lastPosition;

      public EntityBlockConnection(LivingEntity p_406289_, Waypoint.Icon p_410491_, ServerPlayer p_407949_) {
         this.source = p_406289_;
         this.receiver = p_407949_;
         this.icon = p_410491_;
         this.lastPosition = p_406289_.blockPosition();
      }

      @Override
      public void connect() {
         this.receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointPosition(this.source.getUUID(), this.icon, this.lastPosition));
      }

      @Override
      public void disconnect() {
         this.receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(this.source.getUUID()));
      }

      @Override
      public void update() {
         BlockPos blockpos = this.source.blockPosition();
         if (blockpos.distManhattan(this.lastPosition) > 0) {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.updateWaypointPosition(this.source.getUUID(), this.icon, blockpos));
            this.lastPosition = blockpos;
         }
      }

      @Override
      public int distanceManhattan() {
         return this.lastPosition.distManhattan(this.source.blockPosition());
      }

      @Override
      public boolean isBroken() {
         return WaypointTransmitter.BlockConnection.super.isBroken() || WaypointTransmitter.doesSourceIgnoreReceiver(this.source, this.receiver);
      }
   }

   class EntityChunkConnection implements WaypointTransmitter.ChunkConnection {
      private final LivingEntity source;
      private final Waypoint.Icon icon;
      private final ServerPlayer receiver;
      private ChunkPos lastPosition;

      public EntityChunkConnection(LivingEntity p_409563_, Waypoint.Icon p_406581_, ServerPlayer p_410549_) {
         this.source = p_409563_;
         this.icon = p_406581_;
         this.receiver = p_410549_;
         this.lastPosition = p_409563_.chunkPosition();
      }

      @Override
      public int distanceChessboard() {
         return this.lastPosition.getChessboardDistance(this.source.chunkPosition());
      }

      @Override
      public void connect() {
         this.receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointChunk(this.source.getUUID(), this.icon, this.lastPosition));
      }

      @Override
      public void disconnect() {
         this.receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(this.source.getUUID()));
      }

      @Override
      public void update() {
         ChunkPos chunkpos = this.source.chunkPosition();
         if (chunkpos.getChessboardDistance(this.lastPosition) > 0) {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.updateWaypointChunk(this.source.getUUID(), this.icon, chunkpos));
            this.lastPosition = chunkpos;
         }
      }

      @Override
      public boolean isBroken() {
         return !WaypointTransmitter.ChunkConnection.super.isBroken() && !WaypointTransmitter.doesSourceIgnoreReceiver(this.source, this.receiver)
            ? WaypointTransmitter.isChunkVisible(this.lastPosition, this.receiver)
            : true;
      }
   }
}
