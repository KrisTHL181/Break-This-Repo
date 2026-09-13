package com.mojang.text2speech;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

public enum OperatingSystem {
   LINUX("linux"),
   WINDOWS("win"),
   MAC_OS("mac"),
   UNSUPPORTED(null);

   private final @Nullable String detectWith;

   OperatingSystem(@Nullable String detectWith) {
      this.detectWith = detectWith;
   }

   public static OperatingSystem get() {
      String test = System.getProperty("os.name").toLowerCase(Locale.ROOT);

      for (OperatingSystem value : values()) {
         if (value.detectWith != null && test.contains(value.detectWith)) {
            return value;
         }
      }

      return UNSUPPORTED;
   }
}
