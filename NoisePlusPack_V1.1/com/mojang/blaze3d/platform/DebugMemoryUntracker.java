package com.mojang.blaze3d.platform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.Pointer;

@OnlyIn(Dist.CLIENT)
public class DebugMemoryUntracker {
   private static final @Nullable MethodHandle UNTRACK = GLX.make(() -> {
      try {
         Lookup lookup = MethodHandles.lookup();
         Class<?> oclass = Class.forName("org.lwjgl.system.MemoryManage$DebugAllocator");
         Method method = oclass.getDeclaredMethod("untrack", long.class);
         method.setAccessible(true);
         Field field = Class.forName("org.lwjgl.system.MemoryUtil$LazyInit").getDeclaredField("ALLOCATOR");
         field.setAccessible(true);
         Object object = field.get(null);
         return oclass.isInstance(object) ? lookup.unreflect(method) : null;
      } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException | ClassNotFoundException classnotfoundexception) {
         throw new RuntimeException(classnotfoundexception);
      }
   });

   public static void untrack(long p_84002_) {
      if (UNTRACK != null) {
         try {
            UNTRACK.invoke((long)p_84002_);
         } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
         }
      }
   }

   public static void untrack(Pointer p_84004_) {
      untrack(p_84004_.address());
   }
}
