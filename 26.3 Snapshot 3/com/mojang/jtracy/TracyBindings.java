package com.mojang.jtracy;

import java.nio.ByteBuffer;

class TracyBindings {
   private TracyBindings() {
   }

   static native void startup();

   static native void shutdown();

   static native void markFrame(long var0);

   static native void markFrameStart(long var0);

   static native void markFrameEnd(long var0);

   static native int beginZone(String var0, String var1, String var2, int var3);

   static native int frameImage(ByteBuffer var0, int var1, int var2, int var3, boolean var4);

   static native void endZone(int var0);

   static native void addZoneText(int var0, String var1);

   static native void setZoneColor(int var0, int var1);

   static native void addZoneValue(int var0, long var1);

   static native long mallocNamed(long var0, long var2, int var4);

   static native long freeNamed(long var0, long var2);

   static native void setThreadName(String var0, int var1);

   static native void plotValue(long var0, double var2);

   static native long leakName(String var0);

   static native void appInfo(String var0);

   static native void message(String var0);

   static native void messageColored(String var0, int var1);

   static native void newGpuContext(int var0, long var1, float var3, int var4, int var5);

   static native void setGpuContextName(int var0, String var1);

   static native int beginGpuZone(int var0, int var1, String var2, String var3, String var4, int var5);

   static native int endGpuZone(int var0, int var1);

   static native int submitQueryTimestamp(int var0, int var1, long var2);
}
