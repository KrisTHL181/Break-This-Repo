package net.minecraft.world.phys;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
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
         p_450125_ -> Util.fixedSize(p_450125_, 3).map(p_231081_ -> new Vec3((Double)p_231081_.get(0), (Double)p_231081_.get(1), (Double)p_231081_.get(2))),
         p_231083_ -> List.of(p_231083_.x(), p_231083_.y(), p_231083_.z())
      );
   public static final StreamCodec<ByteBuf, Vec3> STREAM_CODEC = new StreamCodec<ByteBuf, Vec3>() {
      public Vec3 decode(ByteBuf p_367785_) {
         return FriendlyByteBuf.readVec3(p_367785_);
      }

      public void encode(ByteBuf p_361343_, Vec3 p_364181_) {
         FriendlyByteBuf.writeVec3(p_361343_, p_364181_);
      }
   };
   public static final Vec3 ZERO = new Vec3(0.0, 0.0, 0.0);
   public static final Vec3 X_AXIS = new Vec3(1.0, 0.0, 0.0);
   public static final Vec3 Y_AXIS = new Vec3(0.0, 1.0, 0.0);
   public static final Vec3 Z_AXIS = new Vec3(0.0, 0.0, 1.0);
   public final double x;
   public final double y;
   public final double z;

   public static Vec3 atLowerCornerOf(Vec3i p_82529_) {
      return new Vec3(p_82529_.getX(), p_82529_.getY(), p_82529_.getZ());
   }

   public static Vec3 atLowerCornerWithOffset(Vec3i p_272866_, double p_273680_, double p_273668_, double p_273687_) {
      return new Vec3(p_272866_.getX() + p_273680_, p_272866_.getY() + p_273668_, p_272866_.getZ() + p_273687_);
   }

   public static Vec3 atCenterOf(Vec3i p_82513_) {
      return atLowerCornerWithOffset(p_82513_, 0.5, 0.5, 0.5);
   }

   public static Vec3 atBottomCenterOf(Vec3i p_82540_) {
      return atLowerCornerWithOffset(p_82540_, 0.5, 0.0, 0.5);
   }

   public static Vec3 upFromBottomCenterOf(Vec3i p_82515_, double p_82516_) {
      return atLowerCornerWithOffset(p_82515_, 0.5, p_82516_, 0.5);
   }

   public Vec3(double p_82484_, double p_82485_, double p_82486_) {
      this.x = p_82484_;
      this.y = p_82485_;
      this.z = p_82486_;
   }

   public Vec3(Vector3fc p_457331_) {
      this(p_457331_.x(), p_457331_.y(), p_457331_.z());
   }

   public Vec3(Vec3i p_363559_) {
      this(p_363559_.getX(), p_363559_.getY(), p_363559_.getZ());
   }

   public Vec3 vectorTo(Vec3 p_82506_) {
      return new Vec3(p_82506_.x - this.x, p_82506_.y - this.y, p_82506_.z - this.z);
   }

   public Vec3 normalize() {
      double d0 = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
      return d0 < 1.0E-5F ? ZERO : new Vec3(this.x / d0, this.y / d0, this.z / d0);
   }

   public double dot(Vec3 p_82527_) {
      return this.x * p_82527_.x + this.y * p_82527_.y + this.z * p_82527_.z;
   }

   public Vec3 cross(Vec3 p_82538_) {
      return new Vec3(this.y * p_82538_.z - this.z * p_82538_.y, this.z * p_82538_.x - this.x * p_82538_.z, this.x * p_82538_.y - this.y * p_82538_.x);
   }

   public Vec3 subtract(Vec3 p_82547_) {
      return this.subtract(p_82547_.x, p_82547_.y, p_82547_.z);
   }

   public Vec3 subtract(double p_365229_) {
      return this.subtract(p_365229_, p_365229_, p_365229_);
   }

   public Vec3 subtract(double p_82493_, double p_82494_, double p_82495_) {
      return this.add(-p_82493_, -p_82494_, -p_82495_);
   }

   public Vec3 add(double p_366763_) {
      return this.add(p_366763_, p_366763_, p_366763_);
   }

   public Vec3 add(Vec3 p_82550_) {
      return this.add(p_82550_.x, p_82550_.y, p_82550_.z);
   }

   public Vec3 add(double p_82521_, double p_82522_, double p_82523_) {
      return new Vec3(this.x + p_82521_, this.y + p_82522_, this.z + p_82523_);
   }

   public boolean closerThan(Position p_82510_, double p_82511_) {
      return this.distanceToSqr(p_82510_.x(), p_82510_.y(), p_82510_.z()) < p_82511_ * p_82511_;
   }

   public double distanceTo(Vec3 p_82555_) {
      double d0 = p_82555_.x - this.x;
      double d1 = p_82555_.y - this.y;
      double d2 = p_82555_.z - this.z;
      return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
   }

   public double distanceToSqr(Vec3 p_82558_) {
      double d0 = p_82558_.x - this.x;
      double d1 = p_82558_.y - this.y;
      double d2 = p_82558_.z - this.z;
      return d0 * d0 + d1 * d1 + d2 * d2;
   }

   public double distanceToSqr(double p_82532_, double p_82533_, double p_82534_) {
      double d0 = p_82532_ - this.x;
      double d1 = p_82533_ - this.y;
      double d2 = p_82534_ - this.z;
      return d0 * d0 + d1 * d1 + d2 * d2;
   }

   public boolean closerThan(Vec3 p_310995_, double p_312797_, double p_311239_) {
      double d0 = p_310995_.x() - this.x;
      double d1 = p_310995_.y() - this.y;
      double d2 = p_310995_.z() - this.z;
      return Mth.lengthSquared(d0, d2) < Mth.square(p_312797_) && Math.abs(d1) < p_311239_;
   }

   public Vec3 scale(double p_82491_) {
      return this.multiply(p_82491_, p_82491_, p_82491_);
   }

   public Vec3 reverse() {
      return this.scale(-1.0);
   }

   public Vec3 multiply(Vec3 p_82560_) {
      return this.multiply(p_82560_.x, p_82560_.y, p_82560_.z);
   }

   public Vec3 multiply(double p_82543_, double p_82544_, double p_82545_) {
      return new Vec3(this.x * p_82543_, this.y * p_82544_, this.z * p_82545_);
   }

   public Vec3 horizontal() {
      return new Vec3(this.x, 0.0, this.z);
   }

   public Vec3 offsetRandom(RandomSource p_272810_, float p_273473_) {
      return this.add((p_272810_.nextFloat() - 0.5F) * p_273473_, (p_272810_.nextFloat() - 0.5F) * p_273473_, (p_272810_.nextFloat() - 0.5F) * p_273473_);
   }

   public Vec3 offsetRandomXZ(RandomSource p_455940_, float p_451564_) {
      return this.add((p_455940_.nextFloat() - 0.5F) * p_451564_, 0.0, (p_455940_.nextFloat() - 0.5F) * p_451564_);
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
   public boolean equals(Object p_82552_) {
      if (this == p_82552_) {
         return true;
      }

      if (p_82552_ instanceof Vec3 vec3) {
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
      long j = Double.doubleToLongBits(this.x);
      int i = (int)(j ^ j >>> 32);
      j = Double.doubleToLongBits(this.y);
      i = 31 * i + (int)(j ^ j >>> 32);
      j = Double.doubleToLongBits(this.z);
      return 31 * i + (int)(j ^ j >>> 32);
   }

   @Override
   public String toString() {
      return "(" + this.x + ", " + this.y + ", " + this.z + ")";
   }

   public Vec3 lerp(Vec3 p_165922_, double p_165923_) {
      return new Vec3(Mth.lerp(p_165923_, this.x, p_165922_.x), Mth.lerp(p_165923_, this.y, p_165922_.y), Mth.lerp(p_165923_, this.z, p_165922_.z));
   }

   public Vec3 xRot(float p_82497_) {
      float f = Mth.cos(p_82497_);
      float f1 = Mth.sin(p_82497_);
      double d0 = this.x;
      double d1 = this.y * f + this.z * f1;
      double d2 = this.z * f - this.y * f1;
      return new Vec3(d0, d1, d2);
   }

   public Vec3 yRot(float p_82525_) {
      float f = Mth.cos(p_82525_);
      float f1 = Mth.sin(p_82525_);
      double d0 = this.x * f + this.z * f1;
      double d1 = this.y;
      double d2 = this.z * f - this.x * f1;
      return new Vec3(d0, d1, d2);
   }

   public Vec3 zRot(float p_82536_) {
      float f = Mth.cos(p_82536_);
      float f1 = Mth.sin(p_82536_);
      double d0 = this.x * f + this.y * f1;
      double d1 = this.y * f - this.x * f1;
      double d2 = this.z;
      return new Vec3(d0, d1, d2);
   }

   public Vec3 rotateClockwise90() {
      return new Vec3(-this.z, this.y, this.x);
   }

   public static Vec3 directionFromRotation(Vec2 p_82504_) {
      return directionFromRotation(p_82504_.x, p_82504_.y);
   }

   public static Vec3 directionFromRotation(float p_82499_, float p_82500_) {
      float f = Mth.cos(-p_82500_ * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f1 = Mth.sin(-p_82500_ * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f2 = -Mth.cos(-p_82499_ * (float) (Math.PI / 180.0));
      float f3 = Mth.sin(-p_82499_ * (float) (Math.PI / 180.0));
      return new Vec3(f1 * f2, f3, f * f2);
   }

   public Vec2 rotation() {
      float f = (float)Math.atan2(-this.x, this.z) * (180.0F / (float)Math.PI);
      float f1 = (float)Math.asin(-this.y / Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z)) * (180.0F / (float)Math.PI);
      return new Vec2(f1, f);
   }

   public Vec3 align(EnumSet<Direction.Axis> p_82518_) {
      double d0 = p_82518_.contains(Direction.Axis.X) ? Mth.floor(this.x) : this.x;
      double d1 = p_82518_.contains(Direction.Axis.Y) ? Mth.floor(this.y) : this.y;
      double d2 = p_82518_.contains(Direction.Axis.Z) ? Mth.floor(this.z) : this.z;
      return new Vec3(d0, d1, d2);
   }

   public double get(Direction.Axis p_82508_) {
      return p_82508_.choose(this.x, this.y, this.z);
   }

   public Vec3 with(Direction.Axis p_193104_, double p_193105_) {
      double d0 = p_193104_ == Direction.Axis.X ? p_193105_ : this.x;
      double d1 = p_193104_ == Direction.Axis.Y ? p_193105_ : this.y;
      double d2 = p_193104_ == Direction.Axis.Z ? p_193105_ : this.z;
      return new Vec3(d0, d1, d2);
   }

   public Vec3 relative(Direction p_231076_, double p_231077_) {
      Vec3i vec3i = p_231076_.getUnitVec3i();
      return new Vec3(this.x + p_231077_ * vec3i.getX(), this.y + p_231077_ * vec3i.getY(), this.z + p_231077_ * vec3i.getZ());
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

   public Vec3 projectedOn(Vec3 p_368324_) {
      return p_368324_.lengthSqr() == 0.0 ? p_368324_ : p_368324_.scale(this.dot(p_368324_)).scale(1.0 / p_368324_.lengthSqr());
   }

   public static Vec3 applyLocalCoordinatesToRotation(Vec2 p_455493_, Vec3 p_457927_) {
      float f = Mth.cos((p_455493_.y + 90.0F) * (float) (Math.PI / 180.0));
      float f1 = Mth.sin((p_455493_.y + 90.0F) * (float) (Math.PI / 180.0));
      float f2 = Mth.cos(-p_455493_.x * (float) (Math.PI / 180.0));
      float f3 = Mth.sin(-p_455493_.x * (float) (Math.PI / 180.0));
      float f4 = Mth.cos((-p_455493_.x + 90.0F) * (float) (Math.PI / 180.0));
      float f5 = Mth.sin((-p_455493_.x + 90.0F) * (float) (Math.PI / 180.0));
      Vec3 vec3 = new Vec3(f * f2, f3, f1 * f2);
      Vec3 vec31 = new Vec3(f * f4, f5, f1 * f4);
      Vec3 vec32 = vec3.cross(vec31).scale(-1.0);
      double d0 = vec3.x * p_457927_.z + vec31.x * p_457927_.y + vec32.x * p_457927_.x;
      double d1 = vec3.y * p_457927_.z + vec31.y * p_457927_.y + vec32.y * p_457927_.x;
      double d2 = vec3.z * p_457927_.z + vec31.z * p_457927_.y + vec32.z * p_457927_.x;
      return new Vec3(d0, d1, d2);
   }

   public Vec3 addLocalCoordinates(Vec3 p_451365_) {
      return applyLocalCoordinatesToRotation(this.rotation(), p_451365_);
   }

   public boolean isFinite() {
      return Double.isFinite(this.x) && Double.isFinite(this.y) && Double.isFinite(this.z);
   }
}
