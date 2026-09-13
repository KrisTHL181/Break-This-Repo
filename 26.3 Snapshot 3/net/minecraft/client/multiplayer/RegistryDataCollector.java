package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RegistryDataCollector {
   private static final Logger LOGGER = LogUtils.getLogger();
   private RegistryDataCollector.@Nullable ContentsCollector contentsCollector;
   private RegistryDataCollector.@Nullable TagCollector tagCollector;

   public void appendContents(final ResourceKey<? extends Registry<?>> registry, final List<RegistrySynchronization.PackedRegistryEntry> elementData) {
      if (this.contentsCollector == null) {
         this.contentsCollector = new RegistryDataCollector.ContentsCollector();
      }

      this.contentsCollector.append(registry, elementData);
   }

   public void appendTags(final Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> data) {
      if (this.tagCollector == null) {
         this.tagCollector = new RegistryDataCollector.TagCollector();
      }

      data.forEach(this.tagCollector::append);
   }

   private static <T> Registry.PendingTags<T> resolveRegistryTags(
      final RegistryAccess.Frozen context, final ResourceKey<? extends Registry<? extends T>> registryKey, final TagNetworkSerialization.NetworkPayload tags
   ) {
      Registry<T> staticRegistry = context.lookupOrThrow(registryKey);
      return staticRegistry.prepareTagReload(tags.resolve(staticRegistry));
   }

   private RegistryAccess loadNewElementsAndTags(
      final ResourceProvider knownDataSource, final RegistryDataCollector.ContentsCollector contentsCollector, final boolean tagsForSynchronizedRegistriesOnly
   ) {
      LayeredRegistryAccess<ClientRegistryLayer> base = ClientRegistryLayer.createRegistryAccess();
      RegistryAccess.Frozen loadingContext = base.getAccessForLoading(ClientRegistryLayer.REMOTE);
      Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> entriesToLoad = new HashMap<>();
      contentsCollector.elements
         .forEach(
            (registryKey, elements) -> entriesToLoad.put(
               (ResourceKey<? extends Registry<?>>)registryKey,
               new RegistryDataLoader.NetworkedRegistryData(
                  (List<RegistrySynchronization.PackedRegistryEntry>)elements, TagNetworkSerialization.NetworkPayload.EMPTY
               )
            )
         );
      List<Registry.PendingTags<?>> pendingStaticTags = new ArrayList<>();
      if (this.tagCollector != null) {
         this.tagCollector.forEach((registryKey, tags) -> {
            if (!tags.isEmpty()) {
               if (RegistrySynchronization.isNetworkable((ResourceKey<? extends Registry<?>>)registryKey)) {
                  entriesToLoad.compute((ResourceKey<? extends Registry<?>>)registryKey, (key, previousData) -> {
                     List<RegistrySynchronization.PackedRegistryEntry> elements = previousData != null ? previousData.elements() : List.of();
                     return new RegistryDataLoader.NetworkedRegistryData(elements, tags);
                  });
               } else if (!tagsForSynchronizedRegistriesOnly) {
                  pendingStaticTags.add(resolveRegistryTags(loadingContext, (ResourceKey<? extends Registry<?>>)registryKey, tags));
               }
            }
         });
      }

      List<HolderLookup.RegistryLookup<?>> contextRegistriesWithTags = TagLoader.buildUpdatedLookups(loadingContext, pendingStaticTags);

      RegistryAccess.Frozen receivedRegistries;
      try {
         long start = Util.getMillis();
         receivedRegistries = RegistryDataLoader.load(
               entriesToLoad, knownDataSource, contextRegistriesWithTags, RegistryDataLoader.SYNCHRONIZED_REGISTRIES, Util.backgroundExecutor()
            )
            .join();
         long end = Util.getMillis();
         LOGGER.debug("Loading network data took {} ms", end - start);
      } catch (Exception e) {
         CrashReport report = CrashReport.forThrowable(e, "Network Registry Load");
         addCrashDetails(report, entriesToLoad, pendingStaticTags);
         throw new ReportedException(report);
      }

      RegistryAccess registries = base.replaceFrom(ClientRegistryLayer.REMOTE, receivedRegistries).compositeAccess();
      pendingStaticTags.forEach(Registry.PendingTags::apply);
      return registries;
   }

   private static void addCrashDetails(
      final CrashReport report,
      final Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> dynamicRegistries,
      final List<Registry.PendingTags<?>> staticRegistries
   ) {
      CrashReportCategory details = report.addCategory("Received Elements and Tags");
      details.setDetail(
         "Dynamic Registries",
         () -> dynamicRegistries.entrySet()
            .stream()
            .sorted(Comparator.comparing(entry -> entry.getKey().identifier()))
            .map(
               entry -> String.format(
                  Locale.ROOT, "\n\t\t%s: elements=%d tags=%d", entry.getKey().identifier(), entry.getValue().elements().size(), entry.getValue().tags().size()
               )
            )
            .collect(Collectors.joining())
      );
      details.setDetail(
         "Static Registries",
         () -> staticRegistries.stream()
            .sorted(Comparator.comparing(entry -> entry.key().identifier()))
            .map(entry -> String.format(Locale.ROOT, "\n\t\t%s: tags=%d", entry.key().identifier(), entry.size()))
            .collect(Collectors.joining())
      );
   }

   private static void loadOnlyTags(
      final RegistryDataCollector.TagCollector tagCollector, final RegistryAccess.Frozen originalRegistries, final boolean includeSharedRegistries
   ) {
      tagCollector.forEach((registryKey, tags) -> {
         if (includeSharedRegistries || RegistrySynchronization.isNetworkable((ResourceKey<? extends Registry<?>>)registryKey)) {
            resolveRegistryTags(originalRegistries, (ResourceKey<? extends Registry<?>>)registryKey, tags).apply();
         }
      });
   }

   private static void updateComponents(final RegistryAccess.Frozen frozenRegistries, final boolean includeSharedRegistries) {
      BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(frozenRegistries).forEach(pendingComponents -> {
         if (includeSharedRegistries || RegistrySynchronization.isNetworkable(pendingComponents.key())) {
            pendingComponents.apply();
         }
      });
   }

   public RegistryAccess.Frozen collectGameRegistries(
      final ResourceProvider knownDataSource, final RegistryAccess.Frozen originalRegistries, final boolean tagsAndComponentsForSynchronizedRegistriesOnly
   ) {
      RegistryAccess registries;
      if (this.contentsCollector != null) {
         registries = this.loadNewElementsAndTags(knownDataSource, this.contentsCollector, tagsAndComponentsForSynchronizedRegistriesOnly);
      } else {
         if (this.tagCollector != null) {
            loadOnlyTags(this.tagCollector, originalRegistries, !tagsAndComponentsForSynchronizedRegistriesOnly);
         }

         registries = originalRegistries;
      }

      RegistryAccess.Frozen frozenRegistries = registries.freeze();
      updateComponents(frozenRegistries, !tagsAndComponentsForSynchronizedRegistriesOnly);
      return frozenRegistries;
   }

   private static class ContentsCollector {
      private final Map<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> elements = new HashMap<>();

      public void append(final ResourceKey<? extends Registry<?>> registry, final List<RegistrySynchronization.PackedRegistryEntry> elementData) {
         this.elements.computeIfAbsent(registry, ignore -> new ArrayList<>()).addAll(elementData);
      }
   }

   private static class TagCollector {
      private final Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags = new HashMap<>();

      public void append(final ResourceKey<? extends Registry<?>> registry, final TagNetworkSerialization.NetworkPayload tagData) {
         this.tags.put(registry, tagData);
      }

      public void forEach(final BiConsumer<? super ResourceKey<? extends Registry<?>>, ? super TagNetworkSerialization.NetworkPayload> action) {
         this.tags.forEach(action);
      }
   }
}
