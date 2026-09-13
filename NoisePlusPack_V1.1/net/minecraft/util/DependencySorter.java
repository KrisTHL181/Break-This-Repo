package net.minecraft.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DependencySorter<K, V extends DependencySorter.Entry<K>> {
   private final Map<K, V> contents = new HashMap<>();

   public DependencySorter<K, V> addEntry(K p_285256_, V p_285334_) {
      this.contents.put(p_285256_, p_285334_);
      return this;
   }

   private void visitDependenciesAndElement(Multimap<K, K> p_285183_, Set<K> p_285506_, K p_285108_, BiConsumer<K, V> p_285007_) {
      if (p_285506_.add(p_285108_)) {
         p_285183_.get(p_285108_).forEach(p_285443_ -> this.visitDependenciesAndElement(p_285183_, p_285506_, (K)p_285443_, p_285007_));
         V v = this.contents.get(p_285108_);
         if (v != null) {
            p_285007_.accept(p_285108_, v);
         }
      }
   }

   private static <K> boolean isCyclic(Multimap<K, K> p_285132_, K p_285324_, K p_285326_) {
      Collection<K> collection = p_285132_.get(p_285326_);
      return collection.contains(p_285324_) ? true : collection.stream().anyMatch(p_284974_ -> isCyclic(p_285132_, p_285324_, (K)p_284974_));
   }

   private static <K> void addDependencyIfNotCyclic(Multimap<K, K> p_285047_, K p_285148_, K p_285193_) {
      if (!isCyclic(p_285047_, p_285148_, p_285193_)) {
         p_285047_.put(p_285148_, p_285193_);
      }
   }

   public void orderByDependencies(BiConsumer<K, V> p_285438_) {
      Multimap<K, K> multimap = HashMultimap.create();
      this.contents
         .forEach((p_285415_, p_285018_) -> p_285018_.visitRequiredDependencies(p_285287_ -> addDependencyIfNotCyclic(multimap, (K)p_285415_, p_285287_)));
      this.contents
         .forEach((p_285462_, p_285526_) -> p_285526_.visitOptionalDependencies(p_285513_ -> addDependencyIfNotCyclic(multimap, (K)p_285462_, p_285513_)));
      Set<K> set = new HashSet<>();
      this.contents.keySet().forEach(p_284996_ -> this.visitDependenciesAndElement(multimap, set, (K)p_284996_, p_285438_));
   }

   public interface Entry<K> {
      void visitRequiredDependencies(Consumer<K> var1);

      void visitOptionalDependencies(Consumer<K> var1);
   }
}
