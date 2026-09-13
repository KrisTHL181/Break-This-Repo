package net.minecraft.server.packs;

import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;

public interface PackResources extends PackMetadataResources {
   String METADATA_EXTENSION = ".mcmeta";
   String PACK_META = "pack.mcmeta";

   @Nullable IoSupplier<InputStream> getResource(PackType type, Identifier location);

   void listResources(PackType type, String namespace, String directory, PackResources.ResourceOutput output);

   Set<String> getNamespaces(PackType type);

   default String packId() {
      return this.location().id();
   }

   default Optional<KnownPack> knownPackInfo() {
      return this.location().knownPackInfo();
   }

   @FunctionalInterface
   interface Filter {
      boolean isFiltered(Identifier resourceId);
   }

   @FunctionalInterface
   interface ResourceOutput extends BiConsumer<Identifier, IoSupplier<InputStream>> {
   }
}
