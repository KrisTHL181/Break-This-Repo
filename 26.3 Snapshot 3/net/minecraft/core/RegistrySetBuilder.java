package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Lifecycle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.registries.BootstrapRegistry;
import net.minecraft.core.registries.EmptyTagLookupWrapper;
import net.minecraft.core.registries.MultiRegistryBootstrap;
import net.minecraft.core.registries.PatchedRegistry;
import net.minecraft.core.registries.SingleRegistryBootstrap;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

public class RegistrySetBuilder {
   private final List<RegistrySetBuilder.RegistryStub> entries = new ArrayList<>();

   private static RegistrySetBuilder.RegistryStub placeholderStub(final ResourceKey<? extends Registry<?>> key) {
      return new RegistrySetBuilder.RegistryStub() {
         @Override
         public Stream<ResourceKey<? extends Registry<?>>> requiredRegistries() {
            return Stream.of(key);
         }

         @Override
         public void apply(final RegistrySetBuilder.BuildState state) {
         }
      };
   }

   public <T> RegistrySetBuilder add(final ResourceKey<? extends Registry<T>> key, final SingleRegistryBootstrap<T> bootstrap) {
      this.entries.add(new RegistrySetBuilder.RegistryStub() {
         @Override
         public Stream<ResourceKey<? extends Registry<?>>> requiredRegistries() {
            return Stream.of(key);
         }

         @Override
         public void apply(final RegistrySetBuilder.BuildState state) {
            bootstrap.run(state.createBootstrap(key));
         }
      });
      return this;
   }

   public RegistrySetBuilder add(final MultiRegistryBootstrap bootstrap) {
      this.entries.add(new RegistrySetBuilder.RegistryStub() {
         @Override
         public Stream<ResourceKey<? extends Registry<?>>> requiredRegistries() {
            return bootstrap.requestedRegistries().stream();
         }

         @Override
         public void apply(final RegistrySetBuilder.BuildState state) {
            bootstrap.run(state::createBootstrap);
         }
      });
      return this;
   }

   private static HolderLookup.Provider buildProviderWithContext(
      final HolderLookup.Provider context, final Stream<? extends HolderLookup.RegistryLookup<?>> newRegistries
   ) {
      Map<ResourceKey<? extends Registry<?>>, HolderLookup.RegistryLookup<?>> lookups = new HashMap<>();
      context.listRegistries()
         .forEach(contextRegistry -> lookups.put(contextRegistry.key(), EmptyTagLookupWrapper.wrap((HolderLookup.RegistryLookup<?>)contextRegistry)));
      newRegistries.forEach(newRegistry -> lookups.put(newRegistry.key(), EmptyTagLookupWrapper.wrap((HolderLookup.RegistryLookup<?>)newRegistry)));
      return HolderLookup.Provider.create(lookups.values().stream());
   }

   public HolderLookup.Provider build(final HolderLookup.Provider context) {
      RegistrySetBuilder.BuildState state = RegistrySetBuilder.BuildState.createAndApply(context, this.entries);
      List<HolderLookup.RegistryLookup<?>> bootstrappedRegistries = new ArrayList<>(state.bootstrappedRegistries.size());

      for (RegistrySetBuilder.BootstrappedRegistryState<?> newRegistry : state.bootstrappedRegistries.values()) {
         newRegistry.bindHolders();
         newRegistry.freeze();
         newRegistry.errorOnMissingHolders(state);
         bootstrappedRegistries.add(newRegistry.registry());
      }

      state.throwOnError();
      return buildProviderWithContext(context, bootstrappedRegistries.stream());
   }

   private static Set<ResourceKey<? extends Registry<?>>> findRegistriesMissingFromPatch(
      final HolderLookup.Provider contextRegistries, final HolderLookup.Provider baseRegistries, final List<RegistrySetBuilder.RegistryStub> entries
   ) {
      Set<? extends ResourceKey<? extends Registry<?>>> existingKeys = Stream.concat(newRegistryKeys(entries.stream()), contextRegistries.listRegistryKeys())
         .collect(Collectors.toSet());
      return baseRegistries.listRegistryKeys().filter(e -> !existingKeys.contains(e)).collect(Collectors.toSet());
   }

