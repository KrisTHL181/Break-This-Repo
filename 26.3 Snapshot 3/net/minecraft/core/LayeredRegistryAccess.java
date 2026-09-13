package net.minecraft.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;

public class LayeredRegistryAccess<T> {
   private final List<T> keys;
   private final List<RegistryAccess.Frozen> values;
   private final RegistryAccess.Frozen composite;

   public LayeredRegistryAccess(final List<T> keys) {
      this(keys, Util.make(() -> {
         RegistryAccess.Frozen[] layers = new RegistryAccess.Frozen[keys.size()];
         Arrays.fill(layers, RegistryAccess.EMPTY);
         return Arrays.asList(layers);
      }));
   }

   private LayeredRegistryAccess(final List<T> keys, final List<RegistryAccess.Frozen> values) {
      this.keys = List.copyOf(keys);
      this.values = List.copyOf(values);
      this.composite = new RegistryAccess.ImmutableRegistryAccess(collectRegistries(values.stream())).freeze();
   }

   private int getLayerIndexOrThrow(final T layer) {
      int index = this.keys.indexOf(layer);
      if (index == -1) {
         throw new IllegalStateException("Can't find " + layer + " inside " + this.keys);
      } else {
         return index;
      }
   }

   public RegistryAccess.Frozen getLayer(final T layer) {
      int index = this.getLayerIndexOrThrow(layer);
      return this.values.get(index);
   }

   public RegistryAccess.Frozen getAccessForLoading(final T forLayer) {
      int index = this.getLayerIndexOrThrow(forLayer);
      return this.getCompositeAccessForLayers(0, index);
   }

   public RegistryAccess.Frozen getAccessFrom(final T forLayer) {
      int index = this.getLayerIndexOrThrow(forLayer);
      return this.getCompositeAccessForLayers(index, this.values.size());
   }

   private RegistryAccess.Frozen getCompositeAccessForLayers(final int from, final int to) {
      return new RegistryAccess.ImmutableRegistryAccess(collectRegistries(this.values.subList(from, to).stream())).freeze();
   }

   public LayeredRegistryAccess<T> replaceFrom(final T fromLayer, final RegistryAccess.Frozen... layers) {
      return this.replaceFrom(fromLayer, Arrays.asList(layers));
   }

   public LayeredRegistryAccess<T> replaceFrom(final T fromLayer, final List<RegistryAccess.Frozen> layers) {
      int index = this.getLayerIndexOrThrow(fromLayer);
      if (layers.size() > this.values.size() - index) {
         throw new IllegalStateException("Too many values to replace");
      }

      List<RegistryAccess.Frozen> newValues = new ArrayList<>();

      for (int i = 0; i < index; i++) {
         newValues.add(this.values.get(i));
      }

      newValues.addAll(layers);

      while (newValues.size() < this.values.size()) {
         newValues.add(RegistryAccess.EMPTY);
      }

      return new LayeredRegistryAccess<>(this.keys, newValues);
   }

   public RegistryAccess.Frozen compositeAccess() {
      return this.composite;
   }

   private static Map<ResourceKey<? extends Registry<?>>, Registry<?>> collectRegistries(final Stream<? extends RegistryAccess> registries) {
      Map<ResourceKey<? extends Registry<?>>, Registry<?>> result = new HashMap<>();
      registries.forEach(access -> access.registries().forEach(e -> {
         if (result.put(e.key(), e.value()) != null) {
            throw new IllegalStateException("Duplicated registry " + e.key());
         }
      }));
      return result;
   }
}
