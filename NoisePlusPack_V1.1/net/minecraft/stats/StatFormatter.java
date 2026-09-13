package net.minecraft.stats;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public interface StatFormatter {
   DecimalFormat DECIMAL_FORMAT = new DecimalFormat("########0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));
   StatFormatter DEFAULT = NumberFormat.getIntegerInstance(Locale.US)::format;
   StatFormatter DIVIDE_BY_TEN = p_12885_ -> DECIMAL_FORMAT.format(p_12885_ * 0.1);
   StatFormatter DISTANCE = p_12883_ -> {
      double d0 = p_12883_ / 100.0;
      double d1 = d0 / 1000.0;
      if (d1 > 0.5) {
         return DECIMAL_FORMAT.format(d1) + " km";
      } else {
         return d0 > 0.5 ? DECIMAL_FORMAT.format(d0) + " m" : p_12883_ + " cm";
      }
   };
   StatFormatter TIME = p_12879_ -> {
      double d0 = p_12879_ / 20.0;
      double d1 = d0 / 60.0;
      double d2 = d1 / 60.0;
      double d3 = d2 / 24.0;
      double d4 = d3 / 365.0;
      if (d4 > 0.5) {
         return DECIMAL_FORMAT.format(d4) + " y";
      } else if (d3 > 0.5) {
         return DECIMAL_FORMAT.format(d3) + " d";
      } else if (d2 > 0.5) {
         return DECIMAL_FORMAT.format(d2) + " h";
      } else {
         return d1 > 0.5 ? DECIMAL_FORMAT.format(d1) + " min" : d0 + " s";
      }
   };

   String format(int var1);
}
