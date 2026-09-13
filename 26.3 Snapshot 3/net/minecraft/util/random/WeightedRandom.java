package net.minecraft.util.random;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

public class WeightedRandom {
   private WeightedRandom() {
   }

   public static <T> int getTotalWeight(final List<T> items, final ToIntFunction<T> weightGetter) {
      long totalWeight = 0L;

      for (T item : items) {
         totalWeight += weightGetter.applyAsInt(item);
      }

      if (totalWeight > 2147483647L) {
         throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
      } else {
         return (int)totalWeight;
      }
   }

   public static <T> Optional<T> getRandomItem(final RandomSource random, final List<T> items, final int totalWeight, final ToIntFunction<T> weightGetter) {
      if (totalWeight < 0) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("Negative total weight in getRandomItem"));
      }

      if (totalWeight == 0) {
         return Optional.empty();
      }

      int selection = random.nextInt(totalWeight);
      return getWeightedItem(items, selection, weightGetter);
   }

   public static <T> Optional<T> getWeightedItem(final List<T> items, int index, final ToIntFunction<T> weightGetter) {
      for (T item : items) {
         index -= weightGetter.applyAsInt(item);
         if (index < 0) {
            return Optional.of(item);
         }
      }

      return Optional.empty();
   }

   public static <T> Optional<T> getRandomItem(final RandomSource random, final List<T> items, final ToIntFunction<T> weightGetter) {
      return getRandomItem(random, items, getTotalWeight(items, weightGetter), weightGetter);
   }
}
