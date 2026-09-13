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

   public LayeredRegistryAccess(List<T> p_251225_) {
      this(p_251225_, Util.make(() -> {
         RegistryAccess.Frozen[] aregistryaccess$frozen = new RegistryAccess.Frozen[p_251225_.size()];
         Arrays.fill(aregistryaccess$frozen, RegistryAccess.EMPTY);
         return Arrays.asList(aregistryaccess$frozen);
      }));
   }

   private LayeredRegistryAccess(List<T> p_250473_, List<RegistryAccess.Frozen> p_249320_) {
      this.keys = List.copyOf(p_250473_);
      this.values = List.copyOf(p_249320_);
      this.composite = new RegistryAccess.ImmutableRegistryAccess(collectRegistries(p_249320_.stream())).freeze();
   }

   private int getLayerIndexOrThrow(T p_250144_) {
      int i = this.keys.indexOf(p_250144_);
      if (i == -1) {
         throw new IllegalStateException("Can't find " + p_250144_ + " inside " + this.keys);
      } else {
         return i;
      }
   }

   public RegistryAccess.Frozen getLayer(T p_250826_) {
      int i = this.getLayerIndexOrThrow(p_250826_);
      return this.values.get(i);
   }

   public RegistryAccess.Frozen getAccessForLoading(T p_251335_) {
      int i = this.getLayerIndexOrThrow(p_251335_);
      return this.getCompositeAccessForLayers(0, i);
   }

   public RegistryAccess.Frozen getAccessFrom(T p_250766_) {
      int i = this.getLayerIndexOrThrow(p_250766_);
      return this.getCompositeAccessForLayers(i, this.values.size());
   }

   private RegistryAccess.Frozen getCompositeAccessForLayers(int p_251526_, int p_251999_) {
      return new RegistryAccess.ImmutableRegistryAccess(collectRegistries(this.values.subList(p_251526_, p_251999_).stream())).freeze();
   }

   public LayeredRegistryAccess<T> replaceFrom(T p_252104_, RegistryAccess.Frozen... p_250492_) {
      return this.replaceFrom(p_252104_, Arrays.asList(p_250492_));
   }

   public LayeredRegistryAccess<T> replaceFrom(T p_249539_, List<RegistryAccess.Frozen> p_250124_) {
      int i = this.getLayerIndexOrThrow(p_249539_);
      if (p_250124_.size() > this.values.size() - i) {
         throw new IllegalStateException("Too many values to replace");
      }

      List<RegistryAccess.Frozen> list = new ArrayList<>();

      for (int j = 0; j < i; j++) {
         list.add(this.values.get(j));
      }

      list.addAll(p_250124_);

      while (list.size() < this.values.size()) {
         list.add(RegistryAccess.EMPTY);
      }

      return new LayeredRegistryAccess<>(this.keys, list);
   }

   public RegistryAccess.Frozen compositeAccess() {
      return this.composite;
   }

   private static Map<ResourceKey<? extends Registry<?>>, Registry<?>> collectRegistries(Stream<? extends RegistryAccess> p_248595_) {
      Map<ResourceKey<? extends Registry<?>>, Registry<?>> map = new HashMap<>();
      p_248595_.forEach(p_252003_ -> p_252003_.registries().forEach(p_325671_ -> {
         if (map.put(p_325671_.key(), p_325671_.value()) != null) {
            throw new IllegalStateException("Duplicated registry " + p_325671_.key());
         }
      }));
      return map;
   }
}
