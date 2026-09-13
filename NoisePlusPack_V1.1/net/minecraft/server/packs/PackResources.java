package net.minecraft.server.packs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;

public interface PackResources extends AutoCloseable {
   String METADATA_EXTENSION = ".mcmeta";
   String PACK_META = "pack.mcmeta";

   @Nullable IoSupplier<InputStream> getRootResource(String... var1);

   @Nullable IoSupplier<InputStream> getResource(PackType var1, Identifier var2);

   void listResources(PackType var1, String var2, String var3, PackResources.ResourceOutput var4);

   Set<String> getNamespaces(PackType var1);

   <T> @Nullable T getMetadataSection(MetadataSectionType<T> var1) throws IOException;

   PackLocationInfo location();

   default String packId() {
      return this.location().id();
   }

   default Optional<KnownPack> knownPackInfo() {
      return this.location().knownPackInfo();
   }

   @Override
   void close();

   @FunctionalInterface
   interface ResourceOutput extends BiConsumer<Identifier, IoSupplier<InputStream>> {
   }
}
