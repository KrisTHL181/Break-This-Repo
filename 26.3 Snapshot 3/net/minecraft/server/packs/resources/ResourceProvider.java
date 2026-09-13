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
   ResourceProvider EMPTY = location -> Optional.empty();

   Optional<Resource> getResource(Identifier location);

   default Resource getResourceOrThrow(final Identifier location) throws FileNotFoundException {
      return this.getResource(location).orElseThrow(() -> new FileNotFoundException(location.toString()));
   }

   default InputStream open(final Identifier location) throws IOException {
      return this.getResourceOrThrow(location).open();
   }

   default BufferedReader openAsReader(final Identifier location) throws IOException {
      return this.getResourceOrThrow(location).openAsReader();
   }

   static ResourceProvider fromMap(final Map<Identifier, Resource> map) {
      return location -> Optional.ofNullable(map.get(location));
   }
}
