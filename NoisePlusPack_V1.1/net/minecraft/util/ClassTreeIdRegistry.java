package net.minecraft.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class ClassTreeIdRegistry {
   public static final int NO_ID_VALUE = -1;
   private final Object2IntMap<Class<?>> classToLastIdCache = Util.make(new Object2IntOpenHashMap(), p_327761_ -> p_327761_.defaultReturnValue(-1));

   public int getLastIdFor(Class<?> p_330417_) {
      int i = this.classToLastIdCache.getInt(p_330417_);
      if (i != -1) {
         return i;
      }

      Class<?> oclass = p_330417_;

      while ((oclass = oclass.getSuperclass()) != Object.class) {
         int j = this.classToLastIdCache.getInt(oclass);
         if (j != -1) {
            return j;
         }
      }

      return -1;
   }

   public int getCount(Class<?> p_335504_) {
      return this.getLastIdFor(p_335504_) + 1;
   }

   public int define(Class<?> p_334105_) {
      int i = this.getLastIdFor(p_334105_);
      int j = i == -1 ? 0 : i + 1;
      this.classToLastIdCache.put(p_334105_, j);
      return j;
   }
}
