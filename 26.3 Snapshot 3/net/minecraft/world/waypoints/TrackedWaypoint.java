package net.minecraft.world.waypoints;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;

public abstract class TrackedWaypoint implements Waypoint {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final StreamCodec<ByteBuf, TrackedWaypoint> STREAM_CODEC = StreamCodec.ofMember(TrackedWaypoint::write, TrackedWaypoint::read);
   protected final Either<UUID, String> identifier;
   private final Waypoint.Icon icon;
   private final TrackedWaypoint.Type type;

   private TrackedWaypoint(final Either<UUID, String> identifier, final Waypoint.Icon icon, final TrackedWaypoint.Type type) {
      this.identifier = identifier;
      this.icon = icon;
      this.type = type;
   }

   public Either<UUID, String> id() {
      return this.identifier;
   }

   public abstract void update(final TrackedWaypoint other);

   public void write(final ByteBuf buf) {
      FriendlyByteBuf byteBuf = new FriendlyByteBuf(buf);
      byteBuf.writeEither(this.identifier, UUIDUtil.STREAM_CODEC, FriendlyByteBuf::writeUtf);
      Waypoint.Icon.STREAM_CODEC.encode(byteBuf, this.icon);
      byteBuf.writeEnum(this.type);
      this.writeContents(buf);
   }

   public abstract void writeContents(final ByteBuf buf);

   private static TrackedWaypoint read(final ByteBuf buf) {
      FriendlyByteBuf byteBuf = new FriendlyByteBuf(buf);
      Either<UUID, String> identifier = byteBuf.readEither(UUIDUtil.STREAM_CODEC, FriendlyByteBuf::readUtf);
      Waypoint.Icon icon = Waypoint.Icon.STREAM_CODEC.decode(byteBuf);
      TrackedWaypoint.Type type = byteBuf.readEnum(TrackedWaypoint.Type.class);
      return (TrackedWaypoint)type.constructor.apply(identifier, icon, byteBuf);
   }

   public static TrackedWaypoint setPosition(final UUID identifier, final Waypoint.Icon icon, final Vec3i position) {
      return new TrackedWaypoint.Vec3iWaypoint(identifier, icon, position);
   }

   public static TrackedWaypoint setChunk(final UUID identifier, final Waypoint.Icon icon, final ChunkPos chunk) {
      return new TrackedWaypoint.ChunkWaypoint(identifier, icon, chunk);
   }

   public static TrackedWaypoint setAzimuth(final UUID identifier, final Waypoint.Icon icon, final float angle) {
      return new TrackedWaypoint.AzimuthWaypoint(identifier, icon, angle);
   }

   public static TrackedWaypoint empty(final UUID identifier) {
      return new TrackedWaypoint.EmptyWaypoint(identifier);
   }

   public abstract double yawAngleToCamera(final Level level, final TrackedWaypoint.Camera camera, final PartialTickSupplier partialTickSupplier);

   public abstract TrackedWaypoint.PitchDirection pitchDirectionToCamera(
      Level level, TrackedWaypoint.Projector projector, final PartialTickSupplier partialTickSupplier
   );

   public abstract double distanceSquared(final Entity fromEntity);

   public Waypoint.Icon icon() {
      return this.icon;
   }

   private static class AzimuthWaypoint extends TrackedWaypoint {
      private float angle;

      public AzimuthWaypoint(final UUID identifier, final Waypoint.Icon icon, final float angle) {
         super(Either.left(identifier), icon, TrackedWaypoint.Type.AZIMUTH);
         this.angle = angle;
      }

      public AzimuthWaypoint(final Either<UUID, String> identifier, final Waypoint.Icon icon, final FriendlyByteBuf byteBuf) {
         super(identifier, icon, TrackedWaypoint.Type.AZIMUTH);
         this.angle = byteBuf.readFloat();
      }

      @Override
      public void update(final TrackedWaypoint other) {
         if (other instanceof TrackedWaypoint.AzimuthWaypoint azimuthWaypoint) {
            this.angle = azimuthWaypoint.angle;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", other.getClass());
         }
      }

      @Override
      public void writeContents(final ByteBuf buf) {
         buf.writeFloat(this.angle);
      }

