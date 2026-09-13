package net.minecraft.server.packs.resources;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;

public interface ResourceManager extends ResourceProvider {
   Set<String> getNamespaces();

   List<Resource> getResourceStack(Identifier location);

   Map<Identifier, Resource> listResources(String directory, ResourceManager.Selector selector);

   Map<Identifier, List<Resource>> listResourceStacks(String directory, ResourceManager.Selector selector);

   Stream<PackResources> listPacks();

   enum Empty implements ResourceManager {
      INSTANCE;

      @Override
      public Set<String> getNamespaces() {
         return Set.of();
      }

      @Override
      public Optional<Resource> getResource(final Identifier location) {
         return Optional.empty();
      }

      @Override
      public List<Resource> getResourceStack(final Identifier location) {
         return List.of();
      }

      @Override
      public Map<Identifier, Resource> listResources(final String directory, final ResourceManager.Selector selector) {
         return Map.of();
      }

      @Override
      public Map<Identifier, List<Resource>> listResourceStacks(final String directory, final ResourceManager.Selector selector) {
         return Map.of();
      }

      @Override
      public Stream<PackResources> listPacks() {
         return Stream.of();
      }
   }

   @FunctionalInterface
   interface Selector {
      boolean isIncluded(Identifier resourceId);
   }
}
