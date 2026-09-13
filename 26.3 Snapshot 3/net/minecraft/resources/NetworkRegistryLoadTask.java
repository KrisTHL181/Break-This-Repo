package net.minecraft.resources;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.Util;

public class NetworkRegistryLoadTask<T> extends RegistryLoadTask<T> {
   private static final RegistrationInfo NETWORK_REGISTRATION_INFO = new RegistrationInfo(Optional.empty(), Lifecycle.experimental());
   private final Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> entries;
   private final ResourceProvider knownDataSource;

   public NetworkRegistryLoadTask(
      final RegistryDataLoader.RegistryData<T> data,
      final Lifecycle lifecycle,
      final Map<ResourceKey<?>, Exception> loadingErrors,
      final Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> entries,
      final ResourceProvider knownDataSource
   ) {
      super(data, lifecycle, loadingErrors);
      this.entries = entries;
      this.knownDataSource = knownDataSource;
   }

   @Override
   public CompletableFuture<?> load(final RegistryOps.RegistryInfoLookup context, final Executor executor) {
      RegistryDataLoader.NetworkedRegistryData registryEntries = this.entries.get(this.registryKey());
      if (registryEntries == null) {
         return CompletableFuture.completedFuture(null);
      }

      RegistryOps<Tag> nbtOps = RegistryOps.create(NbtOps.INSTANCE, context);
      RegistryOps<JsonElement> jsonOps = RegistryOps.create(JsonOps.INSTANCE, context);
      FileToIdConverter knownDataPathConverter = FileToIdConverter.registry(this.registryKey());
      List<CompletableFuture<RegistryLoadTask.PendingRegistration<T>>> elements = new ArrayList<>(registryEntries.elements().size());

      for (RegistrySynchronization.PackedRegistryEntry entry : registryEntries.elements()) {
         ResourceKey<T> elementKey = ResourceKey.create(this.registryKey(), entry.id());
         Optional<Tag> networkContents = entry.data();
         if (networkContents.isPresent()) {
            elements.add(
               CompletableFuture.supplyAsync(
                  () -> new RegistryLoadTask.PendingRegistration<>(
                     elementKey,
                     RegistryLoadTask.PendingRegistration.loadFromNetwork(this.data.elementCodec(), nbtOps, elementKey, networkContents.get()),
                     NETWORK_REGISTRATION_INFO
                  ),
                  executor
               )
            );
         } else {
            elements.add(
               CompletableFuture.supplyAsync(
                  () -> new RegistryLoadTask.PendingRegistration<>(
                     elementKey,
                     RegistryLoadTask.PendingRegistration.findAndLoadFromResource(
                        this.data.elementCodec(), jsonOps, elementKey, knownDataPathConverter, this.knownDataSource
                     ),
                     NETWORK_REGISTRATION_INFO
                  ),
                  executor
               )
            );
         }
      }

      return Util.sequence(elements).thenAcceptAsync(pendingRegistrations -> {
         this.registerElements(pendingRegistrations.stream());
         Map<TagKey<T>, List<Holder<T>>> pendingTags = TagLoader.loadTagsFromNetwork(registryEntries.tags(), this.readOnlyRegistry());
         this.registerTags(pendingTags);
      }, executor);
   }
}
