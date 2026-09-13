package net.minecraft.core.registries;

import com.mojang.serialization.Lifecycle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Cloner;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public class PatchedRegistry<T> implements HolderLookup.RegistryLookup<T> {
   private final ResourceKey<? extends Registry<? extends T>> key;
   private final Lifecycle lifecycle;
   private final Map<ResourceKey<T>, Holder.Reference<T>> entries = new HashMap<>();

   private PatchedRegistry(final ResourceKey<? extends Registry<? extends T>> key, final Lifecycle lifecycle) {
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

   @Override
   public Stream<Holder.Reference<T>> listElements() {
      return this.entries.values().stream();
   }

   @Override
   public Optional<Holder.Reference<T>> get(final ResourceKey<T> id) {
      return Optional.ofNullable(this.entries.get(id));
   }

   @Override
   public Stream<HolderSet.Named<T>> listTags() {
      throw new UnsupportedOperationException("Tags cloning is not supported");
   }

   @Override
   public Optional<HolderSet.Named<T>> get(final TagKey<T> id) {
      return Optional.of(HolderSet.emptyNamed(this, id));
   }

   private static <T> HolderLookup.RegistryLookup<T> createLazyFullPatchedRegistries(
      final Cloner.Factory clonerFactory,
      final ResourceKey<? extends Registry<? extends T>> registryKey,
      final HolderLookup.Provider baseProvider,
      final HolderLookup.Provider patchProvider,
      final MutableObject<HolderLookup.Provider> clonedRegistriesProvider
   ) {
      Cloner<T> cloner = clonerFactory.cloner(registryKey);
      if (cloner == null) {
         throw new NullPointerException("No cloner for " + registryKey.identifier());
      }

      HolderLookup.RegistryLookup<T> patchContents = patchProvider.lookupOrThrow(registryKey);
      HolderLookup.RegistryLookup<T> baseContents = baseProvider.lookupOrThrow(registryKey);
      Lifecycle lifecycle = patchContents.registryLifecycle().add(baseContents.registryLifecycle());
      PatchedRegistry<T> result = new PatchedRegistry<>(registryKey, lifecycle);
      patchContents.listElements().forEach(elementHolder -> {
         ResourceKey<T> elementKey = elementHolder.key();
         PatchedRegistry.LazyHolder<T> holder = new PatchedRegistry.LazyHolder<>(result, elementKey);
         holder.supplier = () -> cloner.clone((T)elementHolder.value(), patchProvider, (HolderLookup.Provider)clonedRegistriesProvider.get());
         result.entries.put(elementKey, holder);
      });
      baseContents.listElements().forEach(elementHolder -> {
         ResourceKey<T> elementKey = elementHolder.key();
         result.entries.computeIfAbsent(elementKey, key -> {
            PatchedRegistry.LazyHolder<T> holder = new PatchedRegistry.LazyHolder<>(result, elementKey);
            holder.supplier = () -> cloner.clone((T)elementHolder.value(), baseProvider, (HolderLookup.Provider)clonedRegistriesProvider.get());
            return holder;
         });
      });
      return result;
   }

   public static HolderLookup.Provider applyPatches(
      final HolderLookup.Provider context,
      final HolderLookup.Provider baseRegistries,
      final HolderLookup.Provider patchRegistries,
      final Cloner.Factory clonerFactory,
      final Set<ResourceKey<? extends Registry<?>>> registriesToClone
   ) {
      MutableObject<HolderLookup.Provider> resultHolder = new MutableObject();
      List<HolderLookup.RegistryLookup<?>> lazyFullRegistries = registriesToClone.stream()
         .map(
            registryKey -> createLazyFullPatchedRegistries(
               clonerFactory, (ResourceKey<? extends Registry<?>>)registryKey, baseRegistries, patchRegistries, resultHolder
            )
         )
         .collect(Collectors.toUnmodifiableList());
      HolderLookup.Provider result = HolderLookup.Provider.create(Stream.concat(context.listRegistries(), lazyFullRegistries.stream()));
      resultHolder.setValue(result);
      return result;
   }

   private static class LazyHolder<T> extends Holder.Reference<T> {
      private @Nullable Supplier<T> supplier;

      protected LazyHolder(final HolderOwner<T> owner, final @Nullable ResourceKey<T> key) {
         super(Holder.Reference.Type.STAND_ALONE, owner, key, null);
      }

      @Override
      protected void bindValue(final T value) {
         super.bindValue(value);
         this.supplier = null;
      }

      @Override
      public T value() {
         if (this.supplier != null) {
            this.bindValue(this.supplier.get());
         }

         return super.value();
      }
   }
}
