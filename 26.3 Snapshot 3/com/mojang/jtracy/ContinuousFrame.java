package com.mojang.jtracy;

public class ContinuousFrame {
   static final ContinuousFrame UNAVAILABLE = new ContinuousFrame(0L);
   private final long id;

   ContinuousFrame(long id) {
      this.id = id;
   }

   public void mark() {
      if (this != UNAVAILABLE) {
         TracyBindings.markFrame(this.id);
      }
   }
}