   public RegistrySetBuilder.PatchedRegistries buildPatch(
      final HolderLookup.Provider context, final HolderLookup.Provider fallbackProvider, final Cloner.Factory clonerFactory
   ) {
      Set<ResourceKey<? extends Registry<?>>> missingFromPatch = findRegistriesMissingFromPatch(context, fallbackProvider, this.entries);
      List<RegistrySetBuilder.RegistryStub> expandedEntries = Stream.concat(
            this.entries.stream(), missingFromPatch.stream().map(RegistrySetBuilder::placeholderStub)
         )
         .toList();
      RegistrySetBuilder.BuildState state = RegistrySetBuilder.BuildState.createAndApply(context, expandedEntries);
      List<HolderLookup.RegistryLookup<?>> bootstrappedRegistries = new ArrayList<>(state.bootstrappedRegistries.size());

      for (RegistrySetBuilder.BootstrappedRegistryState<?> newRegistry : state.bootstrappedRegistries.values()) {
         newRegistry.bindHolders();
         newRegistry.validatePatchHolders(state, fallbackProvider);
         newRegistry.freeze();
         bootstrappedRegistries.add(newRegistry.registry());
      }

      HolderLookup.Provider patchOnlyRegistries = buildProviderWithContext(context, bootstrappedRegistries.stream());
      state.throwOnError();
      HolderLookup.Provider fullPatchedRegistries = EmptyTagLookupWrapper.wrap(
         PatchedRegistry.applyPatches(context, fallbackProvider, patchOnlyRegistries, clonerFactory, state.bootstrappedRegistries.keySet())
      );
      return new RegistrySetBuilder.PatchedRegistries(fullPatchedRegistries, patchOnlyRegistries);
   }

   private static <T> ResourceKey<? extends Registry<T>> eyerollCast(final ResourceKey<? extends Registry<? extends T>> registryKey) {
      return (ResourceKey<? extends Registry<T>>)registryKey;
   }

   private static Stream<ResourceKey<? extends Registry<?>>> newRegistryKeys(final Stream<RegistrySetBuilder.RegistryStub> entries) {
      return entries.flatMap(RegistrySetBuilder.RegistryStub::requiredRegistries).distinct();
   }

   private record BootstrappedRegistryState<T>(BootstrapRegistry<T> registry, Map<ResourceKey<T>, T> registeredValues) {
      public static <T> RegistrySetBuilder.BootstrappedRegistryState<T> create(final ResourceKey<? extends Registry<T>> key, final Lifecycle lifecycle) {
         BootstrapRegistry<T> newRegistry = new BootstrapRegistry<>(key, lifecycle);
         return new RegistrySetBuilder.BootstrappedRegistryState<>(newRegistry, new HashMap<>());
      }

      public BootstrapContext<T> createBootstrapContext(final RegistrySetBuilder.BuildState state) {
         return new BootstrapContext<T>() {
            @Override
            public Holder.Reference<T> register(final ResourceKey<T> key, final T value) {
               T previousValue = BootstrappedRegistryState.this.registeredValues.put(key, value);
               if (previousValue != null) {
                  state.errors.add(new IllegalStateException("Duplicate registration for " + key + ", new=" + value + ", old=" + previousValue));
               }

               return BootstrappedRegistryState.this.registry.getOrThrow(key);
            }

            @Override
            public <S> HolderGetter<S> lookup(final ResourceKey<? extends Registry<? extends S>> key) {
               return state.allRegistries.lookupOrThrow(key);
            }

            @Override
            public <S> Stream<Holder.Reference<S>> listContextElements(final ResourceKey<? extends Registry<? extends S>> key) {
               return state.contextRegistries.lookupOrThrow(key).listElements();
            }
         };
      }

      public void bindHolders() {
         this.registeredValues.forEach((key, value) -> this.registry.getOrThrow((ResourceKey<T>)key).bindValue((T)value));
      }

      public void freeze() {
         this.registry.freeze();
      }

