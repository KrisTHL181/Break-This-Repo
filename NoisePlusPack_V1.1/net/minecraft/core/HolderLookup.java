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
      <T> Optional<? extends HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1);

      default <T> HolderLookup.RegistryLookup<T> lookupOrThrow(ResourceKey<? extends Registry<? extends T>> p_255957_) {
         return this.lookup(p_255957_).orElseThrow(() -> new IllegalStateException("Registry " + p_255957_.identifier() + " not found"));
      }

      default <V> RegistryOps<V> createSerializationContext(DynamicOps<V> p_330698_) {
         return RegistryOps.create((DynamicOps<T>)p_330698_, this);
      }

      static HolderLookup.Provider create(Stream<HolderLookup.RegistryLookup<?>> p_256054_) {
         final Map<ResourceKey<? extends Registry<?>>, HolderLookup.RegistryLookup<?>> map = p_256054_.collect(
            Collectors.toUnmodifiableMap(HolderLookup.RegistryLookup::key, p_256335_ -> p_256335_)
         );
         return new HolderLookup.Provider() {
            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
               return map.keySet().stream();
            }

            @Override
            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_256379_) {
               return Optional.ofNullable((HolderLookup.RegistryLookup<T>)map.get(p_256379_));
            }
         };
      }

      default Lifecycle allRegistriesLifecycle() {
         return this.listRegistries().map(HolderLookup.RegistryLookup::registryLifecycle).reduce(Lifecycle.stable(), Lifecycle::add);
      }
   }

   interface RegistryLookup<T> extends HolderLookup<T>, HolderOwner<T> {
      ResourceKey<? extends Registry<? extends T>> key();

      Lifecycle registryLifecycle();

      default HolderLookup.RegistryLookup<T> filterFeatures(FeatureFlagSet p_249397_) {
         return FeatureElement.FILTERED_REGISTRIES.contains(this.key())
            ? this.filterElements(p_250240_ -> ((FeatureElement)p_250240_).isEnabled(p_249397_))
            : this;
      }

      default HolderLookup.RegistryLookup<T> filterElements(final Predicate<T> p_334671_) {
         return new HolderLookup.RegistryLookup.Delegate<T>() {
            @Override
            public HolderLookup.RegistryLookup<T> parent() {
               return RegistryLookup.this;
            }

            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> p_330384_) {
               return this.parent().get(p_330384_).filter(p_330697_ -> p_334671_.test(p_330697_.value()));
            }

            @Override
            public Stream<Holder.Reference<T>> listElements() {
               return this.parent().listElements().filter(p_331718_ -> p_334671_.test(p_331718_.value()));
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
         default Optional<Holder.Reference<T>> get(ResourceKey<T> p_255619_) {
            return this.parent().get(p_255619_);
         }

         @Override
         default Stream<Holder.Reference<T>> listElements() {
            return this.parent().listElements();
         }

         @Override
         default Optional<HolderSet.Named<T>> get(TagKey<T> p_256245_) {
            return this.parent().get(p_256245_);
         }

         @Override
         default Stream<HolderSet.Named<T>> listTags() {
            return this.parent().listTags();
         }
      }
   }
}
