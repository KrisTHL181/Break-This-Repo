package net.minecraft.server.packs;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;

public class VanillaPackResources {
   private final FixedPathPackResources fullResources;
   private final List<PackResources> resourceLayers;

   public VanillaPackResources(final FixedPathPackResources fullResources, final List<PackResources> resourceLayers) {
      this.fullResources = fullResources;
      this.resourceLayers = resourceLayers;
   }

   public void listRawPaths(final PackType type, final Identifier resource, final Consumer<Path> output) {
      this.fullResources.listRawPaths(type, resource, output);
   }

   public PackResources fullResources() {
      return this.fullResources;
   }

   public Pack.ResourcesSupplier asResourcesSupplier() {
      return new Pack.ResourcesSupplier() {
         @Override
         public PackMetadataResources openMetadata(final PackLocationInfo location) {
            return VanillaPackResources.this.fullResources;
         }

         @Override
         public Stream<PackResources> openResources(final PackLocationInfo location, final Pack.Metadata metadata) {
            return VanillaPackResources.this.resourceLayers.stream();
         }
      };
   }

   public ResourceProvider asProvider() {
      return location -> Optional.ofNullable(this.fullResources.getResource(PackType.CLIENT_RESOURCES, location))
         .map(s -> new Resource(this.fullResources, (IoSupplier<InputStream>)s));
   }
}
