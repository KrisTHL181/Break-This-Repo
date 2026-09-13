package net.minecraft.world.level.block;

import com.mojang.math.OctahedralGroup;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;

public enum Rotation implements StringRepresentable {
   NONE(0, "none", OctahedralGroup.IDENTITY),
   CLOCKWISE_90(1, "clockwise_90", OctahedralGroup.ROT_90_Y_NEG),
   CLOCKWISE_180(2, "180", OctahedralGroup.ROT_180_FACE_XZ),
   COUNTERCLOCKWISE_90(3, "counterclockwise_90", OctahedralGroup.ROT_90_Y_POS);

   public static final IntFunction<Rotation> BY_ID = ByIdMap.continuous(Rotation::getIndex, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
   public static final Codec<Rotation> CODEC = StringRepresentable.fromEnum(Rotation::values);
   public static final StreamCodec<ByteBuf, Rotation> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Rotation::getIndex);
   @Deprecated
   public static final Codec<Rotation> LEGACY_CODEC = ExtraCodecs.legacyEnum(Rotation::valueOf);
   private final int index;
   private final String id;
   private final OctahedralGroup rotation;

   Rotation(final int index, final String id, final OctahedralGroup rotation) {
      this.index = index;
      this.id = id;
      this.rotation = rotation;
   }

   public Rotation getRotated(final Rotation rot) {
      return switch (rot) {
         case CLOCKWISE_90 -> {
            switch (this) {
               case NONE:
                  yield CLOCKWISE_90;
               case CLOCKWISE_90:
                  yield CLOCKWISE_180;
               case CLOCKWISE_180:
                  yield COUNTERCLOCKWISE_90;
               case COUNTERCLOCKWISE_90:
                  yield NONE;
               default:
                  throw new MatchException(null, null);
            }
         }
         case CLOCKWISE_180 -> {
            switch (this) {
               case NONE:
                  yield CLOCKWISE_180;
               case CLOCKWISE_90:
                  yield COUNTERCLOCKWISE_90;
               case CLOCKWISE_180:
                  yield NONE;
               case COUNTERCLOCKWISE_90:
                  yield CLOCKWISE_90;
               default:
                  throw new MatchException(null, null);
            }
         }
         case COUNTERCLOCKWISE_90 -> {
            switch (this) {
               case NONE:
                  yield COUNTERCLOCKWISE_90;
               case CLOCKWISE_90:
                  yield NONE;
               case CLOCKWISE_180:
                  yield CLOCKWISE_90;
               case COUNTERCLOCKWISE_90:
                  yield CLOCKWISE_180;
               default:
                  throw new MatchException(null, null);
            }
         }
         default -> this;
      };
   }

   public OctahedralGroup rotation() {
      return this.rotation;
   }

   public Direction rotate(final Direction direction) {
      if (direction.getAxis() == Direction.Axis.Y) {
         return direction;
      }

      return switch (this) {
         case CLOCKWISE_90 -> direction.getClockWise();
         case CLOCKWISE_180 -> direction.getOpposite();
         case COUNTERCLOCKWISE_90 -> direction.getCounterClockWise();
         default -> direction;
      };
   }

   public int rotate(final int rotation, final int steps) {
      return switch (this) {
         case CLOCKWISE_90 -> (rotation + steps / 4) % steps;
         case CLOCKWISE_180 -> (rotation + steps / 2) % steps;
         case COUNTERCLOCKWISE_90 -> (rotation + steps * 3 / 4) % steps;
         default -> rotation;
      };
   }

   public static Rotation getRandom(final RandomSource random) {
      return Util.getRandom(values(), random);
   }

   public static List<Rotation> getShuffled(final RandomSource random) {
      return Util.shuffledCopy(values(), random);
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }

   private int getIndex() {
      return this.index;
   }
}
