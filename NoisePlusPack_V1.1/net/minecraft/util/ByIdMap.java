package net.minecraft.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public class ByIdMap {
   private static <T> IntFunction<T> createMap(ToIntFunction<T> p_263047_, T[] p_263043_) {
      if (p_263043_.length == 0) {
         throw new IllegalArgumentException("Empty value list");
      }

      Int2ObjectMap<T> int2objectmap = new Int2ObjectOpenHashMap();

      for (T t : p_263043_) {
         int i = p_263047_.applyAsInt(t);
         T t1 = (T)int2objectmap.put(i, t);
         if (t1 != null) {
            throw new IllegalArgumentException("Duplicate entry on id " + i + ": current=" + t + ", previous=" + t1);
         }
      }

      return int2objectmap;
   }

   public static <T> IntFunction<T> sparse(ToIntFunction<T> p_262952_, T[] p_263085_, T p_262981_) {
      IntFunction<T> intfunction = createMap(p_262952_, p_263085_);
      return p_262932_ -> Objects.requireNonNullElse(intfunction.apply(p_262932_), p_262981_);
   }

   private static <T> T[] createSortedArray(ToIntFunction<T> p_262976_, T[] p_263053_) {
      int i = p_263053_.length;
      if (i == 0) {
         throw new IllegalArgumentException("Empty value list");
      }

      T[] at = (T[])p_263053_.clone();
      Arrays.fill(at, null);

      for (T t : p_263053_) {
         int j = p_262976_.applyAsInt(t);
         if (j < 0 || j >= i) {
            throw new IllegalArgumentException("Values are not continous, found index " + j + " for value " + t);
         }

         T t1 = at[j];
         if (t1 != null) {
            throw new IllegalArgumentException("Duplicate entry on id " + j + ": current=" + t + ", previous=" + t1);
         }

         at[j] = t;
      }

      for (int k = 0; k < i; k++) {
         if (at[k] == null) {
            throw new IllegalArgumentException("Missing value at index: " + k);
         }
      }

      return at;
   }

   public static <T> IntFunction<T> continuous(ToIntFunction<T> p_263112_, T[] p_262975_, ByIdMap.OutOfBoundsStrategy p_263075_) {
      T[] at = createSortedArray(p_263112_, p_262975_);
      int i = at.length;

      return switch (p_263075_) {
         case ZERO -> {
            T t = at[0];
            yield p_262927_ -> p_262927_ >= 0 && p_262927_ < i ? at[p_262927_] : t;
         }
         case WRAP -> p_262977_ -> at[Mth.positiveModulo(p_262977_, i)];
         case CLAMP -> p_263013_ -> at[Mth.clamp(p_263013_, 0, i - 1)];
      };
   }

   public enum OutOfBoundsStrategy {
      ZERO,
      WRAP,
      CLAMP;
   }
}
