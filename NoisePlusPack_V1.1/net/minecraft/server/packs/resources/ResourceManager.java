package net.minecraft.server.packs.resources;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;

public interface ResourceManager extends ResourceProvider {
   Set<String> getNamespaces();

   List<Resource> getResourceStack(Identifier var1);

   Map<Identifier, Resource> listResources(String var1, Predicate<Identifier> var2);

   Map<Identifier, List<Resource>> listResourceStacks(String var1, Predicate<Identifier> var2);

   Stream<PackResources> listPacks();

   enum Empty implements ResourceManager {
      INSTANCE;

      @Override
      public Set<String> getNamespaces() {
         return Set.of();
      }

      @Override
      public Optional<Resource> getResource(Identifier p_452908_) {
         return Optional.empty();
      }

      @Override
      public List<Resource> getResourceStack(Identifier p_456425_) {
         return List.of();
      }

      @Override
      public Map<Identifier, Resource> listResources(String p_215570_, Predicate<Identifier> p_215571_) {
         return Map.of();
      }

      @Override
      public Map<Identifier, List<Resource>> listResourceStacks(String p_215573_, Predicate<Identifier> p_215574_) {
         return Map.of();
      }

      @Override
      public Stream<PackResources> listPacks() {
         return Stream.of();
      }
   }
}
