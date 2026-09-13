package net.minecraft.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public class ByIdMap {
   private static <T> IntFunction<T> createMap(final ToIntFunction<T> idGetter, final T[] values) {
      if (values.length == 0) {
         throw new IllegalArgumentException("Empty value list");
      }

      Int2ObjectMap<T> result = new Int2ObjectOpenHashMap();

      for (T value : values) {
         int id = idGetter.applyAsInt(value);
         T previous = (T)result.put(id, value);
         if (previous != null) {
            throw new IllegalArgumentException("Duplicate entry on id " + id + ": current=" + value + ", previous=" + previous);
         }
      }

      return result;
   }

   public static <T> IntFunction<T> sparse(final ToIntFunction<T> idGetter, final T[] values, final T _default) {
      IntFunction<T> idToObject = createMap(idGetter, values);
      return id -> Objects.requireNonNullElse(idToObject.apply(id), _default);
   }

   private static <T> T[] createSortedArray(final ToIntFunction<T> idGetter, final T[] values) {
      int length = values.length;
      if (length == 0) {
         throw new IllegalArgumentException("Empty value list");
      }

      T[] result = (T[])values.clone();
      Arrays.fill(result, null);

      for (T value : values) {
         int id = idGetter.applyAsInt(value);
         if (id < 0 || id >= length) {
            throw new IllegalArgumentException("Values are not continous, found index " + id + " for value " + value);
         }

         T previous = result[id];
         if (previous != null) {
            throw new IllegalArgumentException("Duplicate entry on id " + id + ": current=" + value + ", previous=" + previous);
         }

         result[id] = value;
      }

      for (int i = 0; i < length; i++) {
         if (result[i] == null) {
            throw new IllegalArgumentException("Missing value at index: " + i);
         }
      }

      return result;
   }

   public static <T> IntFunction<T> continuous(final ToIntFunction<T> idGetter, final T[] values, final ByIdMap.OutOfBoundsStrategy strategy) {
      T[] sortedValues = createSortedArray(idGetter, values);
      int length = sortedValues.length;

      return switch (strategy) {
         case ZERO -> {
            T zeroValue = sortedValues[0];
            yield id -> id >= 0 && id < length ? sortedValues[id] : zeroValue;
         }
         case WRAP -> id -> sortedValues[Mth.positiveModulo(id, length)];
         case CLAMP -> id -> sortedValues[Mth.clamp(id, 0, length - 1)];
      };
   }

   public enum OutOfBoundsStrategy {
      ZERO,
      WRAP,
      CLAMP;
   }
}
