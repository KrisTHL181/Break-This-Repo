package net.minecraft.core.component;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class DataComponentInitializers {
   private final List<DataComponentInitializers.InitializerEntry<?>> initializers = new ArrayList<>();

   public <T> void add(final ResourceKey<T> key, final DataComponentInitializers.Initializer<T> initializer) {
      this.initializers.add(new DataComponentInitializers.InitializerEntry<>(key, initializer));
   }

   private Map<ResourceKey<?>, DataComponentMap.Builder> runInitializers(final HolderLookup.Provider context) {
      Map<ResourceKey<?>, DataComponentMap.Builder> results = new HashMap<>();

      for (DataComponentInitializers.InitializerEntry<?> initializer : this.initializers) {
         DataComponentMap.Builder builder = results.computeIfAbsent(initializer.key, k -> DataComponentMap.builder());
         initializer.run(builder, context);
      }

      return results;
   }

   private static <T> void registryEmpty(
      final Map<ResourceKey<? extends Registry<?>>, DataComponentInitializers.PendingComponentBuilders<?>> buildersByRegistry,
      final ResourceKey<? extends Registry<? extends T>> registryKey
   ) {
      ResourceKey<? extends Registry<T>> registryKeyButSane = (ResourceKey<? extends Registry<T>>)registryKey;
      buildersByRegistry.put(registryKey, new DataComponentInitializers.PendingComponentBuilders<>(registryKeyButSane, new HashMap<>()));
   }

   private static <T> void addBuilder(
      final Map<ResourceKey<? extends Registry<?>>, DataComponentInitializers.PendingComponentBuilders<?>> buildersByRegistry,
      final ResourceKey<T> key,
      final DataComponentMap.Builder builder
   ) {
      DataComponentInitializers.PendingComponentBuilders<T> buildersForRegistry = (DataComponentInitializers.PendingComponentBuilders<T>)buildersByRegistry.get(
         key.registryKey()
      );
      buildersForRegistry.builders.put(key, builder);
   }

   public List<DataComponentInitializers.PendingComponents<?>> build(final HolderLookup.Provider context) {
      Map<ResourceKey<? extends Registry<?>>, DataComponentInitializers.PendingComponentBuilders<?>> buildersByRegistry = new HashMap<>();
      context.listRegistryKeys().forEach(registryKey -> registryEmpty(buildersByRegistry, (ResourceKey<? extends Registry<?>>)registryKey));
      this.runInitializers(context).forEach((key, builder) -> addBuilder(buildersByRegistry, (ResourceKey<?>)key, builder));
      return buildersByRegistry.values()
         .stream()
         .map(elementBuilders -> createInitializerForRegistry(context, (DataComponentInitializers.PendingComponentBuilders<?>)elementBuilders))
         .collect(Collectors.toUnmodifiableList());
   }

   private static <T> DataComponentInitializers.PendingComponents<T> createInitializerForRegistry(
      final HolderLookup.Provider context, final DataComponentInitializers.PendingComponentBuilders<T> elementBuilders
   ) {
      final List<DataComponentInitializers.BakedEntry<T>> entries = new ArrayList<>();
      final ResourceKey<? extends Registry<T>> registryKey = elementBuilders.registryKey;
      HolderLookup.RegistryLookup<T> registry = context.lookupOrThrow(registryKey);
      Set<Holder.Reference<T>> elementsWithComponents = Sets.newIdentityHashSet();
      elementBuilders.builders.forEach((elementKey, elementBuilder) -> {
         Holder.Reference<T> element = registry.getOrThrow((ResourceKey<T>)elementKey);
         DataComponentMap components = elementBuilder.build();
         entries.add(new DataComponentInitializers.BakedEntry<>(element, components));
         elementsWithComponents.add(element);
      });
      registry.listElements()
         .filter(e -> !elementsWithComponents.contains(e))
         .forEach(
            elementWithoutComponents -> entries.add(
               new DataComponentInitializers.BakedEntry<>((Holder.Reference<T>)elementWithoutComponents, DataComponentMap.EMPTY)
            )
         );
      return new DataComponentInitializers.PendingComponents<T>() {
         @Override
         public ResourceKey<? extends Registry<? extends T>> key() {
            return registryKey;
         }

         @Override
         public void forEach(final BiConsumer<Holder.Reference<T>, DataComponentMap> output) {
            entries.forEach(e -> output.accept(e.element, e.components));
         }

         @Override
         public void apply() {
            entries.forEach(DataComponentInitializers.BakedEntry::apply);
         }
      };
   }

   private record BakedEntry<T>(Holder.Reference<T> element, DataComponentMap components) {
      public void apply() {
         this.element.bindComponents(this.components);
      }
   }

   @FunctionalInterface
   public interface Initializer<T> {
      void run(DataComponentMap.Builder components, HolderLookup.Provider context, ResourceKey<T> key);

      default DataComponentInitializers.Initializer<T> andThen(final DataComponentInitializers.Initializer<T> other) {
         return (components, context, key) -> {
            this.run(components, context, key);
            other.run(components, context, key);
         };
      }

      default <C> DataComponentInitializers.Initializer<T> add(final DataComponentType<C> type, final C value) {
         return this.andThen((components, context, key) -> components.set(type, value));
      }
   }

   private record InitializerEntry<T>(ResourceKey<T> key, DataComponentInitializers.Initializer<T> initializer) {
      public void run(final DataComponentMap.Builder components, final HolderLookup.Provider context) {
         this.initializer.run(components, context, this.key);
      }
   }

   private record PendingComponentBuilders<T>(ResourceKey<? extends Registry<T>> registryKey, Map<ResourceKey<T>, DataComponentMap.Builder> builders) {
   }

   public interface PendingComponents<T> {
      ResourceKey<? extends Registry<? extends T>> key();

      void forEach(BiConsumer<Holder.Reference<T>, DataComponentMap> output);

      void apply();
   }

   @FunctionalInterface
   public interface SingleComponentInitializer<C> {
      C create(HolderLookup.Provider context);

      default <T> DataComponentInitializers.Initializer<T> asInitializer(final DataComponentType<C> type) {
         return (components, context, key) -> components.set(type, this.create(context));
      }
   }
}
