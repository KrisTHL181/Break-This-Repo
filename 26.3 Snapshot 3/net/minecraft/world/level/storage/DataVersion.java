package net.minecraft.world.level.storage;

import net.minecraft.SharedConstants;

public record DataVersion(int version, String series) {
   public static final String MAIN_SERIES = "main";

   public boolean isSideSeries() {
      return !this.series.equals("main");
   }

   public boolean isCompatible(final DataVersion other) {
      return SharedConstants.DEBUG_OPEN_INCOMPATIBLE_WORLDS ? true : this.series().equals(other.series());
   }
}
