package com.mojang.authlib.minecraft;

public interface TelemetrySession {
   TelemetrySession DISABLED = new TelemetrySession() {
      @Override
      public boolean isEnabled() {
         return false;
      }

      @Override
      public TelemetryEvent createNewEvent(String type) {
         return TelemetryEvent.EMPTY;
      }
   };

   boolean isEnabled();

   TelemetryEvent createNewEvent(String var1);
}