      public void errorOnMissingHolders(final RegistrySetBuilder.BuildState state) {
         this.registry.listElements().forEach(element -> {
            if (!element.isBound()) {
               state.errors().add(new IllegalStateException("No value registered for key " + element.key().identifier()));
            }
         });
      }

      public void validatePatchHolders(final RegistrySetBuilder.BuildState state, final HolderLookup.Provider fallback) {
         HolderLookup<T> baseRegistry = fallback.lookupOrThrow(this.registry.key());
         this.registry.removeIf(element -> {
            if (element.isBound()) {
               return false;
            }

            if (baseRegistry.get(element.key()).isEmpty()) {
               state.errors().add(new IllegalStateException("Value " + element.key().identifier() + " referenced by patched element is not present in base"));
            }

            return true;
         });
      }
   }

   private record BuildState(
      HolderLookup.Provider contextRegistries,
      HolderLookup.Provider allRegistries,
      Map<ResourceKey<? extends Registry<?>>, RegistrySetBuilder.BootstrappedRegistryState<?>> bootstrappedRegistries,
      List<RuntimeException> errors
   ) {
      public static RegistrySetBuilder.BuildState createAndApply(final HolderLookup.Provider context, final List<RegistrySetBuilder.RegistryStub> entries) {
         RegistrySetBuilder.BuildState state = create(context, entries);
         entries.forEach(e -> e.apply(state));
         return state;
      }

      private static RegistrySetBuilder.BuildState create(final HolderLookup.Provider context, final List<RegistrySetBuilder.RegistryStub> entries) {
         List<RuntimeException> errors = new ArrayList<>();
         Builder<ResourceKey<? extends Registry<?>>, HolderLookup.RegistryLookup<?>> allRegistries = ImmutableMap.builder();
         Builder<ResourceKey<? extends Registry<?>>, RegistrySetBuilder.BootstrappedRegistryState<?>> bootstrappedRegistries = ImmutableMap.builder();
         context.listRegistries()
            .forEach(contextRegistry -> allRegistries.put(contextRegistry.key(), EmptyTagLookupWrapper.wrap((HolderLookup.RegistryLookup<?>)contextRegistry)));
         RegistrySetBuilder.newRegistryKeys(entries.stream())
            .forEach(
               newRegistryKey -> {
                  RegistrySetBuilder.BootstrappedRegistryState<?> newRegistryEntry = RegistrySetBuilder.BootstrappedRegistryState.create(
                     RegistrySetBuilder.eyerollCast((ResourceKey<? extends Registry<?>>)newRegistryKey), Lifecycle.stable()
                  );
                  BootstrapRegistry<?> newRegistry = newRegistryEntry.registry();
                  bootstrappedRegistries.put(newRegistry.key(), newRegistryEntry);
                  allRegistries.put(newRegistry.key(), newRegistry);
               }
            );
         return new RegistrySetBuilder.BuildState(
            context, HolderLookup.Provider.create(allRegistries.build().values().stream()), bootstrappedRegistries.build(), errors
         );
      }

      public void throwOnError() {
         if (!this.errors.isEmpty()) {
            IllegalStateException result = new IllegalStateException("Errors during registry creation");

            for (RuntimeException error : this.errors) {
               result.addSuppressed(error);
            }

            throw result;
         }
      }

      public <T> BootstrapContext<T> createBootstrap(final ResourceKey<? extends Registry<T>> key) {
         RegistrySetBuilder.BootstrappedRegistryState<T> targetRegistry = Objects.requireNonNull(
            (RegistrySetBuilder.BootstrappedRegistryState<T>)this.bootstrappedRegistries.get(key), () -> "No registry named " + key.identifier()
         );
         return targetRegistry.createBootstrapContext(this);
      }
   }

   public record PatchedRegistries(HolderLookup.Provider full, HolderLookup.Provider patches) {
   }

   private interface RegistryStub {
      Stream<ResourceKey<? extends Registry<?>>> requiredRegistries();

      void apply(RegistrySetBuilder.BuildState state);
   }
}
