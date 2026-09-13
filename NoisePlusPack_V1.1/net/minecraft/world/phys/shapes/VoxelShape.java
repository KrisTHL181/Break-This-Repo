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

   protected VoxelShape(DiscreteVoxelShape p_83214_) {
      this.shape = p_83214_;
   }

   public double min(Direction.Axis p_83289_) {
      int i = this.shape.firstFull(p_83289_);
      return i >= this.shape.getSize(p_83289_) ? Double.POSITIVE_INFINITY : this.get(p_83289_, i);
   }

   public double max(Direction.Axis p_83298_) {
      int i = this.shape.lastFull(p_83298_);
      return i <= 0 ? Double.NEGATIVE_INFINITY : this.get(p_83298_, i);
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

   protected double get(Direction.Axis p_83257_, int p_83258_) {
      return this.getCoords(p_83257_).getDouble(p_83258_);
   }

   public abstract DoubleList getCoords(Direction.Axis var1);

   public boolean isEmpty() {
      return this.shape.isEmpty();
   }

   public VoxelShape move(Vec3 p_366733_) {
      return this.move(p_366733_.x, p_366733_.y, p_366733_.z);
   }

   public VoxelShape move(Vec3i p_395919_) {
      return this.move(p_395919_.getX(), p_395919_.getY(), p_395919_.getZ());
   }

   public VoxelShape move(double p_83217_, double p_83218_, double p_83219_) {
      return this.isEmpty()
         ? Shapes.empty()
         : new ArrayVoxelShape(
            this.shape,
            new OffsetDoubleList(this.getCoords(Direction.Axis.X), p_83217_),
            new OffsetDoubleList(this.getCoords(Direction.Axis.Y), p_83218_),
            new OffsetDoubleList(this.getCoords(Direction.Axis.Z), p_83219_)
         );
   }

   public VoxelShape optimize() {
      VoxelShape[] avoxelshape = new VoxelShape[]{Shapes.empty()};
      this.forAllBoxes(
         (p_83275_, p_83276_, p_83277_, p_83278_, p_83279_, p_83280_) -> avoxelshape[0] = Shapes.joinUnoptimized(
            avoxelshape[0], Shapes.box(p_83275_, p_83276_, p_83277_, p_83278_, p_83279_, p_83280_), BooleanOp.OR
         )
      );
      return avoxelshape[0];
   }

   public void forAllEdges(Shapes.DoubleLineConsumer p_83225_) {
      this.shape
         .forAllEdges(
            (p_83228_, p_83229_, p_83230_, p_83231_, p_83232_, p_83233_) -> p_83225_.consume(
               this.get(Direction.Axis.X, p_83228_),
               this.get(Direction.Axis.Y, p_83229_),
               this.get(Direction.Axis.Z, p_83230_),
               this.get(Direction.Axis.X, p_83231_),
               this.get(Direction.Axis.Y, p_83232_),
               this.get(Direction.Axis.Z, p_83233_)
            ),
            true
         );
   }

   public void forAllBoxes(Shapes.DoubleLineConsumer p_83287_) {
      DoubleList doublelist = this.getCoords(Direction.Axis.X);
      DoubleList doublelist1 = this.getCoords(Direction.Axis.Y);
      DoubleList doublelist2 = this.getCoords(Direction.Axis.Z);
      this.shape
         .forAllBoxes(
            (p_83239_, p_83240_, p_83241_, p_83242_, p_83243_, p_83244_) -> p_83287_.consume(
               doublelist.getDouble(p_83239_),
               doublelist1.getDouble(p_83240_),
               doublelist2.getDouble(p_83241_),
               doublelist.getDouble(p_83242_),
               doublelist1.getDouble(p_83243_),
               doublelist2.getDouble(p_83244_)
            ),
            true
         );
   }

   public List<AABB> toAabbs() {
      List<AABB> list = Lists.newArrayList();
      this.forAllBoxes(
         (p_83267_, p_83268_, p_83269_, p_83270_, p_83271_, p_83272_) -> list.add(new AABB(p_83267_, p_83268_, p_83269_, p_83270_, p_83271_, p_83272_))
      );
      return list;
   }

   public double min(Direction.Axis p_166079_, double p_166080_, double p_166081_) {
      Direction.Axis direction$axis = AxisCycle.FORWARD.cycle(p_166079_);
      Direction.Axis direction$axis1 = AxisCycle.BACKWARD.cycle(p_166079_);
      int i = this.findIndex(direction$axis, p_166080_);
      int j = this.findIndex(direction$axis1, p_166081_);
      int k = this.shape.firstFull(p_166079_, i, j);
      return k >= this.shape.getSize(p_166079_) ? Double.POSITIVE_INFINITY : this.get(p_166079_, k);
   }

   public double max(Direction.Axis p_83291_, double p_83292_, double p_83293_) {
      Direction.Axis direction$axis = AxisCycle.FORWARD.cycle(p_83291_);
      Direction.Axis direction$axis1 = AxisCycle.BACKWARD.cycle(p_83291_);
      int i = this.findIndex(direction$axis, p_83292_);
      int j = this.findIndex(direction$axis1, p_83293_);
      int k = this.shape.lastFull(p_83291_, i, j);
      return k <= 0 ? Double.NEGATIVE_INFINITY : this.get(p_83291_, k);
   }

   protected int findIndex(Direction.Axis p_83250_, double p_83251_) {
      return Mth.binarySearch(0, this.shape.getSize(p_83250_) + 1, p_166066_ -> p_83251_ < this.get(p_83250_, p_166066_)) - 1;
   }

   public @Nullable BlockHitResult clip(Vec3 p_83221_, Vec3 p_83222_, BlockPos p_83223_) {
      if (this.isEmpty()) {
         return null;
      }

      Vec3 vec3 = p_83222_.subtract(p_83221_);
      if (vec3.lengthSqr() < 1.0E-7) {
         return null;
      }

      Vec3 vec31 = p_83221_.add(vec3.scale(0.001));
      return this.shape
            .isFullWide(
               this.findIndex(Direction.Axis.X, vec31.x - p_83223_.getX()),
               this.findIndex(Direction.Axis.Y, vec31.y - p_83223_.getY()),
               this.findIndex(Direction.Axis.Z, vec31.z - p_83223_.getZ())
            )
         ? new BlockHitResult(vec31, Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z).getOpposite(), p_83223_, true)
         : AABB.clip(this.toAabbs(), p_83221_, p_83222_, p_83223_);
   }

   public Optional<Vec3> closestPointTo(Vec3 p_166068_) {
      if (this.isEmpty()) {
         return Optional.empty();
      }

      MutableObject<Vec3> mutableobject = new MutableObject();
      this.forAllBoxes((p_450128_, p_450129_, p_450130_, p_450131_, p_450132_, p_450133_) -> {
         double d0 = Mth.clamp(p_166068_.x(), p_450128_, p_450131_);
         double d1 = Mth.clamp(p_166068_.y(), p_450129_, p_450132_);
         double d2 = Mth.clamp(p_166068_.z(), p_450130_, p_450133_);
         Vec3 vec3 = (Vec3)mutableobject.get();
         if (vec3 == null || p_166068_.distanceToSqr(d0, d1, d2) < p_166068_.distanceToSqr(vec3)) {
            mutableobject.setValue(new Vec3(d0, d1, d2));
         }
      });
      return Optional.of(Objects.requireNonNull((Vec3)mutableobject.get()));
   }

   public VoxelShape getFaceShape(Direction p_83264_) {
      if (!this.isEmpty() && this != Shapes.block()) {
         if (this.faces != null) {
            VoxelShape voxelshape = this.faces[p_83264_.ordinal()];
            if (voxelshape != null) {
               return voxelshape;
            }
         } else {
            this.faces = new VoxelShape[6];
         }

         VoxelShape voxelshape1 = this.calculateFace(p_83264_);
         this.faces[p_83264_.ordinal()] = voxelshape1;
         return voxelshape1;
      } else {
         return this;
      }
   }

   private VoxelShape calculateFace(Direction p_83295_) {
      Direction.Axis direction$axis = p_83295_.getAxis();
      if (this.isCubeLikeAlong(direction$axis)) {
         return this;
      } else {
         Direction.AxisDirection direction$axisdirection = p_83295_.getAxisDirection();
         int i = this.findIndex(direction$axis, direction$axisdirection == Direction.AxisDirection.POSITIVE ? 0.9999999 : 1.0E-7);
         SliceShape sliceshape = new SliceShape(this, direction$axis, i);
         if (sliceshape.isEmpty()) {
            return Shapes.empty();
         } else {
            return sliceshape.isCubeLike() ? Shapes.block() : sliceshape;
         }
      }
   }

   protected boolean isCubeLike() {
      for (Direction.Axis direction$axis : Direction.Axis.VALUES) {
         if (!this.isCubeLikeAlong(direction$axis)) {
            return false;
         }
      }

      return true;
   }

   private boolean isCubeLikeAlong(Direction.Axis p_363919_) {
      DoubleList doublelist = this.getCoords(p_363919_);
      return doublelist.size() == 2
         && DoubleMath.fuzzyEquals(doublelist.getDouble(0), 0.0, 1.0E-7)
         && DoubleMath.fuzzyEquals(doublelist.getDouble(1), 1.0, 1.0E-7);
   }

   public double collide(Direction.Axis p_83260_, AABB p_83261_, double p_83262_) {
      return this.collideX(AxisCycle.between(p_83260_, Direction.Axis.X), p_83261_, p_83262_);
   }

   protected double collideX(AxisCycle p_83246_, AABB p_83247_, double p_83248_) {
      if (this.isEmpty()) {
         return p_83248_;
      }

      if (Math.abs(p_83248_) < 1.0E-7) {
         return 0.0;
      }

      AxisCycle axiscycle = p_83246_.inverse();
      Direction.Axis direction$axis = axiscycle.cycle(Direction.Axis.X);
      Direction.Axis direction$axis1 = axiscycle.cycle(Direction.Axis.Y);
      Direction.Axis direction$axis2 = axiscycle.cycle(Direction.Axis.Z);
      double d0 = p_83247_.max(direction$axis);
      double d1 = p_83247_.min(direction$axis);
      int i = this.findIndex(direction$axis, d1 + 1.0E-7);
      int j = this.findIndex(direction$axis, d0 - 1.0E-7);
      int k = Math.max(0, this.findIndex(direction$axis1, p_83247_.min(direction$axis1) + 1.0E-7));
      int l = Math.min(this.shape.getSize(direction$axis1), this.findIndex(direction$axis1, p_83247_.max(direction$axis1) - 1.0E-7) + 1);
      int i1 = Math.max(0, this.findIndex(direction$axis2, p_83247_.min(direction$axis2) + 1.0E-7));
      int j1 = Math.min(this.shape.getSize(direction$axis2), this.findIndex(direction$axis2, p_83247_.max(direction$axis2) - 1.0E-7) + 1);
      int k1 = this.shape.getSize(direction$axis);
      if (p_83248_ > 0.0) {
         for (int l1 = j + 1; l1 < k1; l1++) {
            for (int i2 = k; i2 < l; i2++) {
               for (int j2 = i1; j2 < j1; j2++) {
                  if (this.shape.isFullWide(axiscycle, l1, i2, j2)) {
                     double d2 = this.get(direction$axis, l1) - d0;
                     if (d2 >= -1.0E-7) {
                        p_83248_ = Math.min(p_83248_, d2);
                     }

                     return p_83248_;
                  }
               }
            }
         }
      } else if (p_83248_ < 0.0) {
         for (int k2 = i - 1; k2 >= 0; k2--) {
            for (int l2 = k; l2 < l; l2++) {
               for (int i3 = i1; i3 < j1; i3++) {
                  if (this.shape.isFullWide(axiscycle, k2, l2, i3)) {
                     double d3 = this.get(direction$axis, k2 + 1) - d1;
                     if (d3 <= 1.0E-7) {
                        p_83248_ = Math.max(p_83248_, d3);
                     }

                     return p_83248_;
                  }
               }
            }
         }
      }

      return p_83248_;
   }

   @Override
   public boolean equals(Object p_394599_) {
      return super.equals(p_394599_);
   }

   @Override
   public String toString() {
      return this.isEmpty() ? "EMPTY" : "VoxelShape[" + this.bounds() + "]";
   }
}
