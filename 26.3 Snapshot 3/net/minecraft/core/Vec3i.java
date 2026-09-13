package net.minecraft.core;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.stream.IntStream;
import javax.annotation.concurrent.Immutable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.joml.Vector3i;

@Immutable
public class Vec3i implements Comparable<Vec3i> {
   public static final Codec<Vec3i> CODEC = Codec.INT_STREAM
      .comapFlatMap(
         input -> Util.fixedSize(input, 3).map(ints -> new Vec3i(ints[0], ints[1], ints[2])), pos -> IntStream.of(pos.getX(), pos.getY(), pos.getZ())
      );
   public static final StreamCodec<ByteBuf, Vec3i> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, Vec3i::getX, ByteBufCodecs.VAR_INT, Vec3i::getY, ByteBufCodecs.VAR_INT, Vec3i::getZ, Vec3i::new
   );
   public static final Vec3i ZERO = new Vec3i(0, 0, 0);
   private int x;
   private int y;
   private int z;

   public static Codec<Vec3i> offsetCodec(final int maxOffsetPerAxis) {
      return CODEC.validate(
         value -> Math.abs(value.getX()) < maxOffsetPerAxis && Math.abs(value.getY()) < maxOffsetPerAxis && Math.abs(value.getZ()) < maxOffsetPerAxis
            ? DataResult.success(value)
            : DataResult.error(() -> "Position out of range, expected at most " + maxOffsetPerAxis + ": " + value)
      );
   }

   public Vec3i(final int x, final int y, final int z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof Vec3i vec3i) ? false : this.getX() == vec3i.getX() && this.getY() == vec3i.getY() && this.getZ() == vec3i.getZ();
      }
   }

   @Override
   public int hashCode() {
      return (this.getY() + this.getZ() * 31) * 31 + this.getX();
   }

   public int compareTo(final Vec3i pos) {
      if (this.getY() == pos.getY()) {
         return this.getZ() == pos.getZ() ? this.getX() - pos.getX() : this.getZ() - pos.getZ();
      } else {
         return this.getY() - pos.getY();
      }
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getZ() {
      return this.z;
   }

   protected Vec3i setX(final int x) {
      this.x = x;
      return this;
   }

   protected Vec3i setY(final int y) {
      this.y = y;
      return this;
   }

   protected Vec3i setZ(final int z) {
      this.z = z;
      return this;
   }

   public Vec3i offset(final int x, final int y, final int z) {
      return x == 0 && y == 0 && z == 0 ? this : new Vec3i(this.getX() + x, this.getY() + y, this.getZ() + z);
   }

   public Vec3i offset(final Vec3i vec) {
      return this.offset(vec.getX(), vec.getY(), vec.getZ());
   }

   public Vec3i subtract(final Vec3i vec) {
      return this.offset(-vec.getX(), -vec.getY(), -vec.getZ());
   }

   public Vec3i multiply(final int scale) {
      if (scale == 1) {
         return this;
      } else {
         return scale == 0 ? ZERO : new Vec3i(this.getX() * scale, this.getY() * scale, this.getZ() * scale);
      }
   }

   public Vec3i multiply(final int xScale, final int yScale, final int zScale) {
      return new Vec3i(this.getX() * xScale, this.getY() * yScale, this.getZ() * zScale);
   }

   public Vec3i above() {
      return this.above(1);
   }

   public Vec3i above(final int steps) {
      return this.relative(Direction.UP, steps);
   }

   public Vec3i below() {
      return this.below(1);
   }

   public Vec3i below(final int steps) {
      return this.relative(Direction.DOWN, steps);
   }

   public Vec3i north() {
      return this.north(1);
   }

   public Vec3i north(final int steps) {
      return this.relative(Direction.NORTH, steps);
   }

   public Vec3i south() {
      return this.south(1);
   }

   public Vec3i south(final int steps) {
      return this.relative(Direction.SOUTH, steps);
   }

   public Vec3i west() {
      return this.west(1);
   }

   public Vec3i west(final int steps) {
      return this.relative(Direction.WEST, steps);
   }

   public Vec3i east() {
      return this.east(1);
   }

   public Vec3i east(final int steps) {
      return this.relative(Direction.EAST, steps);
   }

   public Vec3i relative(final Direction direction) {
      return this.relative(direction, 1);
   }

   public Vec3i relative(final Direction direction, final int steps) {
      return steps == 0
         ? this
         : new Vec3i(this.getX() + direction.getStepX() * steps, this.getY() + direction.getStepY() * steps, this.getZ() + direction.getStepZ() * steps);
   }

   public Vec3i relative(final Direction.Axis axis, final int steps) {
      if (steps == 0) {
         return this;
      }

      int xStep = axis == Direction.Axis.X ? steps : 0;
      int yStep = axis == Direction.Axis.Y ? steps : 0;
      int zStep = axis == Direction.Axis.Z ? steps : 0;
      return new Vec3i(this.getX() + xStep, this.getY() + yStep, this.getZ() + zStep);
   }

   public Vec3i cross(final Vec3i upVector) {
      return new Vec3i(
         this.getY() * upVector.getZ() - this.getZ() * upVector.getY(),
         this.getZ() * upVector.getX() - this.getX() * upVector.getZ(),
         this.getX() * upVector.getY() - this.getY() * upVector.getX()
      );
   }

   public boolean closerThan(final Vec3i pos, final double distance) {
      return this.distSqr(pos) < Mth.square(distance);
   }

   public boolean closerToCenterThan(final Position pos, final double distance) {
      return this.distToCenterSqr(pos) < Mth.square(distance);
   }

   public double distSqr(final Vec3i pos) {
      return this.distToLowCornerSqr(pos.getX(), pos.getY(), pos.getZ());
   }

   public double distToCenterSqr(final Position pos) {
      return this.distToCenterSqr(pos.x(), pos.y(), pos.z());
   }

   public double distToCenterSqr(final double x, final double y, final double z) {
      double dx = this.getX() + 0.5 - x;
      double dy = this.getY() + 0.5 - y;
      double dz = this.getZ() + 0.5 - z;
      return dx * dx + dy * dy + dz * dz;
   }

   public double distToLowCornerSqr(final double x, final double y, final double z) {
      double dx = this.getX() - x;
      double dy = this.getY() - y;
      double dz = this.getZ() - z;
      return dx * dx + dy * dy + dz * dz;
   }

   public int distManhattan(final Vec3i pos) {
      float xd = Math.abs(pos.getX() - this.getX());
      float yd = Math.abs(pos.getY() - this.getY());
      float zd = Math.abs(pos.getZ() - this.getZ());
      return (int)(xd + yd + zd);
   }

   public int distChessboard(final Vec3i pos) {
      int xd = Math.abs(this.getX() - pos.getX());
      int yd = Math.abs(this.getY() - pos.getY());
      int zd = Math.abs(this.getZ() - pos.getZ());
      return Math.max(Math.max(xd, yd), zd);
   }

   public int get(final Direction.Axis axis) {
      return axis.choose(this.x, this.y, this.z);
   }

   public Vector3i toMutable() {
      return new Vector3i(this.x, this.y, this.z);
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this).add("x", this.getX()).add("y", this.getY()).add("z", this.getZ()).toString();
   }

   public String toShortString() {
      return this.getX() + ", " + this.getY() + ", " + this.getZ();
   }
}
