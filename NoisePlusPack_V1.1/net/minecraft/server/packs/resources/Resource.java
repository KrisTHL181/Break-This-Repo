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

   public Resource(PackResources p_250802_, IoSupplier<InputStream> p_248585_, IoSupplier<ResourceMetadata> p_250094_) {
      this.source = p_250802_;
      this.streamSupplier = p_248585_;
      this.metadataSupplier = p_250094_;
   }

   public Resource(PackResources p_250372_, IoSupplier<InputStream> p_248749_) {
      this.source = p_250372_;
      this.streamSupplier = p_248749_;
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
