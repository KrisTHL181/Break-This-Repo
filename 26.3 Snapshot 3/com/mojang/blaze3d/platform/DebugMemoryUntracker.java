package com.mojang.blaze3d.platform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.jspecify.annotations.Nullable;

public class DebugMemoryUntracker {
   private static final @Nullable MethodHandle UNTRACK = GLX.make(() -> {
      try {
         Lookup lookup = MethodHandles.lookup();
         Class<?> debugAllocator = Class.forName("org.lwjgl.system.MemoryManage$DebugAllocator");
         Method reflectionUntrack = debugAllocator.getDeclaredMethod("untrack", long.class);
         reflectionUntrack.setAccessible(true);
         Field allocatorField = Class.forName("org.lwjgl.system.MemoryUtil$LazyInit").getDeclaredField("ALLOCATOR");
         allocatorField.setAccessible(true);
         Object allocator = allocatorField.get(null);
         return debugAllocator.isInstance(allocator) ? lookup.unreflect(reflectionUntrack) : null;
      } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   });

   public static void untrack(final long address) {
      if (UNTRACK != null) {
         try {
            UNTRACK.invoke((long)address);
         } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
         }
      }
   }
}
