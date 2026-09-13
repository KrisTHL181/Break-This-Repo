package net.minecraft.data.registries;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;

public class RegistriesDatapackGenerator implements DataProvider {
   private final PackOutput output;
   private final String name;
   private final Collection<RegistryDataLoader.RegistryData<?>> registryData;
   private final CompletableFuture<HolderLookup.Provider> registries;

   public RegistriesDatapackGenerator(
      final PackOutput output,
      final String name,
      final Collection<RegistryDataLoader.RegistryData<?>> registryData,
      final CompletableFuture<HolderLookup.Provider> registryContents
   ) {
      this.name = name;
      this.registryData = registryData;
      this.registries = registryContents;
      this.output = output;
   }

   public static RegistriesDatapackGenerator forWorldLayer(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registryContents) {
      return new RegistriesDatapackGenerator(output, "world", RegistryDataLoader.WORLD_REGISTRIES, registryContents);
   }

   public static DataProvider forReloadableLayer(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registryContents) {
      return new RegistriesDatapackGenerator(output, "reloadable", RegistryDataLoader.RELOADABLE_REGISTRIES, registryContents);
   }

   @Override
   public CompletableFuture<?> run(final CachedOutput cache) {
      return this.registries
         .thenCompose(
            access -> {
               DynamicOps<JsonElement> registryOps = access.createSerializationContext(JsonOps.INSTANCE);
               return CompletableFuture.allOf(
                  this.registryData
                     .stream()
                     .flatMap(v -> this.dumpRegistryCap(cache, access, registryOps, (RegistryDataLoader.RegistryData<?>)v).stream())
                     .toArray(CompletableFuture[]::new)
               );
            }
         );
   }

   private <T> Optional<CompletableFuture<?>> dumpRegistryCap(
      final CachedOutput cache, final HolderLookup.Provider registries, final DynamicOps<JsonElement> writeOps, final RegistryDataLoader.RegistryData<T> v
   ) {
      ResourceKey<? extends Registry<T>> registryKey = v.key();
      return registries.lookup(registryKey)
         .map(
            registry -> {
               PackOutput.PathProvider pathProvider = this.output.createRegistryElementsPathProvider(registryKey);
               return CompletableFuture.allOf(
                  registry.listElements()
                     .map(e -> dumpValue(pathProvider.json(e.key().identifier()), cache, writeOps, v.elementCodec(), e.value()))
                     .toArray(CompletableFuture[]::new)
               );
            }
         );
   }

   private static <E> CompletableFuture<?> dumpValue(
      final Path path, final CachedOutput cache, final DynamicOps<JsonElement> ops, final Encoder<E> codec, final E value
   ) {
      return (CompletableFuture<?>)codec.encodeStart(ops, value)
         .mapOrElse(
            result -> DataProvider.saveStable(cache, result, path),
            error -> CompletableFuture.failedFuture(new IllegalStateException("Couldn't generate file '" + path + "': " + error.message()))
         );
   }

   @Override
   public final String getName() {
      return "Registries for " + this.name;
   }
}