      @Override
      public double yawAngleToCamera(final Level level, final TrackedWaypoint.Camera camera, final PartialTickSupplier partialTickSupplier) {
         return Mth.degreesDifference(camera.yaw(), this.angle * (180.0F / (float)Math.PI));
      }

      @Override
      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(
         final Level level, final TrackedWaypoint.Projector projector, final PartialTickSupplier partialTickSupplier
      ) {
         double horizon = projector.projectHorizonToScreen();
         if (horizon < -1.0) {
            return TrackedWaypoint.PitchDirection.DOWN;
         } else {
            return horizon > 1.0 ? TrackedWaypoint.PitchDirection.UP : TrackedWaypoint.PitchDirection.NONE;
         }
      }

      @Override
      public double distanceSquared(final Entity fromEntity) {
         return Double.POSITIVE_INFINITY;
      }
   }

   public interface Camera {
      float yaw();

      Vec3 position();
   }

   private static class ChunkWaypoint extends TrackedWaypoint {
      private ChunkPos chunkPos;

      public ChunkWaypoint(final UUID identifier, final Waypoint.Icon icon, final ChunkPos chunkPos) {
         super(Either.left(identifier), icon, TrackedWaypoint.Type.CHUNK);
         this.chunkPos = chunkPos;
      }

      public ChunkWaypoint(final Either<UUID, String> identifier, final Waypoint.Icon icon, final FriendlyByteBuf byteBuf) {
         super(identifier, icon, TrackedWaypoint.Type.CHUNK);
         this.chunkPos = new ChunkPos(byteBuf.readVarInt(), byteBuf.readVarInt());
      }

      @Override
      public void update(final TrackedWaypoint other) {
         if (other instanceof TrackedWaypoint.ChunkWaypoint chunkWaypoint) {
            this.chunkPos = chunkWaypoint.chunkPos;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", other.getClass());
         }
      }

      @Override
      public void writeContents(final ByteBuf buf) {
         VarInt.write(buf, this.chunkPos.x());
         VarInt.write(buf, this.chunkPos.z());
      }

      private Vec3 position(final double positionY) {
         return Vec3.atCenterOf(this.chunkPos.getMiddleBlockPosition((int)positionY));
      }

      @Override
      public double yawAngleToCamera(final Level level, final TrackedWaypoint.Camera camera, final PartialTickSupplier partialTickSupplier) {
         Vec3 cameraPosition = camera.position();
         Vec3 direction = cameraPosition.subtract(this.position(cameraPosition.y())).rotateClockwise90();
         float waypointAngle = (float)Mth.atan2(direction.z(), direction.x()) * (180.0F / (float)Math.PI);
         return Mth.degreesDifference(camera.yaw(), waypointAngle);
      }

      @Override
      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(
         final Level level, final TrackedWaypoint.Projector projector, final PartialTickSupplier partialTickSupplier
      ) {
         double onScreenHorizon = projector.projectHorizonToScreen();
         if (onScreenHorizon < -1.0) {
            return TrackedWaypoint.PitchDirection.DOWN;
         } else {
            return onScreenHorizon > 1.0 ? TrackedWaypoint.PitchDirection.UP : TrackedWaypoint.PitchDirection.NONE;
         }
      }

      @Override
      public double distanceSquared(final Entity fromEntity) {
         return fromEntity.distanceToSqr(Vec3.atCenterOf(this.chunkPos.getMiddleBlockPosition(fromEntity.getBlockY())));
      }
   }

   private static class EmptyWaypoint extends TrackedWaypoint {
      private EmptyWaypoint(final Either<UUID, String> identifier, final Waypoint.Icon icon, final FriendlyByteBuf byteBuf) {
         super(identifier, icon, TrackedWaypoint.Type.EMPTY);
      }

      private EmptyWaypoint(final UUID identifier) {
         super(Either.left(identifier), Waypoint.Icon.NULL, TrackedWaypoint.Type.EMPTY);
      }

      @Override
      public void update(final TrackedWaypoint other) {
      }

      @Override
      public void writeContents(final ByteBuf buf) {
      }

      @Override
      public double yawAngleToCamera(final Level level, final TrackedWaypoint.Camera camera, final PartialTickSupplier partialTickSupplier) {
         return Double.NaN;
      }

