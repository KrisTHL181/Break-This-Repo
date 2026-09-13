package com.mojang.jtracy;

public class GpuContext {
   static final GpuContext UNAVAILABLE = new GpuContext(0);
   private final int id;

   GpuContext(int id) {
      this.id = id;
   }

   public GpuContext setName(String name) {
      if (this != UNAVAILABLE) {
         TracyBindings.setGpuContextName(this.id, name);
      }

      return this;
   }

   public void beginZone(int query, String name, String function, String file, int line) {
      if (this != UNAVAILABLE) {
         TracyBindings.beginGpuZone(this.id, query, name, function, file, line);
      }
   }

   public void endZone(int query) {
      if (this != UNAVAILABLE) {
         TracyBindings.endGpuZone(this.id, query);
      }
   }

   public void submitQueryTimestamp(int query, long timestamp) {
      if (this != UNAVAILABLE) {
         TracyBindings.submitQueryTimestamp(this.id, query, timestamp);
      }
   }
}
