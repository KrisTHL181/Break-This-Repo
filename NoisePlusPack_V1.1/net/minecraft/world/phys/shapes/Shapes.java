package net.minecraft.world.phys.shapes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import com.mojang.math.OctahedralGroup;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Shapes {
   public static final double EPSILON = 1.0E-7;
   public static final double BIG_EPSILON = 1.0E-6;
   private static final VoxelShape BLOCK = Util.make(() -> {
      DiscreteVoxelShape discretevoxelshape = new BitSetDiscreteVoxelShape(1, 1, 1);
      discretevoxelshape.fill(0, 0, 0);
      return new CubeVoxelShape(discretevoxelshape);
   });
   private static final Vec3 BLOCK_CENTER = new Vec3(0.5, 0.5, 0.5);
   public static final VoxelShape INFINITY = box(
      Double.NEGATIVE_INFINITY,
      Double.NEGATIVE_INFINITY,
      Double.NEGATIVE_INFINITY,
      Double.POSITIVE_INFINITY,
      Double.POSITIVE_INFINITY,
      Double.POSITIVE_INFINITY
   );
   private static final VoxelShape EMPTY = new ArrayVoxelShape(
      new BitSetDiscreteVoxelShape(0, 0, 0),
      new DoubleArrayList(new double[]{0.0}),
      new DoubleArrayList(new double[]{0.0}),
      new DoubleArrayList(new double[]{0.0})
   );

   public static VoxelShape empty() {
      return EMPTY;
   }

   public static VoxelShape block() {
      return BLOCK;
   }

   public static VoxelShape box(double p_83049_, double p_83050_, double p_83051_, double p_83052_, double p_83053_, double p_83054_) {
      if (!(p_83049_ > p_83052_) && !(p_83050_ > p_83053_) && !(p_83051_ > p_83054_)) {
         return create(p_83049_, p_83050_, p_83051_, p_83052_, p_83053_, p_83054_);
      } else {
         throw new IllegalArgumentException("The min values need to be smaller or equals to the max values");
      }
   }

   public static VoxelShape create(double p_166050_, double p_166051_, double p_166052_, double p_166053_, double p_166054_, double p_166055_) {
      if (!(p_166053_ - p_166050_ < 1.0E-7) && !(p_166054_ - p_166051_ < 1.0E-7) && !(p_166055_ - p_166052_ < 1.0E-7)) {
         int i = findBits(p_166050_, p_166053_);
         int j = findBits(p_166051_, p_166054_);
         int k = findBits(p_166052_, p_166055_);
         if (i < 0 || j < 0 || k < 0) {
            return new ArrayVoxelShape(
               BLOCK.shape,
               DoubleArrayList.wrap(new double[]{p_166050_, p_166053_}),
               DoubleArrayList.wrap(new double[]{p_166051_, p_166054_}),
               DoubleArrayList.wrap(new double[]{p_166052_, p_166055_})
            );
         }

         if (i == 0 && j == 0 && k == 0) {
            return block();
         }

         int l = 1 << i;
         int i1 = 1 << j;
         int j1 = 1 << k;
         BitSetDiscreteVoxelShape bitsetdiscretevoxelshape = BitSetDiscreteVoxelShape.withFilledBounds(
            l,
            i1,
            j1,
            (int)Math.round(p_166050_ * l),
            (int)Math.round(p_166051_ * i1),
            (int)Math.round(p_166052_ * j1),
            (int)Math.round(p_166053_ * l),
            (int)Math.round(p_166054_ * i1),
            (int)Math.round(p_166055_ * j1)
         );
         return new CubeVoxelShape(bitsetdiscretevoxelshape);
      } else {
         return empty();
      }
   }

   public static VoxelShape create(AABB p_83065_) {
      return create(p_83065_.minX, p_83065_.minY, p_83065_.minZ, p_83065_.maxX, p_83065_.maxY, p_83065_.maxZ);
   }

   @VisibleForTesting
   protected static int findBits(double p_83042_, double p_83043_) {
      if (!(p_83042_ < -1.0E-7) && !(p_83043_ > 1.0000001)) {
         for (int i = 0; i <= 3; i++) {
            int j = 1 << i;
            double d0 = p_83042_ * j;
            double d1 = p_83043_ * j;
            boolean flag = Math.abs(d0 - Math.round(d0)) < 1.0E-7 * j;
            boolean flag1 = Math.abs(d1 - Math.round(d1)) < 1.0E-7 * j;
            if (flag && flag1) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   protected static long lcm(int p_83056_, int p_83057_) {
      return (long)p_83056_ * (p_83057_ / IntMath.gcd(p_83056_, p_83057_));
   }

   public static VoxelShape or(VoxelShape p_83111_, VoxelShape p_83112_) {
      return join(p_83111_, p_83112_, BooleanOp.OR);
   }

   public static VoxelShape or(VoxelShape p_83125_, VoxelShape... p_83126_) {
      return Arrays.stream(p_83126_).reduce(p_83125_, Shapes::or);
   }

   public static VoxelShape join(VoxelShape p_83114_, VoxelShape p_83115_, BooleanOp p_83116_) {
      return joinUnoptimized(p_83114_, p_83115_, p_83116_).optimize();
   }

   public static VoxelShape joinUnoptimized(VoxelShape p_83149_, VoxelShape p_83150_, BooleanOp p_83151_) {
      if (p_83151_.apply(false, false)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
      }

      if (p_83149_ == p_83150_) {
         return p_83151_.apply(true, true) ? p_83149_ : empty();
      }

      boolean flag = p_83151_.apply(true, false);
      boolean flag1 = p_83151_.apply(false, true);
      if (p_83149_.isEmpty()) {
         return flag1 ? p_83150_ : empty();
      }

      if (p_83150_.isEmpty()) {
         return flag ? p_83149_ : empty();
      }

      IndexMerger indexmerger = createIndexMerger(1, p_83149_.getCoords(Direction.Axis.X), p_83150_.getCoords(Direction.Axis.X), flag, flag1);
      IndexMerger indexmerger1 = createIndexMerger(
         indexmerger.size() - 1, p_83149_.getCoords(Direction.Axis.Y), p_83150_.getCoords(Direction.Axis.Y), flag, flag1
      );
      IndexMerger indexmerger2 = createIndexMerger(
         (indexmerger.size() - 1) * (indexmerger1.size() - 1), p_83149_.getCoords(Direction.Axis.Z), p_83150_.getCoords(Direction.Axis.Z), flag, flag1
      );
      BitSetDiscreteVoxelShape bitsetdiscretevoxelshape = BitSetDiscreteVoxelShape.join(
         p_83149_.shape, p_83150_.shape, indexmerger, indexmerger1, indexmerger2, p_83151_
      );
      return indexmerger instanceof DiscreteCubeMerger && indexmerger1 instanceof DiscreteCubeMerger && indexmerger2 instanceof DiscreteCubeMerger
         ? new CubeVoxelShape(bitsetdiscretevoxelshape)
         : new ArrayVoxelShape(bitsetdiscretevoxelshape, indexmerger.getList(), indexmerger1.getList(), indexmerger2.getList());
   }

   public static boolean joinIsNotEmpty(VoxelShape p_83158_, VoxelShape p_83159_, BooleanOp p_83160_) {
      if (p_83160_.apply(false, false)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
      }

      boolean flag = p_83158_.isEmpty();
      boolean flag1 = p_83159_.isEmpty();
      if (!flag && !flag1) {
         if (p_83158_ == p_83159_) {
            return p_83160_.apply(true, true);
         }

         boolean flag2 = p_83160_.apply(true, false);
         boolean flag3 = p_83160_.apply(false, true);

         for (Direction.Axis direction$axis : AxisCycle.AXIS_VALUES) {
            if (p_83158_.max(direction$axis) < p_83159_.min(direction$axis) - 1.0E-7) {
               return flag2 || flag3;
            }

            if (p_83159_.max(direction$axis) < p_83158_.min(direction$axis) - 1.0E-7) {
               return flag2 || flag3;
            }
         }

         IndexMerger indexmerger = createIndexMerger(1, p_83158_.getCoords(Direction.Axis.X), p_83159_.getCoords(Direction.Axis.X), flag2, flag3);
         IndexMerger indexmerger1 = createIndexMerger(
            indexmerger.size() - 1, p_83158_.getCoords(Direction.Axis.Y), p_83159_.getCoords(Direction.Axis.Y), flag2, flag3
         );
         IndexMerger indexmerger2 = createIndexMerger(
            (indexmerger.size() - 1) * (indexmerger1.size() - 1), p_83158_.getCoords(Direction.Axis.Z), p_83159_.getCoords(Direction.Axis.Z), flag2, flag3
         );
         return joinIsNotEmpty(indexmerger, indexmerger1, indexmerger2, p_83158_.shape, p_83159_.shape, p_83160_);
      } else {
         return p_83160_.apply(!flag, !flag1);
      }
   }

   private static boolean joinIsNotEmpty(
      IndexMerger p_83104_, IndexMerger p_83105_, IndexMerger p_83106_, DiscreteVoxelShape p_83107_, DiscreteVoxelShape p_83108_, BooleanOp p_83109_
   ) {
      return !p_83104_.forMergedIndexes(
         (p_83100_, p_83101_, p_83102_) -> p_83105_.forMergedIndexes(
            (p_166046_, p_166047_, p_166048_) -> p_83106_.forMergedIndexes(
               (p_166036_, p_166037_, p_166038_) -> !p_83109_.apply(
                  p_83107_.isFullWide(p_83100_, p_166046_, p_166036_), p_83108_.isFullWide(p_83101_, p_166047_, p_166037_)
               )
            )
         )
      );
   }

   public static double collide(Direction.Axis p_193136_, AABB p_193137_, Iterable<VoxelShape> p_193138_, double p_193139_) {
      for (VoxelShape voxelshape : p_193138_) {
         if (Math.abs(p_193139_) < 1.0E-7) {
            return 0.0;
         }

         p_193139_ = voxelshape.collide(p_193136_, p_193137_, p_193139_);
      }

      return p_193139_;
   }

   public static boolean blockOccludes(VoxelShape p_83118_, VoxelShape p_83119_, Direction p_83120_) {
      if (p_83118_ == block() && p_83119_ == block()) {
         return true;
      }

      if (p_83119_.isEmpty()) {
         return false;
      }

      Direction.Axis direction$axis = p_83120_.getAxis();
      Direction.AxisDirection direction$axisdirection = p_83120_.getAxisDirection();
      VoxelShape voxelshape = direction$axisdirection == Direction.AxisDirection.POSITIVE ? p_83118_ : p_83119_;
      VoxelShape voxelshape1 = direction$axisdirection == Direction.AxisDirection.POSITIVE ? p_83119_ : p_83118_;
      BooleanOp booleanop = direction$axisdirection == Direction.AxisDirection.POSITIVE ? BooleanOp.ONLY_FIRST : BooleanOp.ONLY_SECOND;
      return DoubleMath.fuzzyEquals(voxelshape.max(direction$axis), 1.0, 1.0E-7)
         && DoubleMath.fuzzyEquals(voxelshape1.min(direction$axis), 0.0, 1.0E-7)
         && !joinIsNotEmpty(
            new SliceShape(voxelshape, direction$axis, voxelshape.shape.getSize(direction$axis) - 1), new SliceShape(voxelshape1, direction$axis, 0), booleanop
         );
   }

   public static boolean mergedFaceOccludes(VoxelShape p_83153_, VoxelShape p_83154_, Direction p_83155_) {
      if (p_83153_ != block() && p_83154_ != block()) {
         Direction.Axis direction$axis = p_83155_.getAxis();
         Direction.AxisDirection direction$axisdirection = p_83155_.getAxisDirection();
         VoxelShape voxelshape = direction$axisdirection == Direction.AxisDirection.POSITIVE ? p_83153_ : p_83154_;
         VoxelShape voxelshape1 = direction$axisdirection == Direction.AxisDirection.POSITIVE ? p_83154_ : p_83153_;
         if (!DoubleMath.fuzzyEquals(voxelshape.max(direction$axis), 1.0, 1.0E-7)) {
            voxelshape = empty();
         }

         if (!DoubleMath.fuzzyEquals(voxelshape1.min(direction$axis), 0.0, 1.0E-7)) {
            voxelshape1 = empty();
         }

         return !joinIsNotEmpty(
            block(),
            joinUnoptimized(
               new SliceShape(voxelshape, direction$axis, voxelshape.shape.getSize(direction$axis) - 1),
               new SliceShape(voxelshape1, direction$axis, 0),
               BooleanOp.OR
            ),
            BooleanOp.ONLY_FIRST
         );
      } else {
         return true;
      }
   }

   public static boolean faceShapeOccludes(VoxelShape p_83146_, VoxelShape p_83147_) {
      if (p_83146_ != block() && p_83147_ != block()) {
         return p_83146_.isEmpty() && p_83147_.isEmpty()
            ? false
            : !joinIsNotEmpty(block(), joinUnoptimized(p_83146_, p_83147_, BooleanOp.OR), BooleanOp.ONLY_FIRST);
      } else {
         return true;
      }
   }

   @VisibleForTesting
   protected static IndexMerger createIndexMerger(int p_83059_, DoubleList p_83060_, DoubleList p_83061_, boolean p_83062_, boolean p_83063_) {
      int i = p_83060_.size() - 1;
      int j = p_83061_.size() - 1;
      if (p_83060_ instanceof CubePointRange && p_83061_ instanceof CubePointRange) {
         long k = lcm(i, j);
         if (p_83059_ * k <= 256L) {
            return new DiscreteCubeMerger(i, j);
         }
      }

      if (p_83060_.getDouble(i) < p_83061_.getDouble(0) - 1.0E-7) {
         return new NonOverlappingMerger(p_83060_, p_83061_, false);
      } else if (p_83061_.getDouble(j) < p_83060_.getDouble(0) - 1.0E-7) {
         return new NonOverlappingMerger(p_83061_, p_83060_, true);
      } else {
         return i == j && Objects.equals(p_83060_, p_83061_) ? new IdenticalMerger(p_83060_) : new IndirectMerger(p_83060_, p_83061_, p_83062_, p_83063_);
      }
   }

   public static VoxelShape rotate(VoxelShape p_392699_, OctahedralGroup p_396102_) {
      return rotate(p_392699_, p_396102_, BLOCK_CENTER);
   }

   public static VoxelShape rotate(VoxelShape p_392970_, OctahedralGroup p_392838_, Vec3 p_394301_) {
      if (p_392838_ == OctahedralGroup.IDENTITY) {
         return p_392970_;
      }

      DiscreteVoxelShape discretevoxelshape = p_392970_.shape.rotate(p_392838_);
      if (p_392970_ instanceof CubeVoxelShape && BLOCK_CENTER.equals(p_394301_)) {
         return new CubeVoxelShape(discretevoxelshape);
      }

      Direction.Axis direction$axis = p_392838_.permutation().permuteAxis(Direction.Axis.X);
      Direction.Axis direction$axis1 = p_392838_.permutation().permuteAxis(Direction.Axis.Y);
      Direction.Axis direction$axis2 = p_392838_.permutation().permuteAxis(Direction.Axis.Z);
      DoubleList doublelist = p_392970_.getCoords(direction$axis);
      DoubleList doublelist1 = p_392970_.getCoords(direction$axis1);
      DoubleList doublelist2 = p_392970_.getCoords(direction$axis2);
      boolean flag = p_392838_.inverts(Direction.Axis.X);
      boolean flag1 = p_392838_.inverts(Direction.Axis.Y);
      boolean flag2 = p_392838_.inverts(Direction.Axis.Z);
      return new ArrayVoxelShape(
         discretevoxelshape,
         flipAxisIfNeeded(doublelist, flag, p_394301_.get(direction$axis), p_394301_.x),
         flipAxisIfNeeded(doublelist1, flag1, p_394301_.get(direction$axis1), p_394301_.y),
         flipAxisIfNeeded(doublelist2, flag2, p_394301_.get(direction$axis2), p_394301_.z)
      );
   }

   @VisibleForTesting
   static DoubleList flipAxisIfNeeded(DoubleList p_455981_, boolean p_451971_, double p_456522_, double p_451917_) {
      if (!p_451971_ && p_456522_ == p_451917_) {
         return p_455981_;
      }

      int i = p_455981_.size();
      DoubleList doublelist = new DoubleArrayList(i);
      if (p_451971_) {
         for (int j = i - 1; j >= 0; j--) {
            doublelist.add(-(p_455981_.getDouble(j) - p_456522_) + p_451917_);
         }
      } else {
         for (int k = 0; k >= 0 && k < i; k++) {
            doublelist.add(p_455981_.getDouble(k) - p_456522_ + p_451917_);
         }
      }

      return doublelist;
   }

   public static boolean equal(VoxelShape p_397633_, VoxelShape p_393144_) {
      return !joinIsNotEmpty(p_397633_, p_393144_, BooleanOp.NOT_SAME);
   }

   public static Map<Direction.Axis, VoxelShape> rotateHorizontalAxis(VoxelShape p_393602_) {
      return rotateHorizontalAxis(p_393602_, BLOCK_CENTER);
   }

   public static Map<Direction.Axis, VoxelShape> rotateHorizontalAxis(VoxelShape p_392909_, Vec3 p_395722_) {
      return Maps.newEnumMap(Map.of(Direction.Axis.Z, p_392909_, Direction.Axis.X, rotate(p_392909_, OctahedralGroup.BLOCK_ROT_Y_90, p_395722_)));
   }

   public static Map<Direction.Axis, VoxelShape> rotateAllAxis(VoxelShape p_393929_) {
      return rotateAllAxis(p_393929_, BLOCK_CENTER);
   }

   public static Map<Direction.Axis, VoxelShape> rotateAllAxis(VoxelShape p_395366_, Vec3 p_391541_) {
      return Maps.newEnumMap(
         Map.of(
            Direction.Axis.Z,
            p_395366_,
            Direction.Axis.X,
            rotate(p_395366_, OctahedralGroup.BLOCK_ROT_Y_90, p_391541_),
            Direction.Axis.Y,
            rotate(p_395366_, OctahedralGroup.BLOCK_ROT_X_90, p_391541_)
         )
      );
   }

   public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape p_395124_) {
      return rotateHorizontal(p_395124_, OctahedralGroup.IDENTITY, BLOCK_CENTER);
   }

   public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape p_453128_, OctahedralGroup p_451561_) {
      return rotateHorizontal(p_453128_, p_451561_, BLOCK_CENTER);
   }

   public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape p_393025_, OctahedralGroup p_459433_, Vec3 p_394205_) {
      return Maps.newEnumMap(
         Map.of(
            Direction.NORTH,
            rotate(p_393025_, p_459433_),
            Direction.EAST,
            rotate(p_393025_, OctahedralGroup.BLOCK_ROT_Y_90.compose(p_459433_), p_394205_),
            Direction.SOUTH,
            rotate(p_393025_, OctahedralGroup.BLOCK_ROT_Y_180.compose(p_459433_), p_394205_),
            Direction.WEST,
            rotate(p_393025_, OctahedralGroup.BLOCK_ROT_Y_270.compose(p_459433_), p_394205_)
         )
      );
   }

   public static Map<Direction, VoxelShape> rotateAll(VoxelShape p_392088_) {
      return rotateAll(p_392088_, OctahedralGroup.IDENTITY, BLOCK_CENTER);
   }

   public static Map<Direction, VoxelShape> rotateAll(VoxelShape p_391678_, Vec3 p_391672_) {
      return rotateAll(p_391678_, OctahedralGroup.IDENTITY, p_391672_);
   }

   public static Map<Direction, VoxelShape> rotateAll(VoxelShape p_456212_, OctahedralGroup p_451296_, Vec3 p_458794_) {
      return Maps.newEnumMap(
         Map.of(
            Direction.NORTH,
            rotate(p_456212_, p_451296_),
            Direction.EAST,
            rotate(p_456212_, OctahedralGroup.BLOCK_ROT_Y_90.compose(p_451296_), p_458794_),
            Direction.SOUTH,
            rotate(p_456212_, OctahedralGroup.BLOCK_ROT_Y_180.compose(p_451296_), p_458794_),
            Direction.WEST,
            rotate(p_456212_, OctahedralGroup.BLOCK_ROT_Y_270.compose(p_451296_), p_458794_),
            Direction.UP,
            rotate(p_456212_, OctahedralGroup.BLOCK_ROT_X_270.compose(p_451296_), p_458794_),
            Direction.DOWN,
            rotate(p_456212_, OctahedralGroup.BLOCK_ROT_X_90.compose(p_451296_), p_458794_)
         )
      );
   }

   public static Map<AttachFace, Map<Direction, VoxelShape>> rotateAttachFace(VoxelShape p_394424_) {
      return rotateAttachFace(p_394424_, OctahedralGroup.IDENTITY);
   }

   public static Map<AttachFace, Map<Direction, VoxelShape>> rotateAttachFace(VoxelShape p_456665_, OctahedralGroup p_456958_) {
      return Map.of(
         AttachFace.WALL,
         rotateHorizontal(p_456665_, p_456958_),
         AttachFace.FLOOR,
         rotateHorizontal(p_456665_, OctahedralGroup.BLOCK_ROT_X_270.compose(p_456958_)),
         AttachFace.CEILING,
         rotateHorizontal(p_456665_, OctahedralGroup.BLOCK_ROT_Y_180.compose(OctahedralGroup.BLOCK_ROT_X_90).compose(p_456958_))
      );
   }

   public interface DoubleLineConsumer {
      void consume(double var1, double var3, double var5, double var7, double var9, double var11);
   }
}