      @Override
      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(
         final Level level, final TrackedWaypoint.Projector projector, final PartialTickSupplier partialTickSupplier
      ) {
         return TrackedWaypoint.PitchDirection.NONE;
      }

      @Override
      public double distanceSquared(final Entity fromEntity) {
         return Double.POSITIVE_INFINITY;
      }
   }

   public enum PitchDirection {
      NONE,
      UP,
      DOWN;
   }

   public interface Projector {
      Vec3 projectPointToScreen(final Vec3 point);

      double projectHorizonToScreen();
   }

   private enum Type {
      EMPTY(TrackedWaypoint.EmptyWaypoint::new),
      VEC3I(TrackedWaypoint.Vec3iWaypoint::new),
      CHUNK(TrackedWaypoint.ChunkWaypoint::new),
      AZIMUTH(TrackedWaypoint.AzimuthWaypoint::new);

      private final TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint> constructor;

      Type(final TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint> constructor) {
         this.constructor = constructor;
      }
   }

   private static class Vec3iWaypoint extends TrackedWaypoint {
      private Vec3i vector;

      public Vec3iWaypoint(final UUID identifier, final Waypoint.Icon icon, final Vec3i vector) {
         super(Either.left(identifier), icon, TrackedWaypoint.Type.VEC3I);
         this.vector = vector;
      }

      public Vec3iWaypoint(final Either<UUID, String> identifier, final Waypoint.Icon icon, final FriendlyByteBuf byteBuf) {
         super(identifier, icon, TrackedWaypoint.Type.VEC3I);
         this.vector = new Vec3i(byteBuf.readVarInt(), byteBuf.readVarInt(), byteBuf.readVarInt());
      }

      @Override
      public void update(final TrackedWaypoint other) {
         if (other instanceof TrackedWaypoint.Vec3iWaypoint vec3iWaypoint) {
            this.vector = vec3iWaypoint.vector;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", other.getClass());
         }
      }

      @Override
      public void writeContents(final ByteBuf buf) {
         VarInt.write(buf, this.vector.getX());
         VarInt.write(buf, this.vector.getY());
         VarInt.write(buf, this.vector.getZ());
      }

      private Vec3 position(final Level level, final PartialTickSupplier partialTick) {
         return this.identifier
            .left()
            .map(level::getEntity)
            .map(e -> e.blockPosition().distManhattan(this.vector) > 3 ? null : e.getEyePosition(partialTick.apply(e)))
            .orElseGet(() -> Vec3.atCenterOf(this.vector));
      }

      @Override
      public double yawAngleToCamera(final Level level, final TrackedWaypoint.Camera camera, final PartialTickSupplier partialTickSupplier) {
         Vec3 direction = camera.position().subtract(this.position(level, partialTickSupplier)).rotateClockwise90();
         float waypointAngle = (float)Mth.atan2(direction.z(), direction.x()) * (180.0F / (float)Math.PI);
         return Mth.degreesDifference(camera.yaw(), waypointAngle);
      }

      @Override
      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(
         final Level level, final TrackedWaypoint.Projector projector, final PartialTickSupplier partialTickSupplier
      ) {
         Vec3 pointOnScreen = projector.projectPointToScreen(this.position(level, partialTickSupplier));
         boolean isBehindCamera = pointOnScreen.z > 1.0;
         double yInFrontOfCamera = isBehindCamera ? -pointOnScreen.y : pointOnScreen.y;
         if (yInFrontOfCamera < -1.0) {
            return TrackedWaypoint.PitchDirection.DOWN;
         }

         if (yInFrontOfCamera > 1.0) {
            return TrackedWaypoint.PitchDirection.UP;
         }

         if (isBehindCamera) {
            if (pointOnScreen.y > 0.0) {
               return TrackedWaypoint.PitchDirection.UP;
            }

            if (pointOnScreen.y < 0.0) {
               return TrackedWaypoint.PitchDirection.DOWN;
            }
         }

         return TrackedWaypoint.PitchDirection.NONE;
      }

      @Override
      public double distanceSquared(final Entity fromEntity) {
         return fromEntity.distanceToSqr(Vec3.atCenterOf(this.vector));
      }
   }
}
