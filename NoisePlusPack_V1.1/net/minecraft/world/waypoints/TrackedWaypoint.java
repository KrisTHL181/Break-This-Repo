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
   static final Logger LOGGER = LogUtils.getLogger();
   public static final StreamCodec<ByteBuf, TrackedWaypoint> STREAM_CODEC = StreamCodec.ofMember(TrackedWaypoint::write, TrackedWaypoint::read);
   protected final Either<UUID, String> identifier;
   private final Waypoint.Icon icon;
   private final TrackedWaypoint.Type type;

   TrackedWaypoint(Either<UUID, String> p_409804_, Waypoint.Icon p_407085_, TrackedWaypoint.Type p_407441_) {
      this.identifier = p_409804_;
      this.icon = p_407085_;
      this.type = p_407441_;
   }

   public Either<UUID, String> id() {
      return this.identifier;
   }

   public abstract void update(TrackedWaypoint var1);

   public void write(ByteBuf p_408987_) {
      FriendlyByteBuf friendlybytebuf = new FriendlyByteBuf(p_408987_);
      friendlybytebuf.writeEither(this.identifier, UUIDUtil.STREAM_CODEC, FriendlyByteBuf::writeUtf);
      Waypoint.Icon.STREAM_CODEC.encode(friendlybytebuf, this.icon);
      friendlybytebuf.writeEnum(this.type);
      this.writeContents(p_408987_);
   }

   public abstract void writeContents(ByteBuf var1);

   private static TrackedWaypoint read(ByteBuf p_410111_) {
      FriendlyByteBuf friendlybytebuf = new FriendlyByteBuf(p_410111_);
      Either<UUID, String> either = friendlybytebuf.readEither(UUIDUtil.STREAM_CODEC, FriendlyByteBuf::readUtf);
      Waypoint.Icon waypoint$icon = Waypoint.Icon.STREAM_CODEC.decode(friendlybytebuf);
      TrackedWaypoint.Type trackedwaypoint$type = friendlybytebuf.readEnum(TrackedWaypoint.Type.class);
      return (TrackedWaypoint)trackedwaypoint$type.constructor.apply(either, waypoint$icon, friendlybytebuf);
   }

   public static TrackedWaypoint setPosition(UUID p_410378_, Waypoint.Icon p_410746_, Vec3i p_410084_) {
      return new TrackedWaypoint.Vec3iWaypoint(p_410378_, p_410746_, p_410084_);
   }

   public static TrackedWaypoint setChunk(UUID p_409592_, Waypoint.Icon p_409699_, ChunkPos p_410738_) {
      return new TrackedWaypoint.ChunkWaypoint(p_409592_, p_409699_, p_410738_);
   }

   public static TrackedWaypoint setAzimuth(UUID p_407857_, Waypoint.Icon p_406574_, float p_407043_) {
      return new TrackedWaypoint.AzimuthWaypoint(p_407857_, p_406574_, p_407043_);
   }

   public static TrackedWaypoint empty(UUID p_407817_) {
      return new TrackedWaypoint.EmptyWaypoint(p_407817_);
   }

   public abstract double yawAngleToCamera(Level var1, TrackedWaypoint.Camera var2, PartialTickSupplier var3);

   public abstract TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level var1, TrackedWaypoint.Projector var2, PartialTickSupplier var3);

   public abstract double distanceSquared(Entity var1);

   public Waypoint.Icon icon() {
      return this.icon;
   }

   static class AzimuthWaypoint extends TrackedWaypoint {
      private float angle;

      public AzimuthWaypoint(UUID p_409219_, Waypoint.Icon p_410737_, float p_406858_) {
         super(Either.left(p_409219_), p_410737_, TrackedWaypoint.Type.AZIMUTH);
         this.angle = p_406858_;
      }

      public AzimuthWaypoint(Either<UUID, String> p_406458_, Waypoint.Icon p_409043_, FriendlyByteBuf p_409061_) {
         super(p_406458_, p_409043_, TrackedWaypoint.Type.AZIMUTH);
         this.angle = p_409061_.readFloat();
      }

      @Override
      public void update(TrackedWaypoint p_406882_) {
         if (p_406882_ instanceof TrackedWaypoint.AzimuthWaypoint trackedwaypoint$azimuthwaypoint) {
            this.angle = trackedwaypoint$azimuthwaypoint.angle;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", p_406882_.getClass());
         }
      }

      @Override
      public void writeContents(ByteBuf p_407138_) {
         p_407138_.writeFloat(this.angle);
      }

      @Override
      public double yawAngleToCamera(Level p_408876_, TrackedWaypoint.Camera p_409314_, PartialTickSupplier p_430455_) {
         return Mth.degreesDifference(p_409314_.yaw(), this.angle * (180.0F / (float)Math.PI));
      }

      @Override
      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level p_410040_, TrackedWaypoint.Projector p_409196_, PartialTickSupplier p_428706_) {
         double d0 = p_409196_.projectHorizonToScreen();
         if (d0 < -1.0) {
            return TrackedWaypoint.PitchDirection.DOWN;
         } else {
            return d0 > 1.0 ? TrackedWaypoint.PitchDirection.UP : TrackedWaypoint.PitchDirection.NONE;
         }
      }

      @Override
      public double distanceSquared(Entity p_408029_) {
         return Double.POSITIVE_INFINITY;
      }
   }

   public interface Camera {
      float yaw();

      Vec3 position();
   }

   static class ChunkWaypoint extends TrackedWaypoint {
      private ChunkPos chunkPos;

      public ChunkWaypoint(UUID p_409430_, Waypoint.Icon p_408027_, ChunkPos p_408783_) {
         super(Either.left(p_409430_), p_408027_, TrackedWaypoint.Type.CHUNK);
         this.chunkPos = p_408783_;
      }

      public ChunkWaypoint(Either<UUID, String> p_406686_, Waypoint.Icon p_407598_, FriendlyByteBuf p_405993_) {
         super(p_406686_, p_407598_, TrackedWaypoint.Type.CHUNK);
         this.chunkPos = new ChunkPos(p_405993_.readVarInt(), p_405993_.readVarInt());
      }

      @Override
      public void update(TrackedWaypoint p_410032_) {
         if (p_410032_ instanceof TrackedWaypoint.ChunkWaypoint trackedwaypoint$chunkwaypoint) {
            this.chunkPos = trackedwaypoint$chunkwaypoint.chunkPos;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", p_410032_.getClass());
         }
      }

      @Override
      public void writeContents(ByteBuf p_409801_) {
         VarInt.write(p_409801_, this.chunkPos.x);
         VarInt.write(p_409801_, this.chunkPos.z);
      }

      private Vec3 position(double p_408206_) {
         return Vec3.atCenterOf(this.chunkPos.getMiddleBlockPosition((int)p_408206_));
      }

      @Override
      public double yawAngleToCamera(Level p_406004_, TrackedWaypoint.Camera p_409311_, PartialTickSupplier p_423267_) {
         Vec3 vec3 = p_409311_.position();
         Vec3 vec31 = vec3.subtract(this.position(vec3.y())).rotateClockwise90();
         float f = (float)Mth.atan2(vec31.z(), vec31.x()) * (180.0F / (float)Math.PI);
         return Mth.degreesDifference(p_409311_.yaw(), f);
      }

      @Override
      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level p_408195_, TrackedWaypoint.Projector p_408761_, PartialTickSupplier p_428340_) {
         double d0 = p_408761_.projectHorizonToScreen();
         if (d0 < -1.0) {
            return TrackedWaypoint.PitchDirection.DOWN;
         } else {
            return d0 > 1.0 ? TrackedWaypoint.PitchDirection.UP : TrackedWaypoint.PitchDirection.NONE;
         }
      }

      @Override
      public double distanceSquared(Entity p_410438_) {
         return p_410438_.distanceToSqr(Vec3.atCenterOf(this.chunkPos.getMiddleBlockPosition(p_410438_.getBlockY())));
      }
   }

   static class EmptyWaypoint extends TrackedWaypoint {
      private EmptyWaypoint(Either<UUID, String> p_408105_, Waypoint.Icon p_406754_, FriendlyByteBuf p_406517_) {
         super(p_408105_, p_406754_, TrackedWaypoint.Type.EMPTY);
      }

      EmptyWaypoint(UUID p_408096_) {
         super(Either.left(p_408096_), Waypoint.Icon.NULL, TrackedWaypoint.Type.EMPTY);
      }

      @Override
      public void update(TrackedWaypoint p_410388_) {
      }

      @Override
      public void writeContents(ByteBuf p_407110_) {
      }

      @Override
      public double yawAngleToCamera(Level p_408498_, TrackedWaypoint.Camera p_407913_, PartialTickSupplier p_427588_) {
         return Double.NaN;
      }

      @Override
      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level p_409138_, TrackedWaypoint.Projector p_407627_, PartialTickSupplier p_422616_) {
         return TrackedWaypoint.PitchDirection.NONE;
      }

      @Override
      public double distanceSquared(Entity p_408264_) {
         return Double.POSITIVE_INFINITY;
      }
   }

   public enum PitchDirection {
      NONE,
      UP,
      DOWN;
   }

   public interface Projector {
      Vec3 projectPointToScreen(Vec3 var1);

      double projectHorizonToScreen();
   }

   enum Type {
      EMPTY(TrackedWaypoint.EmptyWaypoint::new),
      VEC3I(TrackedWaypoint.Vec3iWaypoint::new),
      CHUNK(TrackedWaypoint.ChunkWaypoint::new),
      AZIMUTH(TrackedWaypoint.AzimuthWaypoint::new);

      final TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint> constructor;

      Type(final TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint> p_407775_) {
         this.constructor = p_407775_;
      }
   }

   static class Vec3iWaypoint extends TrackedWaypoint {
      private Vec3i vector;

      public Vec3iWaypoint(UUID p_406801_, Waypoint.Icon p_408551_, Vec3i p_409526_) {
         super(Either.left(p_406801_), p_408551_, TrackedWaypoint.Type.VEC3I);
         this.vector = p_409526_;
      }

      public Vec3iWaypoint(Either<UUID, String> p_407185_, Waypoint.Icon p_407780_, FriendlyByteBuf p_406924_) {
         super(p_407185_, p_407780_, TrackedWaypoint.Type.VEC3I);
         this.vector = new Vec3i(p_406924_.readVarInt(), p_406924_.readVarInt(), p_406924_.readVarInt());
      }

      @Override
      public void update(TrackedWaypoint p_407449_) {
         if (p_407449_ instanceof TrackedWaypoint.Vec3iWaypoint trackedwaypoint$vec3iwaypoint) {
            this.vector = trackedwaypoint$vec3iwaypoint.vector;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", p_407449_.getClass());
         }
      }

      @Override
      public void writeContents(ByteBuf p_407331_) {
         VarInt.write(p_407331_, this.vector.getX());
         VarInt.write(p_407331_, this.vector.getY());
         VarInt.write(p_407331_, this.vector.getZ());
      }

      private Vec3 position(Level p_409788_, PartialTickSupplier p_423067_) {
         return this.identifier
            .left()
            .map(p_409788_::getEntity)
            .map(p_422278_ -> p_422278_.blockPosition().distManhattan(this.vector) > 3 ? null : p_422278_.getEyePosition(p_423067_.apply(p_422278_)))
            .orElseGet(() -> Vec3.atCenterOf(this.vector));
      }

      @Override
      public double yawAngleToCamera(Level p_407511_, TrackedWaypoint.Camera p_406431_, PartialTickSupplier p_423582_) {
         Vec3 vec3 = p_406431_.position().subtract(this.position(p_407511_, p_423582_)).rotateClockwise90();
         float f = (float)Mth.atan2(vec3.z(), vec3.x()) * (180.0F / (float)Math.PI);
         return Mth.degreesDifference(p_406431_.yaw(), f);
      }

      @Override
      public TrackedWaypoint.PitchDirection pitchDirectionToCamera(Level p_409720_, TrackedWaypoint.Projector p_410220_, PartialTickSupplier p_428970_) {
         Vec3 vec3 = p_410220_.projectPointToScreen(this.position(p_409720_, p_428970_));
         boolean flag = vec3.z > 1.0;
         double d0 = flag ? -vec3.y : vec3.y;
         if (d0 < -1.0) {
            return TrackedWaypoint.PitchDirection.DOWN;
         }

         if (d0 > 1.0) {
            return TrackedWaypoint.PitchDirection.UP;
         }

         if (flag) {
            if (vec3.y > 0.0) {
               return TrackedWaypoint.PitchDirection.UP;
            }

            if (vec3.y < 0.0) {
               return TrackedWaypoint.PitchDirection.DOWN;
            }
         }

         return TrackedWaypoint.PitchDirection.NONE;
      }

      @Override
      public double distanceSquared(Entity p_410208_) {
         return p_410208_.distanceToSqr(Vec3.atCenterOf(this.vector));
      }
   }
}
