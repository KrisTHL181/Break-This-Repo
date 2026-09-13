package net.minecraft.util;

import java.time.Month;
import java.time.MonthDay;
import java.time.ZonedDateTime;
import java.util.List;

public class SpecialDates {
   public static final MonthDay HALLOWEEN = MonthDay.of(Month.OCTOBER, 31);
   public static final List<MonthDay> CHRISTMAS_RANGE = List.of(
      MonthDay.of(Month.DECEMBER, 24), MonthDay.of(Month.DECEMBER, 25), MonthDay.of(Month.DECEMBER, 26)
   );
   public static final MonthDay CHRISTMAS = MonthDay.of(Month.DECEMBER, 24);
   public static final MonthDay NEW_YEAR = MonthDay.of(Month.JANUARY, 1);

   public static MonthDay dayNow() {
      return MonthDay.from(ZonedDateTime.now());
   }

   public static boolean isHalloween() {
      return HALLOWEEN.equals(dayNow());
   }

   public static boolean isExtendedChristmas() {
      return CHRISTMAS_RANGE.contains(dayNow());
   }
}
