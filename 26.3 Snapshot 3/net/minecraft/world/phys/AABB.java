package net.minecraft.world.phys;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class AABB {
   private static final double EPSILON = 1.0E-7;
   public final double minX;
   public final double minY;
   public final double minZ;
   public final double maxX;
   public final double maxY;
   public final double maxZ;

   public AABB(final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ) {
      this.minX = Math.min(minX, maxX);
      this.minY = Math.min(minY, maxY);
      this.minZ = Math.min(minZ, maxZ);
      this.maxX = Math.max(minX, maxX);
      this.maxY = Math.max(minY, maxY);
      this.maxZ = Math.max(minZ, maxZ);
   }

   public AABB(final BlockPos pos) {
      this(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
   }

   public AABB(final Vec3 begin, final Vec3 end) {
      this(begin.x, begin.y, begin.z, end.x, end.y, end.z);
   }

   public static AABB of(final BoundingBox box) {
      return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
   }

   public static AABB unitCubeFromLowerCorner(final Vec3 pos) {
      return new AABB(pos.x, pos.y, pos.z, pos.x + 1.0, pos.y + 1.0, pos.z + 1.0);
   }

   public static AABB encapsulatingFullBlocks(final BlockPos pos0, final BlockPos pos1) {
      return new AABB(
         Math.min(pos0.getX(), pos1.getX()),
         Math.min(pos0.getY(), pos1.getY()),
         Math.min(pos0.getZ(), pos1.getZ()),
         Math.max(pos0.getX(), pos1.getX()) + 1,
         Math.max(pos0.getY(), pos1.getY()) + 1,
         Math.max(pos0.getZ(), pos1.getZ()) + 1
      );
   }

   public AABB setMinX(final double minX) {
      return new AABB(minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
   }

   public AABB setMinY(final double minY) {
      return new AABB(this.minX, minY, this.minZ, this.maxX, this.maxY, this.maxZ);
   }

   public AABB setMinZ(final double minZ) {
      return new AABB(this.minX, this.minY, minZ, this.maxX, this.maxY, this.maxZ);
   }

   public AABB setMaxX(final double maxX) {
      return new AABB(this.minX, this.minY, this.minZ, maxX, this.maxY, this.maxZ);
   }

   public AABB setMaxY(final double maxY) {
      return new AABB(this.minX, this.minY, this.minZ, this.maxX, maxY, this.maxZ);
   }

   public AABB setMaxZ(final double maxZ) {
      return new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, maxZ);
   }

   public double min(final Direction.Axis axis) {
      return axis.choose(this.minX, this.minY, this.minZ);
   }

   public double max(final Direction.Axis axis) {
      return axis.choose(this.maxX, this.maxY, this.maxZ);
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }

      if (o instanceof AABB aabb) {
         if (Double.compare(aabb.minX, this.minX) != 0) {
            return false;
         } else if (Double.compare(aabb.minY, this.minY) != 0) {
            return false;
         } else if (Double.compare(aabb.minZ, this.minZ) != 0) {
            return false;
         } else if (Double.compare(aabb.maxX, this.maxX) != 0) {
            return false;
         } else {
            return Double.compare(aabb.maxY, this.maxY) != 0 ? false : Double.compare(aabb.maxZ, this.maxZ) == 0;
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      long temp = Double.doubleToLongBits(this.minX);
      int result = (int)(temp ^ temp >>> 32);
      temp = Double.doubleToLongBits(this.minY);
      result = 31 * result + (int)(temp ^ temp >>> 32);
      temp = Double.doubleToLongBits(this.minZ);
      result = 31 * result + (int)(temp ^ temp >>> 32);
      temp = Double.doubleToLongBits(this.maxX);
      result = 31 * result + (int)(temp ^ temp >>> 32);
      temp = Double.doubleToLongBits(this.maxY);
      result = 31 * result + (int)(temp ^ temp >>> 32);
      temp = Double.doubleToLongBits(this.maxZ);
      return 31 * result + (int)(temp ^ temp >>> 32);
   }

   public AABB contract(final double xa, final double ya, final double za) {
      double minX = this.minX;
      double minY = this.minY;
      double minZ = this.minZ;
      double maxX = this.maxX;
      double maxY = this.maxY;
      double maxZ = this.maxZ;
      if (xa < 0.0) {
         minX -= xa;
      } else if (xa > 0.0) {
         maxX -= xa;
      }

      if (ya < 0.0) {
         minY -= ya;
      } else if (ya > 0.0) {
         maxY -= ya;
      }

      if (za < 0.0) {
         minZ -= za;
      } else if (za > 0.0) {
         maxZ -= za;
      }

      return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
   }

   public AABB expandTowards(final Vec3 delta) {
      return this.expandTowards(delta.x, delta.y, delta.z);
   }

   public AABB expandTowards(final double xa, final double ya, final double za) {
      double minX = this.minX;
      double minY = this.minY;
      double minZ = this.minZ;
      double maxX = this.maxX;
      double maxY = this.maxY;
      double maxZ = this.maxZ;
      if (xa < 0.0) {
         minX += xa;
      } else if (xa > 0.0) {
         maxX += xa;
      }

      if (ya < 0.0) {
         minY += ya;
      } else if (ya > 0.0) {
         maxY += ya;
      }

      if (za < 0.0) {
         minZ += za;
      } else if (za > 0.0) {
         maxZ += za;
      }

      return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
   }

   public AABB inflate(final double xAdd, final double yAdd, final double zAdd) {
      double minX = this.minX - xAdd;
      double minY = this.minY - yAdd;
      double minZ = this.minZ - zAdd;
      double maxX = this.maxX + xAdd;
      double maxY = this.maxY + yAdd;
      double maxZ = this.maxZ + zAdd;
      return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
   }

   public AABB inflate(final double amountToAddInAllDirections) {
      return this.inflate(amountToAddInAllDirections, amountToAddInAllDirections, amountToAddInAllDirections);
   }

   public AABB intersect(final AABB other) {
      double minX = Math.max(this.minX, other.minX);
      double minY = Math.max(this.minY, other.minY);
      double minZ = Math.max(this.minZ, other.minZ);
      double maxX = Math.min(this.maxX, other.maxX);
      double maxY = Math.min(this.maxY, other.maxY);
      double maxZ = Math.min(this.maxZ, other.maxZ);
      return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
   }

   public AABB minmax(final AABB other) {
      double minX = Math.min(this.minX, other.minX);
      double minY = Math.min(this.minY, other.minY);
      double minZ = Math.min(this.minZ, other.minZ);
      double maxX = Math.max(this.maxX, other.maxX);
      double maxY = Math.max(this.maxY, other.maxY);
      double maxZ = Math.max(this.maxZ, other.maxZ);
      return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
   }

   public AABB move(final double xa, final double ya, final double za) {
      return new AABB(this.minX + xa, this.minY + ya, this.minZ + za, this.maxX + xa, this.maxY + ya, this.maxZ + za);
   }

   public AABB move(final BlockPos pos) {
      return new AABB(
         this.minX + pos.getX(), this.minY + pos.getY(), this.minZ + pos.getZ(), this.maxX + pos.getX(), this.maxY + pos.getY(), this.maxZ + pos.getZ()
      );
   }

   public AABB move(final Vec3 pos) {
      return this.move(pos.x, pos.y, pos.z);
   }

   public AABB move(final Vector3f pos) {
      return this.move(pos.x, pos.y, pos.z);
   }

   public boolean intersects(final AABB aabb) {
      return this.intersects(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
   }

   public boolean intersects(final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ) {
      return this.minX < maxX && this.maxX > minX && this.minY < maxY && this.maxY > minY && this.minZ < maxZ && this.maxZ > minZ;
   }

   public boolean intersects(final Vec3 min, final Vec3 max) {
      return this.intersects(
         Math.min(min.x, max.x), Math.min(min.y, max.y), Math.min(min.z, max.z), Math.max(min.x, max.x), Math.max(min.y, max.y), Math.max(min.z, max.z)
      );
   }

   public boolean intersects(final BlockPos pos) {
      return this.intersects(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
   }

   public boolean contains(final Vec3 vec) {
      return this.contains(vec.x, vec.y, vec.z);
   }

   public boolean contains(final double x, final double y, final double z) {
      return x >= this.minX && x < this.maxX && y >= this.minY && y < this.maxY && z >= this.minZ && z < this.maxZ;
   }

   public double getSize() {
      double xs = this.getXsize();
      double ys = this.getYsize();
      double zs = this.getZsize();
      return (xs + ys + zs) / 3.0;
   }

   public double getXsize() {
      return this.maxX - this.minX;
   }

   public double getYsize() {
      return this.maxY - this.minY;
   }

   public double getZsize() {
      return this.maxZ - this.minZ;
   }

   public AABB deflate(final double xSubstract, final double ySubtract, final double zSubtract) {
      return this.inflate(-xSubstract, -ySubtract, -zSubtract);
   }

   public AABB deflate(final double amount) {
      return this.inflate(-amount);
   }

   public Optional<Vec3> clip(final Vec3 from, final Vec3 to) {
      return clip(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ, from, to);
   }

   public static Optional<Vec3> clip(
      final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ, final Vec3 from, final Vec3 to
   ) {
      double[] scaleReference = new double[]{1.0};
      double dx = to.x - from.x;
      double dy = to.y - from.y;
      double dz = to.z - from.z;
      Direction direction = getDirection(minX, minY, minZ, maxX, maxY, maxZ, from, scaleReference, null, dx, dy, dz);
      if (direction == null) {
         return Optional.empty();
      }

      double scale = scaleReference[0];
      return Optional.of(from.add(scale * dx, scale * dy, scale * dz));
   }

   public static @Nullable BlockHitResult clip(final Iterable<AABB> aabBs, final Vec3 from, final Vec3 to, final BlockPos pos) {
      double[] scaleReference = new double[]{1.0};
      Direction direction = null;
      double dx = to.x - from.x;
      double dy = to.y - from.y;
      double dz = to.z - from.z;

      for (AABB aabb : aabBs) {
         direction = getDirection(aabb.move(pos), from, scaleReference, direction, dx, dy, dz);
      }

      if (direction == null) {
         return null;
      }

      double scale = scaleReference[0];
      return new BlockHitResult(from.add(scale * dx, scale * dy, scale * dz), direction, pos, false);
   }

   private static @Nullable Direction getDirection(
      final AABB aabb, final Vec3 from, final double[] scaleReference, final @Nullable Direction direction, final double dx, final double dy, final double dz
   ) {
      return getDirection(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, from, scaleReference, direction, dx, dy, dz);
   }

   private static @Nullable Direction getDirection(
      final double minX,
      final double minY,
      final double minZ,
      final double maxX,
      final double maxY,
      final double maxZ,
      final Vec3 from,
      final double[] scaleReference,
      @Nullable Direction direction,
      final double dx,
      final double dy,
      final double dz
   ) {
      if (dx > 1.0E-7) {
         direction = clipPoint(scaleReference, direction, dx, dy, dz, minX, minY, maxY, minZ, maxZ, Direction.WEST, from.x, from.y, from.z);
      } else if (dx < -1.0E-7) {
         direction = clipPoint(scaleReference, direction, dx, dy, dz, maxX, minY, maxY, minZ, maxZ, Direction.EAST, from.x, from.y, from.z);
      }

      if (dy > 1.0E-7) {
         direction = clipPoint(scaleReference, direction, dy, dz, dx, minY, minZ, maxZ, minX, maxX, Direction.DOWN, from.y, from.z, from.x);
      } else if (dy < -1.0E-7) {
         direction = clipPoint(scaleReference, direction, dy, dz, dx, maxY, minZ, maxZ, minX, maxX, Direction.UP, from.y, from.z, from.x);
      }

      if (dz > 1.0E-7) {
         direction = clipPoint(scaleReference, direction, dz, dx, dy, minZ, minX, maxX, minY, maxY, Direction.NORTH, from.z, from.x, from.y);
      } else if (dz < -1.0E-7) {
         direction = clipPoint(scaleReference, direction, dz, dx, dy, maxZ, minX, maxX, minY, maxY, Direction.SOUTH, from.z, from.x, from.y);
      }

      return direction;
   }

   private static @Nullable Direction clipPoint(
      final double[] scaleReference,
      final @Nullable Direction direction,
      final double da,
      final double db,
      final double dc,
      final double point,
      final double minB,
      final double maxB,
      final double minC,
      final double maxC,
      final Direction newDirection,
      final double fromA,
      final double fromB,
      final double fromC
   ) {
      double s = (point - fromA) / da;
      double pb = fromB + s * db;
      double pc = fromC + s * dc;
      if (0.0 < s && s < scaleReference[0] && minB - 1.0E-7 < pb && pb < maxB + 1.0E-7 && minC - 1.0E-7 < pc && pc < maxC + 1.0E-7) {
         scaleReference[0] = s;
         return newDirection;
      } else {
         return direction;
      }
   }

   public boolean collidedAlongVector(final Vec3 vector, final List<AABB> aabbs) {
      Vec3 from = this.getCenter();
      Vec3 to = from.add(vector);

      for (AABB shapePart : aabbs) {
         AABB inflated = shapePart.inflate(this.getXsize() * 0.5 - 1.0E-7, this.getYsize() * 0.5 - 1.0E-7, this.getZsize() * 0.5 - 1.0E-7);
         if (inflated.contains(to) || inflated.contains(from)) {
            return true;
         }

         if (inflated.clip(from, to).isPresent()) {
            return true;
         }
      }

      return false;
   }

   public double distanceToSqr(final Vec3 point) {
      double dx = Math.max(Math.max(this.minX - point.x, point.x - this.maxX), 0.0);
      double dy = Math.max(Math.max(this.minY - point.y, point.y - this.maxY), 0.0);
      double dz = Math.max(Math.max(this.minZ - point.z, point.z - this.maxZ), 0.0);
      return Mth.lengthSquared(dx, dy, dz);
   }

   public double distanceToSqr(final AABB boundingBox) {
      double dx = Math.max(Math.max(this.minX - boundingBox.maxX, boundingBox.minX - this.maxX), 0.0);
      double dy = Math.max(Math.max(this.minY - boundingBox.maxY, boundingBox.minY - this.maxY), 0.0);
      double dz = Math.max(Math.max(this.minZ - boundingBox.maxZ, boundingBox.minZ - this.maxZ), 0.0);
      return Mth.lengthSquared(dx, dy, dz);
   }

   @Override
   public String toString() {
      return "AABB[" + this.minX + ", " + this.minY + ", " + this.minZ + "] -> [" + this.maxX + ", " + this.maxY + ", " + this.maxZ + "]";
   }

   public boolean hasNaN() {
      return Double.isNaN(this.minX)
         || Double.isNaN(this.minY)
         || Double.isNaN(this.minZ)
         || Double.isNaN(this.maxX)
         || Double.isNaN(this.maxY)
         || Double.isNaN(this.maxZ);
   }

   public Vec3 getCenter() {
      return new Vec3(Mth.lerp(0.5, this.minX, this.maxX), Mth.lerp(0.5, this.minY, this.maxY), Mth.lerp(0.5, this.minZ, this.maxZ));
   }

   public Vec3 getBottomCenter() {
      return new Vec3(Mth.lerp(0.5, this.minX, this.maxX), this.minY, Mth.lerp(0.5, this.minZ, this.maxZ));
   }

   public Vec3 getMinPosition() {
      return new Vec3(this.minX, this.minY, this.minZ);
   }

   public Vec3 getMaxPosition() {
      return new Vec3(this.maxX, this.maxY, this.maxZ);
   }

   public static AABB ofSize(final Vec3 center, final double sizeX, final double sizeY, final double sizeZ) {
      return new AABB(
         center.x - sizeX / 2.0, center.y - sizeY / 2.0, center.z - sizeZ / 2.0, center.x + sizeX / 2.0, center.y + sizeY / 2.0, center.z + sizeZ / 2.0
      );
   }

   public AABB nextDeflated() {
      return new AABB(
         Math.nextUp(this.minX), Math.nextUp(this.minY), Math.nextUp(this.minZ), Math.nextDown(this.maxX), Math.nextDown(this.maxY), Math.nextDown(this.maxZ)
      );
   }

   public static class Builder {
      private float minX = Float.POSITIVE_INFINITY;
      private float minY = Float.POSITIVE_INFINITY;
      private float minZ = Float.POSITIVE_INFINITY;
      private float maxX = Float.NEGATIVE_INFINITY;
      private float maxY = Float.NEGATIVE_INFINITY;
      private float maxZ = Float.NEGATIVE_INFINITY;
      private boolean defined;

      public void include(final Vector3fc v) {
         this.minX = Math.min(this.minX, v.x());
         this.minY = Math.min(this.minY, v.y());
         this.minZ = Math.min(this.minZ, v.z());
         this.maxX = Math.max(this.maxX, v.x());
         this.maxY = Math.max(this.maxY, v.y());
         this.maxZ = Math.max(this.maxZ, v.z());
         this.defined = true;
      }

      public boolean isDefined() {
         return this.defined;
      }

      public AABB build() {
         if (!this.defined) {
            throw new IllegalStateException("Cannot build an undefined AABB. Include at least one point.");
         } else {
            return new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
         }
      }
   }
}
