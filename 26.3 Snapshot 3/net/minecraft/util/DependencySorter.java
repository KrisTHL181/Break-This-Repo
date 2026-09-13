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

   public DependencySorter<K, V> addEntry(final K id, final V value) {
      this.contents.put(id, value);
      return this;
   }

   private void visitDependenciesAndElement(final Multimap<K, K> dependencies, final Set<K> alreadyVisited, final K id, final BiConsumer<K, V> output) {
      if (alreadyVisited.add(id)) {
         dependencies.get(id).forEach(dependency -> this.visitDependenciesAndElement(dependencies, alreadyVisited, (K)dependency, output));
         V current = this.contents.get(id);
         if (current != null) {
            output.accept(id, current);
         }
      }
   }

   private static <K> boolean isCyclic(final Multimap<K, K> directDependencies, final K from, final K to) {
      Collection<K> dependencies = directDependencies.get(to);
      return dependencies.contains(from) ? true : dependencies.stream().anyMatch(dep -> isCyclic(directDependencies, from, (K)dep));
   }

   private static <K> void addDependencyIfNotCyclic(final Multimap<K, K> directDependencies, final K from, final K to) {
      if (!isCyclic(directDependencies, from, to)) {
         directDependencies.put(from, to);
      }
   }

   public void orderByDependencies(final BiConsumer<K, V> output) {
      Multimap<K, K> directDependencies = HashMultimap.create();
      this.contents.forEach((id, value) -> value.visitRequiredDependencies(dep -> addDependencyIfNotCyclic(directDependencies, (K)id, dep)));
      this.contents.forEach((id, value) -> value.visitOptionalDependencies(dep -> addDependencyIfNotCyclic(directDependencies, (K)id, dep)));
      Set<K> alreadyVisited = new HashSet<>();
      this.contents.keySet().forEach(topId -> this.visitDependenciesAndElement(directDependencies, alreadyVisited, (K)topId, output));
   }

   public interface Entry<K> {
      void visitRequiredDependencies(final Consumer<K> output);

      void visitOptionalDependencies(final Consumer<K> output);
   }
}
