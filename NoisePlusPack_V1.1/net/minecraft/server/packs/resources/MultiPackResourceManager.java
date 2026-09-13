package net.minecraft.server.packs.resources;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class MultiPackResourceManager implements CloseableResourceManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<String, FallbackResourceManager> namespacedManagers;
   private final List<PackResources> packs;

   public MultiPackResourceManager(PackType p_203797_, List<PackResources> p_203798_) {
      this.packs = List.copyOf(p_203798_);
      Map<String, FallbackResourceManager> map = new HashMap<>();
      List<String> list = p_203798_.stream().flatMap(p_215471_ -> p_215471_.getNamespaces(p_203797_).stream()).distinct().toList();

      for (PackResources packresources : p_203798_) {
         ResourceFilterSection resourcefiltersection = this.getPackFilterSection(packresources);
         Set<String> set = packresources.getNamespaces(p_203797_);
         Predicate<Identifier> predicate = resourcefiltersection != null ? p_449189_ -> resourcefiltersection.isPathFiltered(p_449189_.getPath()) : null;

         for (String s : list) {
            boolean flag = set.contains(s);
            boolean flag1 = resourcefiltersection != null && resourcefiltersection.isNamespaceFiltered(s);
            if (flag || flag1) {
               FallbackResourceManager fallbackresourcemanager = map.get(s);
               if (fallbackresourcemanager == null) {
                  fallbackresourcemanager = new FallbackResourceManager(p_203797_, s);
                  map.put(s, fallbackresourcemanager);
               }

               if (flag && flag1) {
                  fallbackresourcemanager.push(packresources, predicate);
               } else if (flag) {
                  fallbackresourcemanager.push(packresources);
               } else {
                  fallbackresourcemanager.pushFilterOnly(packresources.packId(), predicate);
               }
            }
         }
      }

      this.namespacedManagers = map;
   }

   private @Nullable ResourceFilterSection getPackFilterSection(PackResources p_215468_) {
      try {
         return p_215468_.getMetadataSection(ResourceFilterSection.TYPE);
      } catch (IOException ioexception) {
         LOGGER.error("Failed to get filter section from pack {}", p_215468_.packId());
         return null;
      }
   }

   @Override
   public Set<String> getNamespaces() {
      return this.namespacedManagers.keySet();
   }

   @Override
   public Optional<Resource> getResource(Identifier p_453208_) {
      ResourceManager resourcemanager = this.namespacedManagers.get(p_453208_.getNamespace());
      return resourcemanager != null ? resourcemanager.getResource(p_453208_) : Optional.empty();
   }

   @Override
   public List<Resource> getResourceStack(Identifier p_450950_) {
      ResourceManager resourcemanager = this.namespacedManagers.get(p_450950_.getNamespace());
      return resourcemanager != null ? resourcemanager.getResourceStack(p_450950_) : List.of();
   }

   @Override
   public Map<Identifier, Resource> listResources(String p_215476_, Predicate<Identifier> p_215477_) {
      checkTrailingDirectoryPath(p_215476_);
      Map<Identifier, Resource> map = new TreeMap<>();

      for (FallbackResourceManager fallbackresourcemanager : this.namespacedManagers.values()) {
         map.putAll(fallbackresourcemanager.listResources(p_215476_, p_215477_));
      }

      return map;
   }

   @Override
   public Map<Identifier, List<Resource>> listResourceStacks(String p_215479_, Predicate<Identifier> p_215480_) {
      checkTrailingDirectoryPath(p_215479_);
      Map<Identifier, List<Resource>> map = new TreeMap<>();

      for (FallbackResourceManager fallbackresourcemanager : this.namespacedManagers.values()) {
         map.putAll(fallbackresourcemanager.listResourceStacks(p_215479_, p_215480_));
      }

      return map;
   }

   private static void checkTrailingDirectoryPath(String p_249608_) {
      if (p_249608_.endsWith("/")) {
         throw new IllegalArgumentException("Trailing slash in path " + p_249608_);
      }
   }

   @Override
   public Stream<PackResources> listPacks() {
      return this.packs.stream();
   }

   @Override
   public void close() {
      this.packs.forEach(PackResources::close);
   }
}
