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

   Mirror(final String p_221529_, final OctahedralGroup p_221530_) {
      this.id = p_221529_;
      this.symbol = Component.translatable("mirror." + p_221529_);
      this.rotation = p_221530_;
   }

   public int mirror(int p_54844_, int p_54845_) {
      int i = p_54845_ / 2;
      int j = p_54844_ > i ? p_54844_ - p_54845_ : p_54844_;
      switch (this) {
         case LEFT_RIGHT:
            return (i - j + p_54845_) % p_54845_;
         case FRONT_BACK:
            return (p_54845_ - j) % p_54845_;
         default:
            return p_54844_;
      }
   }

   public Rotation getRotation(Direction p_54847_) {
      Direction.Axis direction$axis = p_54847_.getAxis();
      return this == LEFT_RIGHT && direction$axis == Direction.Axis.Z || this == FRONT_BACK && direction$axis == Direction.Axis.X
         ? Rotation.CLOCKWISE_180
         : Rotation.NONE;
   }

   public Direction mirror(Direction p_54849_) {
      if (this == FRONT_BACK && p_54849_.getAxis() == Direction.Axis.X) {
         return p_54849_.getOpposite();
      } else {
         return this == LEFT_RIGHT && p_54849_.getAxis() == Direction.Axis.Z ? p_54849_.getOpposite() : p_54849_;
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
