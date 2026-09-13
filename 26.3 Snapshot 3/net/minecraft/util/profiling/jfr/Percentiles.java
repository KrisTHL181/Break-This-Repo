package net.minecraft.util.profiling.jfr;

import com.google.common.math.Quantiles;
import com.google.common.math.Quantiles.ScaleAndIndexes;
import it.unimi.dsi.fastutil.ints.Int2DoubleRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleSortedMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleSortedMaps;
import java.util.Comparator;
import java.util.Map;
import net.minecraft.util.Util;

public class Percentiles {
   public static final ScaleAndIndexes DEFAULT_INDEXES = Quantiles.scale(100).indexes(new int[]{50, 75, 90, 99});

   private Percentiles() {
   }

   public static Map<Integer, Double> evaluate(final long[] dataset) {
      return dataset.length == 0 ? Map.of() : sorted(DEFAULT_INDEXES.compute(dataset));
   }

   public static Map<Integer, Double> evaluate(final int[] dataset) {
      return dataset.length == 0 ? Map.of() : sorted(DEFAULT_INDEXES.compute(dataset));
   }

   public static Map<Integer, Double> evaluate(final double[] dataset) {
      return dataset.length == 0 ? Map.of() : sorted(DEFAULT_INDEXES.compute(dataset));
   }

   private static Map<Integer, Double> sorted(final Map<Integer, Double> percentiles) {
      Int2DoubleSortedMap sorted = Util.make(new Int2DoubleRBTreeMap(Comparator.reverseOrder()), it -> it.putAll(percentiles));
      return Int2DoubleSortedMaps.unmodifiable(sorted);
   }
}
