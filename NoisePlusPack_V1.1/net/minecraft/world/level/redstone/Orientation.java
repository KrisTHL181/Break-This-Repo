package net.minecraft.world.level.redstone;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

public class Orientation {
   public static final StreamCodec<ByteBuf, Orientation> STREAM_CODEC = ByteBufCodecs.idMapper(Orientation::fromIndex, Orientation::getIndex);
   private static final Orientation[] ORIENTATIONS = Util.make(() -> {
      Orientation[] aorientation = new Orientation[48];
      generateContext(new Orientation(Direction.UP, Direction.NORTH, Orientation.SideBias.LEFT), aorientation);
      return aorientation;
   });
   private final Direction up;
   private final Direction front;
   private final Direction side;
   private final Orientation.SideBias sideBias;
   private final int index;
   private final List<Direction> neighbors;
   private final List<Direction> horizontalNeighbors;
   private final List<Direction> verticalNeighbors;
   private final Map<Direction, Orientation> withFront = new EnumMap<>(Direction.class);
   private final Map<Direction, Orientation> withUp = new EnumMap<>(Direction.class);
   private final Map<Orientation.SideBias, Orientation> withSideBias = new EnumMap<>(Orientation.SideBias.class);

   private Orientation(Direction p_370197_, Direction p_369849_, Orientation.SideBias p_366338_) {
      this.up = p_370197_;
      this.front = p_369849_;
      this.sideBias = p_366338_;
      this.index = generateIndex(p_370197_, p_369849_, p_366338_);
      Vec3i vec3i = p_369849_.getUnitVec3i().cross(p_370197_.getUnitVec3i());
      Direction direction = Direction.getNearest(vec3i, null);
      Objects.requireNonNull(direction);
      if (this.sideBias == Orientation.SideBias.RIGHT) {
         this.side = direction;
      } else {
         this.side = direction.getOpposite();
      }

      this.neighbors = List.of(this.front.getOpposite(), this.front, this.side, this.side.getOpposite(), this.up.getOpposite(), this.up);
      this.horizontalNeighbors = this.neighbors.stream().filter(p_363625_ -> p_363625_.getAxis() != this.up.getAxis()).toList();
      this.verticalNeighbors = this.neighbors.stream().filter(p_365283_ -> p_365283_.getAxis() == this.up.getAxis()).toList();
   }

   public static Orientation of(Direction p_367835_, Direction p_362776_, Orientation.SideBias p_367906_) {
      return ORIENTATIONS[generateIndex(p_367835_, p_362776_, p_367906_)];
   }

   public Orientation withUp(Direction p_368311_) {
      return this.withUp.get(p_368311_);
   }

   public Orientation withFront(Direction p_366881_) {
      return this.withFront.get(p_366881_);
   }

   public Orientation withFrontPreserveUp(Direction p_364290_) {
      return p_364290_.getAxis() == this.up.getAxis() ? this : this.withFront.get(p_364290_);
   }

   public Orientation withFrontAdjustSideBias(Direction p_367524_) {
      Orientation orientation = this.withFront(p_367524_);
      return this.front == orientation.side ? orientation.withMirror() : orientation;
   }

   public Orientation withSideBias(Orientation.SideBias p_365192_) {
      return this.withSideBias.get(p_365192_);
   }

   public Orientation withMirror() {
      return this.withSideBias(this.sideBias.getOpposite());
   }

   public Direction getFront() {
      return this.front;
   }

   public Direction getUp() {
      return this.up;
   }

   public Direction getSide() {
      return this.side;
   }

   public Orientation.SideBias getSideBias() {
      return this.sideBias;
   }

   public List<Direction> getDirections() {
      return this.neighbors;
   }

   public List<Direction> getHorizontalDirections() {
      return this.horizontalNeighbors;
   }

   public List<Direction> getVerticalDirections() {
      return this.verticalNeighbors;
   }

   @Override
   public String toString() {
      return "[up=" + this.up + ",front=" + this.front + ",sideBias=" + this.sideBias + "]";
   }

   public int getIndex() {
      return this.index;
   }

   public static Orientation fromIndex(int p_367043_) {
      return ORIENTATIONS[p_367043_];
   }

   public static Orientation random(RandomSource p_363137_) {
      return Util.getRandom(ORIENTATIONS, p_363137_);
   }

   private static Orientation generateContext(Orientation p_365452_, Orientation[] p_362094_) {
      if (p_362094_[p_365452_.getIndex()] != null) {
         return p_362094_[p_365452_.getIndex()];
      }

      p_362094_[p_365452_.getIndex()] = p_365452_;

      for (Orientation.SideBias orientation$sidebias : Orientation.SideBias.values()) {
         p_365452_.withSideBias.put(orientation$sidebias, generateContext(new Orientation(p_365452_.up, p_365452_.front, orientation$sidebias), p_362094_));
      }

      for (Direction direction1 : Direction.values()) {
         Direction direction = p_365452_.up;
         if (direction1 == p_365452_.up) {
            direction = p_365452_.front.getOpposite();
         }

         if (direction1 == p_365452_.up.getOpposite()) {
            direction = p_365452_.front;
         }

         p_365452_.withFront.put(direction1, generateContext(new Orientation(direction, direction1, p_365452_.sideBias), p_362094_));
      }

      for (Direction direction2 : Direction.values()) {
         Direction direction3 = p_365452_.front;
         if (direction2 == p_365452_.front) {
            direction3 = p_365452_.up.getOpposite();
         }

         if (direction2 == p_365452_.front.getOpposite()) {
            direction3 = p_365452_.up;
         }

         p_365452_.withUp.put(direction2, generateContext(new Orientation(direction2, direction3, p_365452_.sideBias), p_362094_));
      }

      return p_365452_;
   }

   @VisibleForTesting
   protected static int generateIndex(Direction p_368123_, Direction p_368048_, Orientation.SideBias p_369086_) {
      if (p_368123_.getAxis() == p_368048_.getAxis()) {
         throw new IllegalStateException("Up-vector and front-vector can not be on the same axis");
      }

      int i;
      if (p_368123_.getAxis() == Direction.Axis.Y) {
         i = p_368048_.getAxis() == Direction.Axis.X ? 1 : 0;
      } else {
         i = p_368048_.getAxis() == Direction.Axis.Y ? 1 : 0;
      }

      int j = i << 1 | p_368048_.getAxisDirection().ordinal();
      return ((p_368123_.ordinal() << 2) + j << 1) + p_369086_.ordinal();
   }

   public enum SideBias {
      LEFT("left"),
      RIGHT("right");

      private final String name;

      SideBias(final String p_365296_) {
         this.name = p_365296_;
      }

      public Orientation.SideBias getOpposite() {
         return this == LEFT ? RIGHT : LEFT;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }
}
