package com.mojang.jtracy;

public class Zone implements AutoCloseable {
   static final Zone UNAVAILABLE = new Zone(0);
   private final int id;

   Zone(int id) {
      this.id = id;
   }

   public Zone addText(String text) {
      if (this != UNAVAILABLE) {
         TracyBindings.addZoneText(this.id, text);
      }

      return this;
   }

   public Zone setColor(int color) {
      if (this != UNAVAILABLE) {
         TracyBindings.setZoneColor(this.id, color);
      }

      return this;
   }

   public Zone addValue(long value) {
      if (this != UNAVAILABLE) {
         TracyBindings.addZoneValue(this.id, value);
      }

      return this;
   }

   @Override
   public void close() {
      if (this != UNAVAILABLE) {
         TracyBindings.endZone(this.id);
      }
   }
}
