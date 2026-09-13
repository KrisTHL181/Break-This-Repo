package com.mojang.authlib.minecraft;

public interface TelemetryEvent extends TelemetryPropertyContainer {
   TelemetryEvent EMPTY = new TelemetryEvent() {
      @Override
      public void addProperty(String id, String value) {
      }

      @Override
      public void addProperty(String id, int value) {
      }

      @Override
      public void addProperty(String id, long value) {
      }

      @Override
      public void addProperty(String id, boolean value) {
      }

      @Override
      public void addNullProperty(String id) {
      }

      @Override
      public void send() {
      }
   };

   void send();
}
