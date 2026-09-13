package net.minecraft.server.packs.resources;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

@FunctionalInterface
public interface ResourceProvider {
   ResourceProvider EMPTY = p_457023_ -> Optional.empty();

   Optional<Resource> getResource(Identifier var1);

   default Resource getResourceOrThrow(Identifier p_456632_) throws FileNotFoundException {
      return this.getResource(p_456632_).orElseThrow(() -> new FileNotFoundException(p_456632_.toString()));
   }

   default InputStream open(Identifier p_460638_) throws IOException {
      return this.getResourceOrThrow(p_460638_).open();
   }

   default BufferedReader openAsReader(Identifier p_459945_) throws IOException {
      return this.getResourceOrThrow(p_459945_).openAsReader();
   }

   static ResourceProvider fromMap(Map<Identifier, Resource> p_251819_) {
      return p_455539_ -> Optional.ofNullable(p_251819_.get(p_455539_));
   }
}
