package net.minecraft.world.phys;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.network.LpVec3;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Vec3 implements Position {
   public static final Codec<Vec3> CODEC = Codec.DOUBLE
      .listOf()
      .comapFlatMap(
         input -> Util.fixedSize(input, 3).map(doubles -> new Vec3((Double)doubles.get(0), (Double)doubles.get(1), (Double)doubles.get(2))),
         pos -> List.of(pos.x(), pos.y(), pos.z())
      );
   public static final StreamCodec<ByteBuf, Vec3> STREAM_CODEC = new StreamCodec<ByteBuf, Vec3>() {
      public Vec3 decode(final ByteBuf input) {
         return new Vec3(input.readDouble(), input.readDouble(), input.readDouble());
      }

      public void encode(final ByteBuf output, final Vec3 value) {
         output.writeDouble(value.x());
         output.writeDouble(value.y());
         output.writeDouble(value.z());
      }
   };
   public static final StreamCodec<ByteBuf, Vec3> LP_STREAM_CODEC = StreamCodec.of(LpVec3::write, LpVec3::read);
   public static final Vec3 ZERO = new Vec3(0.0, 0.0, 0.0);
   public static final Vec3 X_AXIS = new Vec3(1.0, 0.0, 0.0);
   public static final Vec3 Y_AXIS = new Vec3(0.0, 1.0, 0.0);
   public static final Vec3 Z_AXIS = new Vec3(0.0, 0.0, 1.0);
   public final double x;
   public final double y;
   public final double z;

   public static Vec3 atLowerCornerOf(final Vec3i pos) {
      return new Vec3(pos.getX(), pos.getY(), pos.getZ());
   }

   public static Vec3 atLowerCornerWithOffset(final Vec3i pos, final double x, final double y, final double z) {
      return new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
   }

   public static Vec3 atCenterOf(final Vec3i pos) {
      return atLowerCornerWithOffset(pos, 0.5, 0.5, 0.5);
   }

   public static Vec3 atCenterOfWithY(final Vec3i pos, final double y) {
      return new Vec3(pos.getX() + 0.5, y, pos.getZ() + 0.5);
   }

   public static Vec3 atBottomCenterOf(final Vec3i pos) {
      return atLowerCornerWithOffset(pos, 0.5, 0.0, 0.5);
   }

   public static Vec3 upFromBottomCenterOf(final Vec3i pos, final double yOffset) {
      return atLowerCornerWithOffset(pos, 0.5, yOffset, 0.5);
   }

   public Vec3(final double x, final double y, final double z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public Vec3(final Vector3fc vec) {
      this(vec.x(), vec.y(), vec.z());
   }

   public Vec3(final Vec3i vec) {
      this(vec.getX(), vec.getY(), vec.getZ());
   }

   public Vec3 vectorTo(final Vec3 vec) {
      return new Vec3(vec.x - this.x, vec.y - this.y, vec.z - this.z);
   }

   public Vec3 normalize() {
      double dist = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
      return dist < 1.0E-5F ? ZERO : new Vec3(this.x / dist, this.y / dist, this.z / dist);
   }

   public double dot(final Vec3 vec) {
      return this.x * vec.x + this.y * vec.y + this.z * vec.z;
   }

   public Vec3 cross(final Vec3 vec) {
      return new Vec3(this.y * vec.z - this.z * vec.y, this.z * vec.x - this.x * vec.z, this.x * vec.y - this.y * vec.x);
   }

   public Vec3 subtract(final Vec3 vec) {
      return this.subtract(vec.x, vec.y, vec.z);
   }

   public Vec3 subtract(final double value) {
      return this.subtract(value, value, value);
   }

   public Vec3 subtract(final double x, final double y, final double z) {
      return this.add(-x, -y, -z);
   }

   public Vec3 add(final double value) {
      return this.add(value, value, value);
   }

   public Vec3 add(final Vec3 vec) {
      return this.add(vec.x, vec.y, vec.z);
   }

   public Vec3 add(final double x, final double y, final double z) {
      return new Vec3(this.x + x, this.y + y, this.z + z);
   }

   public boolean closerThan(final Position pos, final double distance) {
      return this.distanceToSqr(pos.x(), pos.y(), pos.z()) < distance * distance;
   }

   public double distanceTo(final Vec3 vec) {
      double xd = vec.x - this.x;
      double yd = vec.y - this.y;
      double zd = vec.z - this.z;
      return Math.sqrt(xd * xd + yd * yd + zd * zd);
   }

   public double distanceToSqr(final Vec3 vec) {
      double xd = vec.x - this.x;
      double yd = vec.y - this.y;
      double zd = vec.z - this.z;
      return xd * xd + yd * yd + zd * zd;
   }

   public double distanceToSqr(final double x, final double y, final double z) {
      double xd = x - this.x;
      double yd = y - this.y;
      double zd = z - this.z;
      return xd * xd + yd * yd + zd * zd;
   }

   public boolean closerThan(final Vec3 vec, final double distanceXZ, final double distanceY) {
      double dx = vec.x() - this.x;
      double dy = vec.y() - this.y;
      double dz = vec.z() - this.z;
      return Mth.lengthSquared(dx, dz) < Mth.square(distanceXZ) && Math.abs(dy) < distanceY;
   }

   public Vec3 scale(final double scale) {
      return this.multiply(scale, scale, scale);
   }

   public Vec3 reverse() {
      return this.scale(-1.0);
   }

   public Vec3 multiply(final Vec3 scale) {
      return this.multiply(scale.x, scale.y, scale.z);
   }

   public Vec3 multiply(final double xScale, final double yScale, final double zScale) {
      return new Vec3(this.x * xScale, this.y * yScale, this.z * zScale);
   }

   public Vec3 horizontal() {
      return new Vec3(this.x, 0.0, this.z);
   }

   public Vec3 offsetRandom(final RandomSource random, final float offset) {
      return this.add((random.nextFloat() - 0.5F) * offset, (random.nextFloat() - 0.5F) * offset, (random.nextFloat() - 0.5F) * offset);
   }

   public Vec3 offsetRandomXZ(final RandomSource random, final float offset) {
      return this.add((random.nextFloat() - 0.5F) * offset, 0.0, (random.nextFloat() - 0.5F) * offset);
   }

   public double length() {
      return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
   }

   public double lengthSqr() {
      return this.x * this.x + this.y * this.y + this.z * this.z;
   }

   public double horizontalDistance() {
      return Math.sqrt(this.x * this.x + this.z * this.z);
   }

   public double horizontalDistanceSqr() {
      return this.x * this.x + this.z * this.z;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }

      if (o instanceof Vec3 vec3) {
         if (Double.compare(vec3.x, this.x) != 0) {
            return false;
         } else {
            return Double.compare(vec3.y, this.y) != 0 ? false : Double.compare(vec3.z, this.z) == 0;
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      long temp = Double.doubleToLongBits(this.x);
      int result = (int)(temp ^ temp >>> 32);
      temp = Double.doubleToLongBits(this.y);
      result = 31 * result + (int)(temp ^ temp >>> 32);
      temp = Double.doubleToLongBits(this.z);
      return 31 * result + (int)(temp ^ temp >>> 32);
   }

   @Override
   public String toString() {
      return "(" + this.x + ", " + this.y + ", " + this.z + ")";
   }

   public Vec3 lerp(final Vec3 vec, final double a) {
      return new Vec3(Mth.lerp(a, this.x, vec.x), Mth.lerp(a, this.y, vec.y), Mth.lerp(a, this.z, vec.z));
   }

   public Vec3 xRot(final float radians) {
      float cos = Mth.cos(radians);
      float sin = Mth.sin(radians);
      double xx = this.x;
      double yy = this.y * cos + this.z * sin;
      double zz = this.z * cos - this.y * sin;
      return new Vec3(xx, yy, zz);
   }

   public Vec3 yRot(final float radians) {
      float cos = Mth.cos(radians);
      float sin = Mth.sin(radians);
      double xx = this.x * cos + this.z * sin;
      double yy = this.y;
      double zz = this.z * cos - this.x * sin;
      return new Vec3(xx, yy, zz);
   }

   public Vec3 zRot(final float radians) {
      float cos = Mth.cos(radians);
      float sin = Mth.sin(radians);
      double xx = this.x * cos + this.y * sin;
      double yy = this.y * cos - this.x * sin;
      double zz = this.z;
      return new Vec3(xx, yy, zz);
   }

   public Vec3 rotateClockwise90() {
      return new Vec3(-this.z, this.y, this.x);
   }

   public static Vec3 directionFromRotation(final Vec2 rotation) {
      return directionFromRotation(rotation.x, rotation.y);
   }

   public static Vec3 directionFromRotation(final float rotX, final float rotY) {
      float yCos = Mth.cos(-rotY * (float) (Math.PI / 180.0) - (float) Math.PI);
      float ySin = Mth.sin(-rotY * (float) (Math.PI / 180.0) - (float) Math.PI);
      float xCos = -Mth.cos(-rotX * (float) (Math.PI / 180.0));
      float xSin = Mth.sin(-rotX * (float) (Math.PI / 180.0));
      return new Vec3(ySin * xCos, xSin, yCos * xCos);
   }

   public Vec2 rotation() {
      float yaw = (float)Math.atan2(-this.x, this.z) * (180.0F / (float)Math.PI);
      float pitch = (float)Math.asin(-this.y / Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z)) * (180.0F / (float)Math.PI);
      return new Vec2(pitch, yaw);
   }

   public Vec3 align(final EnumSet<Direction.Axis> axes) {
      double x = axes.contains(Direction.Axis.X) ? Mth.floor(this.x) : this.x;
      double y = axes.contains(Direction.Axis.Y) ? Mth.floor(this.y) : this.y;
      double z = axes.contains(Direction.Axis.Z) ? Mth.floor(this.z) : this.z;
      return new Vec3(x, y, z);
   }

   public double get(final Direction.Axis axis) {
      return axis.choose(this.x, this.y, this.z);
   }

   public Vec3 with(final Direction.Axis axis, final double value) {
      double x = axis == Direction.Axis.X ? value : this.x;
      double y = axis == Direction.Axis.Y ? value : this.y;
      double z = axis == Direction.Axis.Z ? value : this.z;
      return new Vec3(x, y, z);
   }

   public Vec3 relative(final Direction direction, final double distance) {
      Vec3i normal = direction.getUnitVec3i();
      return new Vec3(this.x + distance * normal.getX(), this.y + distance * normal.getY(), this.z + distance * normal.getZ());
   }

   @Override
   public final double x() {
      return this.x;
   }

   @Override
   public final double y() {
      return this.y;
   }

   @Override
   public final double z() {
      return this.z;
   }

   public Vector3f toVector3f() {
      return new Vector3f((float)this.x, (float)this.y, (float)this.z);
   }

   public Vec3 projectedOn(final Vec3 onto) {
      return onto.lengthSqr() == 0.0 ? onto : onto.scale(this.dot(onto)).scale(1.0 / onto.lengthSqr());
   }

   public static Vec3 applyLocalCoordinatesToRotation(final Vec2 rotation, final Vec3 direction) {
      float yCos = Mth.cos((rotation.y + 90.0F) * (float) (Math.PI / 180.0));
      float ySin = Mth.sin((rotation.y + 90.0F) * (float) (Math.PI / 180.0));
      float xCos = Mth.cos(-rotation.x * (float) (Math.PI / 180.0));
      float xSin = Mth.sin(-rotation.x * (float) (Math.PI / 180.0));
      float xCosUp = Mth.cos((-rotation.x + 90.0F) * (float) (Math.PI / 180.0));
      float xSinUp = Mth.sin((-rotation.x + 90.0F) * (float) (Math.PI / 180.0));
      Vec3 forwards = new Vec3(yCos * xCos, xSin, ySin * xCos);
      Vec3 up = new Vec3(yCos * xCosUp, xSinUp, ySin * xCosUp);
      Vec3 left = forwards.cross(up).scale(-1.0);
      double xa = forwards.x * direction.z + up.x * direction.y + left.x * direction.x;
      double ya = forwards.y * direction.z + up.y * direction.y + left.y * direction.x;
      double za = forwards.z * direction.z + up.z * direction.y + left.z * direction.x;
      return new Vec3(xa, ya, za);
   }

   public Vec3 addLocalCoordinates(final Vec3 direction) {
      return applyLocalCoordinatesToRotation(this.rotation(), direction);
   }

   public boolean isFinite() {
      return Double.isFinite(this.x) && Double.isFinite(this.y) && Double.isFinite(this.z);
   }
}
