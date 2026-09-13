package net.minecraft.util;

import org.jspecify.annotations.Nullable;

public class MemoryReserve {
   private static byte @Nullable [] reserve;

   public static void allocate() {
      reserve = new byte[10485760];
   }

   public static void release() {
      if (reserve != null) {
         reserve = null;

         try {
            System.gc();
            System.gc();
            System.gc();
         } catch (Throwable var1) {
         }
      }
   }
}
