package net.minecraft.core.registries;

import com.mojang.serialization.Lifecycle;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jspecify.annotations.Nullable;

public class BootstrapRegistry<T> implements HolderLookup.RegistryLookup<T> {
   private final ResourceKey<? extends Registry<T>> key;
   private final Lifecycle lifecycle;
   private BootstrapRegistry.Storage<T> storage = new BootstrapRegistry.RegistrationStorage();

   public BootstrapRegistry(final ResourceKey<? extends Registry<T>> key, final Lifecycle lifecycle) {
      this.key = key;
      this.lifecycle = lifecycle;
   }

   @Override
   public ResourceKey<? extends Registry<? extends T>> key() {
      return this.key;
   }

   @Override
   public Lifecycle registryLifecycle() {
      return this.lifecycle;
   }

   public void freeze() {
      this.storage = this.storage.freeze();
   }

   public void removeIf(final Predicate<Holder.Reference<T>> predicate) {
      this.storage.removeIf(predicate);
   }

   @Override
   public Optional<Holder.Reference<T>> get(final ResourceKey<T> id) {
      return Optional.ofNullable(this.storage.get(id));
   }

   @Override
   public Stream<Holder.Reference<T>> listElements() {
      return this.storage.listElements().stream();
   }

   @Override
   public Optional<HolderSet.Named<T>> get(final TagKey<T> id) {
      return Optional.ofNullable(this.storage.get(id));
   }

   @Override
   public Stream<HolderSet.Named<T>> listTags() {
      return this.storage.listTags().stream();
   }

   private static class FrozenStorage<T> implements BootstrapRegistry.Storage<T> {
      private final Map<ResourceKey<T>, Holder.Reference<T>> holders;
      private final Map<TagKey<T>, HolderSet.Named<T>> holderSets;

      private FrozenStorage(final Map<ResourceKey<T>, Holder.Reference<T>> holders, final Map<TagKey<T>, HolderSet.Named<T>> holderSets) {
         this.holders = Map.copyOf(holders);
         this.holderSets = Map.copyOf(holderSets);
      }

      @Override
      public BootstrapRegistry.Storage<T> freeze() {
         return this;
      }

      @Override
      public void removeIf(final Predicate<Holder.Reference<T>> predicate) {
         throw new UnsupportedOperationException("Registry is already frozen");
      }

      @Override
      public Holder.@Nullable Reference<T> get(final ResourceKey<T> id) {
         return this.holders.get(id);
      }

      @Override
      public Collection<Holder.Reference<T>> listElements() {
         return this.holders.values();
      }

      @Override
      public HolderSet.@Nullable Named<T> get(final TagKey<T> id) {
         return this.holderSets.get(id);
      }

      @Override
      public Collection<HolderSet.Named<T>> listTags() {
         return this.holderSets.values();
      }
   }

   private class RegistrationStorage implements BootstrapRegistry.Storage<T> {
      private final Map<ResourceKey<T>, Holder.Reference<T>> holders = new HashMap<>();
      private final Map<TagKey<T>, HolderSet.Named<T>> holderSets = new HashMap<>();

      @Override
      public BootstrapRegistry.Storage<T> freeze() {
         return new BootstrapRegistry.FrozenStorage<>(this.holders, this.holderSets);
      }

      @Override
      public void removeIf(final Predicate<Holder.Reference<T>> predicate) {
         this.holders.values().removeIf(predicate);
      }

      @Override
      public Holder.Reference<T> get(final ResourceKey<T> id) {
         return this.holders.computeIfAbsent(id, key -> Holder.Reference.createStandAlone(BootstrapRegistry.this, (ResourceKey<T>)key));
      }

      @Override
      public Collection<Holder.Reference<T>> listElements() {
         throw new UnsupportedOperationException("List is not available during bootstrap");
      }

      @Override
      public HolderSet.Named<T> get(final TagKey<T> id) {
         return this.holderSets.computeIfAbsent(id, key -> HolderSet.emptyNamed(BootstrapRegistry.this, (TagKey<T>)key));
      }

      @Override
      public Collection<HolderSet.Named<T>> listTags() {
         throw new UnsupportedOperationException("List is not available during bootstrap");
      }
   }

   private interface Storage<T> {
      BootstrapRegistry.Storage<T> freeze();

      void removeIf(Predicate<Holder.Reference<T>> predicate);

      Holder.@Nullable Reference<T> get(ResourceKey<T> id);

      Collection<Holder.Reference<T>> listElements();

      HolderSet.@Nullable Named<T> get(TagKey<T> id);

      Collection<HolderSet.Named<T>> listTags();
   }
}
