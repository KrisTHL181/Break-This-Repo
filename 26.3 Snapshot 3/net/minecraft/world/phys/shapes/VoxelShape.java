package net.minecraft.world.phys.shapes;

import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public abstract class VoxelShape {
   protected final DiscreteVoxelShape shape;
   private @Nullable VoxelShape @Nullable [] faces;

   protected VoxelShape(final DiscreteVoxelShape shape) {
      this.shape = shape;
   }

   public double min(final Direction.Axis axis) {
      int i = this.shape.firstFull(axis);
      return i >= this.shape.getSize(axis) ? Double.POSITIVE_INFINITY : this.get(axis, i);
   }

   public double max(final Direction.Axis axis) {
      int i = this.shape.lastFull(axis);
      return i <= 0 ? Double.NEGATIVE_INFINITY : this.get(axis, i);
   }

   public AABB bounds() {
      if (this.isEmpty()) {
         throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("No bounds for empty shape."));
      } else {
         return new AABB(
            this.min(Direction.Axis.X),
            this.min(Direction.Axis.Y),
            this.min(Direction.Axis.Z),
            this.max(Direction.Axis.X),
            this.max(Direction.Axis.Y),
            this.max(Direction.Axis.Z)
         );
      }
   }

   public VoxelShape singleEncompassing() {
      return this.isEmpty()
         ? Shapes.empty()
         : Shapes.box(
            this.min(Direction.Axis.X),
            this.min(Direction.Axis.Y),
            this.min(Direction.Axis.Z),
            this.max(Direction.Axis.X),
            this.max(Direction.Axis.Y),
            this.max(Direction.Axis.Z)
         );
   }

   protected double get(final Direction.Axis axis, final int i) {
      return this.getCoords(axis).getDouble(i);
   }

   public abstract DoubleList getCoords(final Direction.Axis axis);

   public boolean isEmpty() {
      return this.shape.isEmpty();
   }

   public VoxelShape move(final Vec3 delta) {
      return this.move(delta.x, delta.y, delta.z);
   }

   public VoxelShape move(final Vec3i delta) {
      return this.move(delta.getX(), delta.getY(), delta.getZ());
   }

   public VoxelShape move(final double dx, final double dy, final double dz) {
      return this.isEmpty()
         ? Shapes.empty()
         : new ArrayVoxelShape(
            this.shape,
            new OffsetDoubleList(this.getCoords(Direction.Axis.X), dx),
            new OffsetDoubleList(this.getCoords(Direction.Axis.Y), dy),
            new OffsetDoubleList(this.getCoords(Direction.Axis.Z), dz)
         );
   }

   public VoxelShape optimize() {
      VoxelShape[] result = new VoxelShape[]{Shapes.empty()};
      this.forAllBoxes((x1, y1, z1, x2, y2, z2) -> result[0] = Shapes.joinUnoptimized(result[0], Shapes.box(x1, y1, z1, x2, y2, z2), BooleanOp.OR));
      return result[0];
   }

   public void forAllEdges(final Shapes.DoubleLineConsumer consumer) {
      this.shape
         .forAllEdges(
            (xi1, yi1, zi1, xi2, yi2, zi2) -> consumer.consume(
               this.get(Direction.Axis.X, xi1),
               this.get(Direction.Axis.Y, yi1),
               this.get(Direction.Axis.Z, zi1),
               this.get(Direction.Axis.X, xi2),
               this.get(Direction.Axis.Y, yi2),
               this.get(Direction.Axis.Z, zi2)
            ),
            true
         );
   }

   public void forAllBoxes(final Shapes.DoubleLineConsumer consumer) {
      DoubleList xCoords = this.getCoords(Direction.Axis.X);
      DoubleList yCoords = this.getCoords(Direction.Axis.Y);
      DoubleList zCoords = this.getCoords(Direction.Axis.Z);
      this.shape
         .forAllBoxes(
            (xi1, yi1, zi1, xi2, yi2, zi2) -> consumer.consume(
               xCoords.getDouble(xi1), yCoords.getDouble(yi1), zCoords.getDouble(zi1), xCoords.getDouble(xi2), yCoords.getDouble(yi2), zCoords.getDouble(zi2)
            ),
            true
         );
   }

   public List<AABB> toAabbs() {
      List<AABB> list = Lists.newArrayList();
      this.forAllBoxes((x1, y1, z1, x2, y2, z2) -> list.add(new AABB(x1, y1, z1, x2, y2, z2)));
      return list;
   }

   public double min(final Direction.Axis aAxis, final double b, final double c) {
      Direction.Axis bAxis = AxisCycle.FORWARD.cycle(aAxis);
      Direction.Axis cAxis = AxisCycle.BACKWARD.cycle(aAxis);
      int bi = this.findIndex(bAxis, b);
      int ci = this.findIndex(cAxis, c);
      int i = this.shape.firstFull(aAxis, bi, ci);
      return i >= this.shape.getSize(aAxis) ? Double.POSITIVE_INFINITY : this.get(aAxis, i);
   }

   public double max(final Direction.Axis aAxis, final double b, final double c) {
      Direction.Axis bAxis = AxisCycle.FORWARD.cycle(aAxis);
      Direction.Axis cAxis = AxisCycle.BACKWARD.cycle(aAxis);
      int bi = this.findIndex(bAxis, b);
      int ci = this.findIndex(cAxis, c);
      int i = this.shape.lastFull(aAxis, bi, ci);
      return i <= 0 ? Double.NEGATIVE_INFINITY : this.get(aAxis, i);
   }

   protected int findIndex(final Direction.Axis axis, final double coord) {
      return Mth.binarySearch(0, this.shape.getSize(axis) + 1, index -> coord < this.get(axis, index)) - 1;
   }

   public @Nullable BlockHitResult clip(final Vec3 from, final Vec3 to, final BlockPos pos) {
      if (this.isEmpty()) {
         return null;
      }

      Vec3 diff = to.subtract(from);
      if (diff.lengthSqr() < 1.0E-7) {
         return null;
      }

      Vec3 testPoint = from.add(diff.scale(0.001));
      return this.shape
            .isFullWide(
               this.findIndex(Direction.Axis.X, testPoint.x - pos.getX()),
               this.findIndex(Direction.Axis.Y, testPoint.y - pos.getY()),
               this.findIndex(Direction.Axis.Z, testPoint.z - pos.getZ())
            )
         ? new BlockHitResult(testPoint, Direction.getApproximateNearest(diff.x, diff.y, diff.z).getOpposite(), pos, true)
         : AABB.clip(this.toAabbs(), from, to, pos);
   }

   public Optional<Vec3> closestPointTo(final Vec3 point) {
      if (this.isEmpty()) {
         return Optional.empty();
      }

      MutableObject<Vec3> closest = new MutableObject();
      this.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
         double x = Mth.clamp(point.x(), x1, x2);
         double y = Mth.clamp(point.y(), y1, y2);
         double z = Mth.clamp(point.z(), z1, z2);
         Vec3 currentClosest = (Vec3)closest.get();
         if (currentClosest == null || point.distanceToSqr(x, y, z) < point.distanceToSqr(currentClosest)) {
            closest.setValue(new Vec3(x, y, z));
         }
      });
      return Optional.of(Objects.requireNonNull((Vec3)closest.get()));
   }

   public VoxelShape getFaceShape(final Direction direction) {
      if (!this.isEmpty() && this != Shapes.block()) {
         if (this.faces != null) {
            VoxelShape face = this.faces[direction.ordinal()];
            if (face != null) {
               return face;
            }
         } else {
            this.faces = new VoxelShape[6];
         }

         VoxelShape face = this.calculateFace(direction);
         this.faces[direction.ordinal()] = face;
         return face;
      } else {
         return this;
      }
   }

   private VoxelShape calculateFace(final Direction direction) {
      Direction.Axis axis = direction.getAxis();
      if (this.isCubeLikeAlong(axis)) {
         return this;
      } else {
         Direction.AxisDirection sign = direction.getAxisDirection();
         int index = this.findIndex(axis, sign == Direction.AxisDirection.POSITIVE ? 0.9999999 : 1.0E-7);
         SliceShape slice = new SliceShape(this, axis, index);
         if (slice.isEmpty()) {
            return Shapes.empty();
         } else {
            return slice.isCubeLike() ? Shapes.block() : slice;
         }
      }
   }

   protected boolean isCubeLike() {
      for (Direction.Axis axis : Direction.Axis.VALUES) {
         if (!this.isCubeLikeAlong(axis)) {
            return false;
         }
      }

      return true;
   }

   private boolean isCubeLikeAlong(final Direction.Axis axis) {
      DoubleList coords = this.getCoords(axis);
      return coords.size() == 2 && DoubleMath.fuzzyEquals(coords.getDouble(0), 0.0, 1.0E-7) && DoubleMath.fuzzyEquals(coords.getDouble(1), 1.0, 1.0E-7);
   }

   public double collide(final Direction.Axis axis, final AABB moving, final double distance) {
      return this.collideX(AxisCycle.between(axis, Direction.Axis.X), moving, distance);
   }

   protected double collideX(final AxisCycle transform, final AABB moving, double distance) {
      if (this.isEmpty()) {
         return distance;
      }

      if (Math.abs(distance) < 1.0E-7) {
         return 0.0;
      }

      AxisCycle inverse = transform.inverse();
      Direction.Axis aAxis = inverse.cycle(Direction.Axis.X);
      Direction.Axis bAxis = inverse.cycle(Direction.Axis.Y);
      Direction.Axis cAxis = inverse.cycle(Direction.Axis.Z);
      double maxA = moving.max(aAxis);
      double minA = moving.min(aAxis);
      int aMin = this.findIndex(aAxis, minA + 1.0E-7);
      int aMax = this.findIndex(aAxis, maxA - 1.0E-7);
      int bMin = Math.max(0, this.findIndex(bAxis, moving.min(bAxis) + 1.0E-7));
      int bMax = Math.min(this.shape.getSize(bAxis), this.findIndex(bAxis, moving.max(bAxis) - 1.0E-7) + 1);
      int cMin = Math.max(0, this.findIndex(cAxis, moving.min(cAxis) + 1.0E-7));
      int cMax = Math.min(this.shape.getSize(cAxis), this.findIndex(cAxis, moving.max(cAxis) - 1.0E-7) + 1);
      int aSize = this.shape.getSize(aAxis);
      if (distance > 0.0) {
         for (int a = aMax + 1; a < aSize; a++) {
            for (int b = bMin; b < bMax; b++) {
               for (int c = cMin; c < cMax; c++) {
                  if (this.shape.isFullWide(inverse, a, b, c)) {
                     double newDistance = this.get(aAxis, a) - maxA;
                     if (newDistance >= -1.0E-7) {
                        distance = Math.min(distance, newDistance);
                     }

                     return distance;
                  }
               }
            }
         }
      } else if (distance < 0.0) {
         for (int a = aMin - 1; a >= 0; a--) {
            for (int b = bMin; b < bMax; b++) {
               for (int c = cMin; c < cMax; c++) {
                  if (this.shape.isFullWide(inverse, a, b, c)) {
                     double newDistance = this.get(aAxis, a + 1) - minA;
                     if (newDistance <= 1.0E-7) {
                        distance = Math.max(distance, newDistance);
                     }

                     return distance;
                  }
               }
            }
         }
      }

      return distance;
   }

   @Override
   public boolean equals(final Object obj) {
      return super.equals(obj);
   }

   @Override
   public String toString() {
      return this.isEmpty() ? "EMPTY" : "VoxelShape[" + this.bounds() + "]";
   }
}
