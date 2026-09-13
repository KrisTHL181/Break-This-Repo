package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FallbackResourceManager implements ResourceManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final List<FallbackResourceManager.PackEntry> fallbacks = new ArrayList<>();
   private final PackType type;
   private final String namespace;

   public FallbackResourceManager(final PackType type, final String namespace) {
      this.type = type;
      this.namespace = namespace;
   }

   public void push(final PackResources pack) {
      this.pushInternal(pack.packId(), pack, null);
   }

   public void push(final PackResources pack, final PackResources.Filter filter) {
      this.pushInternal(pack.packId(), pack, filter);
   }

   public void pushFilterOnly(final String name, final PackResources.Filter filter) {
      this.pushInternal(name, null, filter);
   }

   private void pushInternal(final String name, final @Nullable PackResources pack, final PackResources.@Nullable Filter contentFilter) {
      this.fallbacks.add(new FallbackResourceManager.PackEntry(name, pack, contentFilter));
   }

   @Override
   public Set<String> getNamespaces() {
      return ImmutableSet.of(this.namespace);
   }

   @Override
   public Optional<Resource> getResource(final Identifier location) {
      for (int i = this.fallbacks.size() - 1; i >= 0; i--) {
         FallbackResourceManager.PackEntry entry = this.fallbacks.get(i);
         PackResources fallback = entry.resources;
         if (fallback != null) {
            IoSupplier<InputStream> resource = fallback.getResource(this.type, location);
            if (resource != null) {
               IoSupplier<ResourceMetadata> metadataGetter = this.createStackMetadataFinder(location, i);
               return Optional.of(createResource(fallback, location, resource, metadataGetter));
            }
         }

         if (entry.isFiltered(location)) {
            LOGGER.warn("Resource {} not found, but was filtered by pack {}", location, entry.name);
            return Optional.empty();
         }
      }

      return Optional.empty();
   }

   private static Resource createResource(
      final PackResources source, final Identifier location, final IoSupplier<InputStream> resource, final IoSupplier<ResourceMetadata> metadata
   ) {
      return new Resource(source, wrapForDebug(location, source, resource), metadata);
   }

   private static IoSupplier<InputStream> wrapForDebug(final Identifier location, final PackResources source, final IoSupplier<InputStream> resource) {
      return LOGGER.isDebugEnabled() ? () -> new FallbackResourceManager.LeakedResourceWarningInputStream(resource.get(), location, source.packId()) : resource;
   }

   @Override
   public List<Resource> getResourceStack(final Identifier location) {
      Identifier metadataLocation = getMetadataLocation(location);
      List<Resource> result = new ArrayList<>();
      boolean filterMeta = false;
      String lastFilterName = null;

      for (int i = this.fallbacks.size() - 1; i >= 0; i--) {
         FallbackResourceManager.PackEntry entry = this.fallbacks.get(i);
         PackResources fileSource = entry.resources;
         if (fileSource != null) {
            IoSupplier<InputStream> resource = fileSource.getResource(this.type, location);
            if (resource != null) {
               IoSupplier<ResourceMetadata> metadataGetter;
               if (filterMeta) {
                  metadataGetter = ResourceMetadata.EMPTY_SUPPLIER;
               } else {
                  metadataGetter = () -> {
                     IoSupplier<InputStream> metaResource = fileSource.getResource(this.type, metadataLocation);
                     return metaResource != null ? parseMetadata(metaResource) : ResourceMetadata.EMPTY;
                  };
               }

               result.add(new Resource(fileSource, resource, metadataGetter));
            }
         }

         if (entry.isFiltered(location)) {
            lastFilterName = entry.name;
            break;
         }

         if (entry.isFiltered(metadataLocation)) {
            filterMeta = true;
         }
      }

      if (result.isEmpty() && lastFilterName != null) {
         LOGGER.warn("Resource {} not found, but was filtered by pack {}", location, lastFilterName);
      }

      return Lists.reverse(result);
   }

   private static boolean isMetadata(final Identifier location) {
      return location.getPath().endsWith(".mcmeta");
   }

   private static Identifier getIdentifierFromMetadata(final Identifier identifier) {
      String newPath = identifier.getPath().substring(0, identifier.getPath().length() - ".mcmeta".length());
      return identifier.withPath(newPath);
   }

   private static Identifier getMetadataLocation(final Identifier identifier) {
      return identifier.withPath(identifier.getPath() + ".mcmeta");
   }

   @Override
   public Map<Identifier, Resource> listResources(final String directory, final ResourceManager.Selector selector) {
      record ResourceWithSourceAndIndex(PackResources packResources, IoSupplier<InputStream> resource, int packIndex) {
      }

      Map<Identifier, ResourceWithSourceAndIndex> topResourceForFileLocation = new HashMap<>();
      Map<Identifier, ResourceWithSourceAndIndex> topResourceForMetaLocation = new HashMap<>();
      int packCount = this.fallbacks.size();

      for (int i = 0; i < packCount; i++) {
         FallbackResourceManager.PackEntry entry = this.fallbacks.get(i);
         entry.filterAll(topResourceForFileLocation.keySet());
         entry.filterAll(topResourceForMetaLocation.keySet());
         PackResources packResources = entry.resources;
         if (packResources != null) {
            int packIndex = i;
            packResources.listResources(this.type, this.namespace, directory, (resource, streamSupplier) -> {
               if (isMetadata(resource)) {
                  if (selector.isIncluded(getIdentifierFromMetadata(resource))) {
                     topResourceForMetaLocation.put(resource, new ResourceWithSourceAndIndex(packResources, streamSupplier, packIndex));
                  }
               } else if (selector.isIncluded(resource)) {
                  topResourceForFileLocation.put(resource, new ResourceWithSourceAndIndex(packResources, streamSupplier, packIndex));
               }
            });
         }
      }

      Map<Identifier, Resource> result = new TreeMap<>();
      topResourceForFileLocation.forEach((location, resource) -> {
         Identifier metadataLocation = getMetadataLocation(location);
         ResourceWithSourceAndIndex metaResource = topResourceForMetaLocation.get(metadataLocation);
         IoSupplier<ResourceMetadata> metaGetter;
         if (metaResource != null && metaResource.packIndex >= resource.packIndex) {
            metaGetter = convertToMetadata(metaResource.resource);
         } else {
            metaGetter = ResourceMetadata.EMPTY_SUPPLIER;
         }

         result.put(location, createResource(resource.packResources, location, resource.resource, metaGetter));
      });
      return result;
   }

   private IoSupplier<ResourceMetadata> createStackMetadataFinder(final Identifier location, final int finalPackIndex) {
      return () -> {
         Identifier metadataLocation = getMetadataLocation(location);

         for (int i = this.fallbacks.size() - 1; i >= finalPackIndex; i--) {
            FallbackResourceManager.PackEntry entry = this.fallbacks.get(i);
            PackResources metadataPackCandidate = entry.resources;
            if (metadataPackCandidate != null) {
               IoSupplier<InputStream> resource = metadataPackCandidate.getResource(this.type, metadataLocation);
               if (resource != null) {
                  return parseMetadata(resource);
               }
            }

            if (entry.isFiltered(metadataLocation)) {
               break;
            }
         }

         return ResourceMetadata.EMPTY;
      };
   }

   private static IoSupplier<ResourceMetadata> convertToMetadata(final IoSupplier<InputStream> input) {
      return () -> parseMetadata(input);
   }

   private static ResourceMetadata parseMetadata(final IoSupplier<InputStream> input) throws IOException {
      try (InputStream metadata = input.get()) {
         return ResourceMetadata.fromJsonStream(metadata);
      }
   }

   private static void applyPackFiltersToExistingResources(
      final FallbackResourceManager.PackEntry entry, final Map<Identifier, FallbackResourceManager.EntryStack> foundResources
   ) {
      for (FallbackResourceManager.EntryStack e : foundResources.values()) {
         if (entry.isFiltered(e.fileLocation)) {
            e.fileSources.clear();
         } else if (entry.isFiltered(e.metadataLocation())) {
            e.metaSources.clear();
         }
      }
   }

   private void listPackResources(
      final FallbackResourceManager.PackEntry entry,
      final String directory,
      final ResourceManager.Selector selector,
      final Map<Identifier, FallbackResourceManager.EntryStack> foundResources
   ) {
      PackResources pack = entry.resources;
      if (pack != null) {
         pack.listResources(
            this.type,
            this.namespace,
            directory,
            (id, resource) -> {
               if (isMetadata(id)) {
                  Identifier actualId = getIdentifierFromMetadata(id);
                  if (!selector.isIncluded(actualId)) {
                     return;
                  }

                  foundResources.computeIfAbsent(actualId, FallbackResourceManager.EntryStack::new).metaSources.put(pack, resource);
               } else {
                  if (!selector.isIncluded(id)) {
                     return;
                  }

                  foundResources.computeIfAbsent(id, FallbackResourceManager.EntryStack::new)
                     .fileSources
                     .add(new FallbackResourceManager.ResourceWithSource(pack, resource));
               }
            }
         );
      }
   }

   @Override
   public Map<Identifier, List<Resource>> listResourceStacks(final String directory, final ResourceManager.Selector selector) {
      Map<Identifier, FallbackResourceManager.EntryStack> foundResources = new HashMap<>();

      for (FallbackResourceManager.PackEntry entry : this.fallbacks) {
         applyPackFiltersToExistingResources(entry, foundResources);
         this.listPackResources(entry, directory, selector, foundResources);
      }

      SortedMap<Identifier, List<Resource>> result = new TreeMap<>();

      for (FallbackResourceManager.EntryStack entry : foundResources.values()) {
         if (!entry.fileSources.isEmpty()) {
            List<Resource> resources = new ArrayList<>();

            for (FallbackResourceManager.ResourceWithSource stackEntry : entry.fileSources) {
               PackResources source = stackEntry.source;
               IoSupplier<InputStream> metaSource = entry.metaSources.get(source);
               IoSupplier<ResourceMetadata> metaGetter = metaSource != null ? convertToMetadata(metaSource) : ResourceMetadata.EMPTY_SUPPLIER;
               resources.add(createResource(source, entry.fileLocation, stackEntry.resource, metaGetter));
            }

            result.put(entry.fileLocation, resources);
         }
      }

      return result;
   }

   @Override
   public Stream<PackResources> listPacks() {
      return this.fallbacks.stream().map(p -> p.resources).filter(Objects::nonNull);
   }

   private record EntryStack(
      Identifier fileLocation,
      Identifier metadataLocation,
      List<FallbackResourceManager.ResourceWithSource> fileSources,
      Map<PackResources, IoSupplier<InputStream>> metaSources
   ) {
      public EntryStack(final Identifier fileLocation) {
         this(fileLocation, FallbackResourceManager.getMetadataLocation(fileLocation), new ArrayList<>(), new Object2ObjectArrayMap());
      }
   }

   private static class LeakedResourceWarningInputStream extends FilterInputStream {
      private final Supplier<String> message;
      private boolean closed;

      public LeakedResourceWarningInputStream(final InputStream wrapped, final Identifier location, final String name) {
         super(wrapped);
         Exception exception = new Exception("Stacktrace");
         this.message = () -> {
            StringWriter data = new StringWriter();
            exception.printStackTrace(new PrintWriter(data));
            return "Leaked resource: '" + location + "' loaded from pack: '" + name + "'\n" + data;
         };
      }

      @Override
      public void close() throws IOException {
         super.close();
         this.closed = true;
      }

      @Override
      protected void finalize() throws Throwable {
         if (!this.closed) {
            FallbackResourceManager.LOGGER.warn("{}", this.message.get());
         }

         super.finalize();
      }
   }

   private record PackEntry(String name, @Nullable PackResources resources, PackResources.@Nullable Filter filter) {
      public void filterAll(final Collection<Identifier> collection) {
         if (this.filter != null) {
            collection.removeIf(this.filter::isFiltered);
         }
      }

      public boolean isFiltered(final Identifier location) {
         return this.filter != null && this.filter.isFiltered(location);
      }
   }

   private record ResourceWithSource(PackResources source, IoSupplier<InputStream> resource) {
   }
}
