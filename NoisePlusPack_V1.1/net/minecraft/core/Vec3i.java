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
         p_448581_ -> Util.fixedSize(p_448581_, 3).map(p_175586_ -> new Vec3i(p_175586_[0], p_175586_[1], p_175586_[2])),
         p_123313_ -> IntStream.of(p_123313_.getX(), p_123313_.getY(), p_123313_.getZ())
      );
   public static final StreamCodec<ByteBuf, Vec3i> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, Vec3i::getX, ByteBufCodecs.VAR_INT, Vec3i::getY, ByteBufCodecs.VAR_INT, Vec3i::getZ, Vec3i::new
   );
   public static final Vec3i ZERO = new Vec3i(0, 0, 0);
   private int x;
   private int y;
   private int z;

   public static Codec<Vec3i> offsetCodec(int p_194651_) {
      return CODEC.validate(
         p_274739_ -> Math.abs(p_274739_.getX()) < p_194651_ && Math.abs(p_274739_.getY()) < p_194651_ && Math.abs(p_274739_.getZ()) < p_194651_
            ? DataResult.success(p_274739_)
            : DataResult.error(() -> "Position out of range, expected at most " + p_194651_ + ": " + p_274739_)
      );
   }

   public Vec3i(int p_123296_, int p_123297_, int p_123298_) {
      this.x = p_123296_;
      this.y = p_123297_;
      this.z = p_123298_;
   }

   @Override
   public boolean equals(Object p_123327_) {
      if (this == p_123327_) {
         return true;
      } else {
         return p_123327_ instanceof Vec3i vec3i ? this.getX() == vec3i.getX() && this.getY() == vec3i.getY() && this.getZ() == vec3i.getZ() : false;
      }
   }

   @Override
   public int hashCode() {
      return (this.getY() + this.getZ() * 31) * 31 + this.getX();
   }

   public int compareTo(Vec3i p_123330_) {
      if (this.getY() == p_123330_.getY()) {
         return this.getZ() == p_123330_.getZ() ? this.getX() - p_123330_.getX() : this.getZ() - p_123330_.getZ();
      } else {
         return this.getY() - p_123330_.getY();
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

   protected Vec3i setX(int p_175605_) {
      this.x = p_175605_;
      return this;
   }

   protected Vec3i setY(int p_175604_) {
      this.y = p_175604_;
      return this;
   }

   protected Vec3i setZ(int p_175603_) {
      this.z = p_175603_;
      return this;
   }

   public Vec3i offset(int p_175593_, int p_175594_, int p_175595_) {
      return p_175593_ == 0 && p_175594_ == 0 && p_175595_ == 0 ? this : new Vec3i(this.getX() + p_175593_, this.getY() + p_175594_, this.getZ() + p_175595_);
   }

   public Vec3i offset(Vec3i p_175597_) {
      return this.offset(p_175597_.getX(), p_175597_.getY(), p_175597_.getZ());
   }

   public Vec3i subtract(Vec3i p_175596_) {
      return this.offset(-p_175596_.getX(), -p_175596_.getY(), -p_175596_.getZ());
   }

   public Vec3i multiply(int p_175602_) {
      if (p_175602_ == 1) {
         return this;
      } else {
         return p_175602_ == 0 ? ZERO : new Vec3i(this.getX() * p_175602_, this.getY() * p_175602_, this.getZ() * p_175602_);
      }
   }

   public Vec3i multiply(int p_460014_, int p_453576_, int p_454975_) {
      return new Vec3i(this.getX() * p_460014_, this.getY() * p_453576_, this.getZ() * p_454975_);
   }

   public Vec3i above() {
      return this.above(1);
   }

   public Vec3i above(int p_123336_) {
      return this.relative(Direction.UP, p_123336_);
   }

   public Vec3i below() {
      return this.below(1);
   }

   public Vec3i below(int p_123335_) {
      return this.relative(Direction.DOWN, p_123335_);
   }

   public Vec3i north() {
      return this.north(1);
   }

   public Vec3i north(int p_175601_) {
      return this.relative(Direction.NORTH, p_175601_);
   }

   public Vec3i south() {
      return this.south(1);
   }

   public Vec3i south(int p_175600_) {
      return this.relative(Direction.SOUTH, p_175600_);
   }

   public Vec3i west() {
      return this.west(1);
   }

   public Vec3i west(int p_175599_) {
      return this.relative(Direction.WEST, p_175599_);
   }

   public Vec3i east() {
      return this.east(1);
   }

   public Vec3i east(int p_175598_) {
      return this.relative(Direction.EAST, p_175598_);
   }

   public Vec3i relative(Direction p_175592_) {
      return this.relative(p_175592_, 1);
   }

   public Vec3i relative(Direction p_123321_, int p_123322_) {
      return p_123322_ == 0
         ? this
         : new Vec3i(
            this.getX() + p_123321_.getStepX() * p_123322_, this.getY() + p_123321_.getStepY() * p_123322_, this.getZ() + p_123321_.getStepZ() * p_123322_
         );
   }

   public Vec3i relative(Direction.Axis p_175590_, int p_175591_) {
      if (p_175591_ == 0) {
         return this;
      }

      int i = p_175590_ == Direction.Axis.X ? p_175591_ : 0;
      int j = p_175590_ == Direction.Axis.Y ? p_175591_ : 0;
      int k = p_175590_ == Direction.Axis.Z ? p_175591_ : 0;
      return new Vec3i(this.getX() + i, this.getY() + j, this.getZ() + k);
   }

   public Vec3i cross(Vec3i p_123325_) {
      return new Vec3i(
         this.getY() * p_123325_.getZ() - this.getZ() * p_123325_.getY(),
         this.getZ() * p_123325_.getX() - this.getX() * p_123325_.getZ(),
         this.getX() * p_123325_.getY() - this.getY() * p_123325_.getX()
      );
   }

   public boolean closerThan(Vec3i p_123315_, double p_123316_) {
      return this.distSqr(p_123315_) < Mth.square(p_123316_);
   }

   public boolean closerToCenterThan(Position p_203196_, double p_203197_) {
      return this.distToCenterSqr(p_203196_) < Mth.square(p_203197_);
   }

   public double distSqr(Vec3i p_123332_) {
      return this.distToLowCornerSqr(p_123332_.getX(), p_123332_.getY(), p_123332_.getZ());
   }

   public double distToCenterSqr(Position p_203194_) {
      return this.distToCenterSqr(p_203194_.x(), p_203194_.y(), p_203194_.z());
   }

   public double distToCenterSqr(double p_203199_, double p_203200_, double p_203201_) {
      double d0 = this.getX() + 0.5 - p_203199_;
      double d1 = this.getY() + 0.5 - p_203200_;
      double d2 = this.getZ() + 0.5 - p_203201_;
      return d0 * d0 + d1 * d1 + d2 * d2;
   }

   public double distToLowCornerSqr(double p_203203_, double p_203204_, double p_203205_) {
      double d0 = this.getX() - p_203203_;
      double d1 = this.getY() - p_203204_;
      double d2 = this.getZ() - p_203205_;
      return d0 * d0 + d1 * d1 + d2 * d2;
   }

   public int distManhattan(Vec3i p_123334_) {
      float f = Math.abs(p_123334_.getX() - this.getX());
      float f1 = Math.abs(p_123334_.getY() - this.getY());
      float f2 = Math.abs(p_123334_.getZ() - this.getZ());
      return (int)(f + f1 + f2);
   }

   public int distChessboard(Vec3i p_367966_) {
      int i = Math.abs(this.getX() - p_367966_.getX());
      int j = Math.abs(this.getY() - p_367966_.getY());
      int k = Math.abs(this.getZ() - p_367966_.getZ());
      return Math.max(Math.max(i, j), k);
   }

   public int get(Direction.Axis p_123305_) {
      return p_123305_.choose(this.x, this.y, this.z);
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
