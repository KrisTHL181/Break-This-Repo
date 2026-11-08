package com.mojang.jtracy;

public class Plot {
   static final Plot UNAVAILABLE = new Plot(0L);
   private final long handle;

   Plot(long handle) {
      this.handle = handle;
   }

   public void setValue(double value) {
      if (this != UNAVAILABLE) {
         TracyBindings.plotValue(this.handle, value);
      }
   }
}
