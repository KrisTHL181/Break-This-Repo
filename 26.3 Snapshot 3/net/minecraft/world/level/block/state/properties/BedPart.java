package net.minecraft.world.level.block.state.properties;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum BedPart implements StringRepresentable {
   HEAD("head"),
   FOOT("foot");

   public static final Codec<BedPart> CODEC = StringRepresentable.fromEnum(BedPart::values);
   private final String name;

   BedPart(final String name) {
      this.name = name;
   }

   @Override
   public String toString() {
      return this.name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
