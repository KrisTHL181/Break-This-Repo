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

   Rotation(final int p_396352_, final String p_221988_, final OctahedralGroup p_221989_) {
      this.index = p_396352_;
      this.id = p_221988_;
      this.rotation = p_221989_;
   }

   public Rotation getRotated(Rotation p_55953_) {
      return switch (p_55953_) {
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

   public Direction rotate(Direction p_55955_) {
      if (p_55955_.getAxis() == Direction.Axis.Y) {
         return p_55955_;
      }

      return switch (this) {
         case CLOCKWISE_90 -> p_55955_.getClockWise();
         case CLOCKWISE_180 -> p_55955_.getOpposite();
         case COUNTERCLOCKWISE_90 -> p_55955_.getCounterClockWise();
         default -> p_55955_;
      };
   }

   public int rotate(int p_55950_, int p_55951_) {
      return switch (this) {
         case CLOCKWISE_90 -> (p_55950_ + p_55951_ / 4) % p_55951_;
         case CLOCKWISE_180 -> (p_55950_ + p_55951_ / 2) % p_55951_;
         case COUNTERCLOCKWISE_90 -> (p_55950_ + p_55951_ * 3 / 4) % p_55951_;
         default -> p_55950_;
      };
   }

   public static Rotation getRandom(RandomSource p_221991_) {
      return Util.getRandom(values(), p_221991_);
   }

   public static List<Rotation> getShuffled(RandomSource p_221993_) {
      return Util.shuffledCopy(values(), p_221993_);
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }

   private int getIndex() {
      return this.index;
   }
}
