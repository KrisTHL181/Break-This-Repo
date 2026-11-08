package net.minecraft.resources;

@FunctionalInterface
public interface DependantName<T, V> {
   V get(ResourceKey<T> id);

   static <T, V> DependantName<T, V> fixed(final V value) {
      return id -> value;
   }
}
