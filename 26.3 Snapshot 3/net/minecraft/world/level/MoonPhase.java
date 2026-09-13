package net.minecraft.world.level;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum MoonPhase implements StringRepresentable {
   FULL_MOON(0, "full_moon"),
   WANING_GIBBOUS(1, "waning_gibbous"),
   THIRD_QUARTER(2, "third_quarter"),
   WANING_CRESCENT(3, "waning_crescent"),
   NEW_MOON(4, "new_moon"),
   WAXING_CRESCENT(5, "waxing_crescent"),
   FIRST_QUARTER(6, "first_quarter"),
   WAXING_GIBBOUS(7, "waxing_gibbous");

   public static final Codec<MoonPhase> CODEC = StringRepresentable.fromEnum(MoonPhase::values);
   public static final int COUNT = values().length;
   public static final int PHASE_LENGTH = 24000;
   private final int index;
   private final String name;

   MoonPhase(final int index, final String name) {
      this.index = index;
      this.name = name;
   }

   public int index() {
      return this.index;
   }

   public int startTick() {
      return this.index * 24000;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
