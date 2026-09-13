package net.minecraft.world.level.block.state.properties;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum ChestType implements StringRepresentable {
   SINGLE("single"),
   LEFT("left"),
   RIGHT("right");

   public static final Codec<ChestType> CODEC = StringRepresentable.fromEnum(ChestType::values);
   private final String name;

   ChestType(final String name) {
      this.name = name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public ChestType getOpposite() {
      return switch (this) {
         case SINGLE -> SINGLE;
         case LEFT -> RIGHT;
         case RIGHT -> LEFT;
      };
   }
}
