package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.Graph;
import net.minecraft.util.Util;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.apache.commons.lang3.mutable.MutableInt;

public class FeatureSorter {
   public static <T> List<FeatureSorter.StepFeatureData> buildFeaturesPerStep(
      List<T> p_220604_, Function<T, List<HolderSet<PlacedFeature>>> p_220605_, boolean p_220606_
   ) {
      Object2IntMap<PlacedFeature> object2intmap = new Object2IntOpenHashMap();
      MutableInt mutableint = new MutableInt(0);

      record FeatureData(int featureIndex, int step, PlacedFeature feature) {
      }

      Comparator<FeatureData> comparator = Comparator.comparingInt(FeatureData::step).thenComparingInt(FeatureData::featureIndex);
      Map<FeatureData, Set<FeatureData>> map = new TreeMap<>(comparator);
      int i = 0;

      for (T t : p_220604_) {
         List<FeatureData> list = Lists.newArrayList();
         List<HolderSet<PlacedFeature>> list1 = p_220605_.apply(t);
         i = Math.max(i, list1.size());

         for (int j = 0; j < list1.size(); j++) {
            for (Holder<PlacedFeature> holder : list1.get(j)) {
               PlacedFeature placedfeature = holder.value();
               list.add(new FeatureData(object2intmap.computeIfAbsent(placedfeature, p_220609_ -> mutableint.getAndIncrement()), j, placedfeature));
            }
         }

         for (int k = 0; k < list.size(); k++) {
            Set<FeatureData> set2 = map.computeIfAbsent(list.get(k), p_220602_ -> new TreeSet<>(comparator));
            if (k < list.size() - 1) {
               set2.add(list.get(k + 1));
            }
         }
      }

      Set<FeatureData> set = new TreeSet<>(comparator);
      Set<FeatureData> set1 = new TreeSet<>(comparator);
      List<FeatureData> list2 = Lists.newArrayList();

      for (FeatureData featuresorter$1featuredata : map.keySet()) {
         if (!set1.isEmpty()) {
            throw new IllegalStateException("You somehow broke the universe; DFS bork (iteration finished with non-empty in-progress vertex set");
         }

         if (!set.contains(featuresorter$1featuredata) && Graph.depthFirstSearch(map, set, set1, list2::add, featuresorter$1featuredata)) {
            if (!p_220606_) {
               throw new IllegalStateException("Feature order cycle found");
            }

            List<T> list3 = new ArrayList<>(p_220604_);

            int j1;
            do {
               j1 = list3.size();
               ListIterator<T> listiterator = list3.listIterator();

               while (listiterator.hasNext()) {
                  T t1 = listiterator.next();
                  listiterator.remove();

                  try {
                     buildFeaturesPerStep(list3, p_220605_, false);
                  } catch (IllegalStateException illegalstateexception) {
                     continue;
                  }

                  listiterator.add(t1);
               }
            } while (j1 != list3.size());

            throw new IllegalStateException("Feature order cycle found, involved sources: " + list3);
         }
      }

      Collections.reverse(list2);
      Builder<FeatureSorter.StepFeatureData> builder = ImmutableList.builder();

      for (int l = 0; l < i; l++) {
         int i1 = l;
         List<PlacedFeature> list4 = list2.stream().filter(p_220599_ -> p_220599_.step() == i1).map(FeatureData::feature).collect(Collectors.toList());
         builder.add(new FeatureSorter.StepFeatureData(list4));
      }

      return builder.build();
   }

   public record StepFeatureData(List<PlacedFeature> features, ToIntFunction<PlacedFeature> indexMapping) {
      StepFeatureData(List<PlacedFeature> p_220627_) {
         this(p_220627_, Util.createIndexIdentityLookup(p_220627_));
      }
   }
}
