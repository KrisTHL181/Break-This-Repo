package net.minecraft.core;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;

public interface HolderLookup<T> extends HolderGetter<T> {
   Stream<Holder.Reference<T>> listElements();

   default Stream<ResourceKey<T>> listElementIds() {
      return this.listElements().map(Holder.Reference::key);
   }

   Stream<HolderSet.Named<T>> listTags();

   default Stream<TagKey<T>> listTagIds() {
      return this.listTags().map(HolderSet.Named::key);
   }

   interface Provider extends HolderGetter.Provider {
      Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys();

      default Stream<HolderLookup.RegistryLookup<?>> listRegistries() {
         return this.listRegistryKeys().map(this::lookupOrThrow);
      }

      @Override
      <T> Optional<? extends HolderLookup.RegistryLookup<T>> lookup(final ResourceKey<? extends Registry<? extends T>> key);

      default <T> HolderLookup.RegistryLookup<T> lookupOrThrow(final ResourceKey<? extends Registry<? extends T>> key) {
         return this.lookup(key).orElseThrow(() -> new IllegalStateException("Registry " + key.identifier() + " not found"));
      }

      default <V> RegistryOps<V> createSerializationContext(final DynamicOps<V> parent) {
         return RegistryOps.create((DynamicOps<T>)parent, this);
      }

      static HolderLookup.Provider create(final Stream<HolderLookup.RegistryLookup<?>> lookups) {
         final Map<ResourceKey<? extends Registry<?>>, HolderLookup.RegistryLookup<?>> map = lookups.collect(
            Collectors.toUnmodifiableMap(HolderLookup.RegistryLookup::key, e -> e)
         );
         return new HolderLookup.Provider() {
            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
               return map.keySet().stream();
            }

            @Override
            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(final ResourceKey<? extends Registry<? extends T>> key) {
               return Optional.ofNullable((HolderLookup.RegistryLookup<T>)map.get(key));
            }
         };
      }

      default Lifecycle allRegistriesLifecycle() {
         return this.listRegistries().map(HolderLookup.RegistryLookup::registryLifecycle).reduce(Lifecycle.stable(), Lifecycle::add);
      }
   }

   interface RegistryLookup<T> extends HolderLookup<T> {
      ResourceKey<? extends Registry<? extends T>> key();

      Lifecycle registryLifecycle();

      default HolderLookup.RegistryLookup<T> filterFeatures(final FeatureFlagSet enabledFeatures) {
         return FeatureElement.FILTERED_REGISTRIES.contains(this.key()) ? this.filterElements(t -> ((FeatureElement)t).isEnabled(enabledFeatures)) : this;
      }

      default HolderLookup.RegistryLookup<T> filterElements(final Predicate<T> filter) {
         return new HolderLookup.RegistryLookup.Delegate<T>() {
            @Override
            public HolderLookup.RegistryLookup<T> parent() {
               return RegistryLookup.this;
            }

            @Override
            public Optional<Holder.Reference<T>> get(final ResourceKey<T> id) {
               return this.parent().get(id).filter(holder -> filter.test(holder.value()));
            }

            @Override
            public Stream<Holder.Reference<T>> listElements() {
               return this.parent().listElements().filter(e -> filter.test(e.value()));
            }
         };
      }

      interface Delegate<T> extends HolderLookup.RegistryLookup<T> {
         HolderLookup.RegistryLookup<T> parent();

         @Override
         default ResourceKey<? extends Registry<? extends T>> key() {
            return this.parent().key();
         }

         @Override
         default Lifecycle registryLifecycle() {
            return this.parent().registryLifecycle();
         }

         @Override
         default Optional<Holder.Reference<T>> get(final ResourceKey<T> id) {
            return this.parent().get(id);
         }

         @Override
         default Stream<Holder.Reference<T>> listElements() {
            return this.parent().listElements();
         }

         @Override
         default Optional<HolderSet.Named<T>> get(final TagKey<T> id) {
            return this.parent().get(id);
         }

         @Override
         default Stream<HolderSet.Named<T>> listTags() {
            return this.parent().listTags();
         }

         @Override
         default boolean canSerialize(final HolderOwner<T> owner) {
            return this.parent().canSerialize(owner);
         }
      }
   }
}
