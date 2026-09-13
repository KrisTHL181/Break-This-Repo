package net.minecraft.world.level.block;

import com.mojang.math.OctahedralGroup;
import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

public enum Mirror implements StringRepresentable {
   NONE("none", OctahedralGroup.IDENTITY),
   LEFT_RIGHT("left_right", OctahedralGroup.INVERT_Z),
   FRONT_BACK("front_back", OctahedralGroup.INVERT_X);

   public static final Codec<Mirror> CODEC = StringRepresentable.fromEnum(Mirror::values);
   @Deprecated
   public static final Codec<Mirror> LEGACY_CODEC = ExtraCodecs.legacyEnum(Mirror::valueOf);
   private final String id;
   private final Component symbol;
   private final OctahedralGroup rotation;

   Mirror(final String id, final OctahedralGroup rotation) {
      this.id = id;
      this.symbol = Component.translatable("mirror." + id);
      this.rotation = rotation;
   }

   public int mirror(final int rotation, final int steps) {
      int halfSteps = steps / 2;
      int correctedRotation = rotation > halfSteps ? rotation - steps : rotation;

      return switch (this) {
         case LEFT_RIGHT -> (halfSteps - correctedRotation + steps) % steps;
         case FRONT_BACK -> (steps - correctedRotation) % steps;
         default -> rotation;
      };
   }

   public Rotation getRotation(final Direction value) {
      Direction.Axis axis = value.getAxis();
      return (this != LEFT_RIGHT || axis != Direction.Axis.Z) && (this != FRONT_BACK || axis != Direction.Axis.X) ? Rotation.NONE : Rotation.CLOCKWISE_180;
   }

   public Direction mirror(final Direction direction) {
      if (this == FRONT_BACK && direction.getAxis() == Direction.Axis.X) {
         return direction.getOpposite();
      } else {
         return this == LEFT_RIGHT && direction.getAxis() == Direction.Axis.Z ? direction.getOpposite() : direction;
      }
   }

   public OctahedralGroup rotation() {
      return this.rotation;
   }

   public Component symbol() {
      return this.symbol;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }
}
