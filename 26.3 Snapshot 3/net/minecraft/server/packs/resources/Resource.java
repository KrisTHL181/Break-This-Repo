package net.minecraft.server.packs.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.KnownPack;
import org.jspecify.annotations.Nullable;

public class Resource {
   private final PackResources source;
   private final IoSupplier<InputStream> streamSupplier;
   private final IoSupplier<ResourceMetadata> metadataSupplier;
   private @Nullable ResourceMetadata cachedMetadata;

   public Resource(final PackResources source, final IoSupplier<InputStream> streamSupplier, final IoSupplier<ResourceMetadata> metadataSupplier) {
      this.source = source;
      this.streamSupplier = streamSupplier;
      this.metadataSupplier = metadataSupplier;
   }

   public Resource(final PackResources source, final IoSupplier<InputStream> streamSupplier) {
      this.source = source;
      this.streamSupplier = streamSupplier;
      this.metadataSupplier = ResourceMetadata.EMPTY_SUPPLIER;
      this.cachedMetadata = ResourceMetadata.EMPTY;
   }

   public PackResources source() {
      return this.source;
   }

   public String sourcePackId() {
      return this.source.packId();
   }

   public Optional<KnownPack> knownPackInfo() {
      return this.source.knownPackInfo();
   }

   public InputStream open() throws IOException {
      return this.streamSupplier.get();
   }

   public BufferedReader openAsReader() throws IOException {
      return new BufferedReader(new InputStreamReader(this.open(), StandardCharsets.UTF_8));
   }

   public ResourceMetadata metadata() throws IOException {
      if (this.cachedMetadata == null) {
         this.cachedMetadata = this.metadataSupplier.get();
      }

      return this.cachedMetadata;
   }
}
