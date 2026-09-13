package com.mojang.realmsclient;

import java.util.Locale;

public enum Unit {
   B,
   KB,
   MB,
   GB;

   private static final int BASE_UNIT = 1024;

   public static Unit getLargest(final long bytes) {
      if (bytes < 1024L) {
         return B;
      }

      try {
         int exp = (int)(Math.log(bytes) / Math.log(1024.0));
         String pre = String.valueOf("KMGTPE".charAt(exp - 1));
         return valueOf(pre + "B");
      } catch (Exception ignored) {
         return GB;
      }
   }

   public static double convertTo(final long bytes, final Unit unit) {
      return unit == B ? bytes : bytes / Math.pow(1024.0, unit.ordinal());
   }

   public static String humanReadable(final long bytes) {
      int unit = 1024;
      if (bytes < 1024L) {
         return bytes + " B";
      }

      int exp = (int)(Math.log(bytes) / Math.log(1024.0));
      String pre = "KMGTPE".charAt(exp - 1) + "";
      return String.format(Locale.ROOT, "%.1f %sB", bytes / Math.pow(1024.0, exp), pre);
   }

   public static String humanReadable(final long bytes, final Unit unit) {
      return String.format(Locale.ROOT, "%." + (unit == GB ? "1" : "0") + "f %s", convertTo(bytes, unit), unit.name());
   }
}
