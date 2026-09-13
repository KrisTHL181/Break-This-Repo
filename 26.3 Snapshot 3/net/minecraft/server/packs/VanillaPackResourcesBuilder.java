package net.minecraft.server.packs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.FileSystemUtil;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class VanillaPackResourcesBuilder {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static Consumer<VanillaPackResourcesBuilder> developmentConfig = builder -> {};
   private final FixedPathPackResources.Builder fullBuilder = new FixedPathPackResources.Builder();
   private final List<FixedPathPackResources.Builder> layeredBuilders = new ArrayList<>();
   private final Set<String> namespaces = new HashSet<>();
   private @Nullable ResourceMetadata metadata;
   private static final Map<PackType, Path> ROOT_DIR_BY_TYPE = Util.make(() -> {
      synchronized (VanillaPackResources.class) {
         Builder<PackType, Path> result = ImmutableMap.builder();

         for (PackType type : PackType.values()) {
            String probeName = "/" + type.getDirectory() + "/.mcassetsroot";
            URL probeUrl = VanillaPackResources.class.getResource(probeName);
            if (probeUrl == null) {
               LOGGER.error("File {} does not exist in classpath", probeName);
            } else {
               try {
                  URI probeUri = probeUrl.toURI();
                  String scheme = probeUri.getScheme();
                  if (!"jar".equals(scheme) && !"file".equals(scheme)) {
                     LOGGER.warn("Assets URL '{}' uses unexpected schema", probeUri);
                  }

                  Path probePath = FileSystemUtil.safeGetPath(probeUri);
                  result.put(type, probePath.getParent());
               } catch (Exception e) {
                  LOGGER.error("Couldn't resolve path to vanilla assets", e);
               }
            }
         }

         return result.build();
      }
   });

   public VanillaPackResourcesBuilder() {
      this.layeredBuilders.add(new FixedPathPackResources.Builder());
   }

   private void forLastLayer(final Consumer<FixedPathPackResources.Builder> task) {
      task.accept(this.fullBuilder);
      task.accept(this.layeredBuilders.getLast());
   }

   private void forAllLayers(final Consumer<FixedPathPackResources.Builder> task) {
      task.accept(this.fullBuilder);

      for (FixedPathPackResources.Builder layeredBuilder : this.layeredBuilders) {
         task.accept(layeredBuilder);
      }
   }

   private void pushRootPath(final Path path) {
      this.forLastLayer(builder -> builder.pushRootPath(path));
   }

   private void pushPathForType(final PackType packType, final Path path) {
      this.forLastLayer(builder -> builder.pushPathForType(packType, path));
   }

   public VanillaPackResourcesBuilder pushJarResources() {
      ROOT_DIR_BY_TYPE.forEach((packType, path) -> {
         this.pushRootPath(path.getParent());
         this.pushPathForType(packType, path);
      });
      return this;
   }

   public VanillaPackResourcesBuilder pushClasspathResources(final PackType packType, final Class<?> source) {
      Enumeration<URL> resources = null;

      try {
         resources = source.getClassLoader().getResources(packType.getDirectory() + "/");
      } catch (IOException var8) {
      }

      while (resources != null && resources.hasMoreElements()) {
         URL url = resources.nextElement();

         try {
            URI uri = url.toURI();
            if ("file".equals(uri.getScheme())) {
               Path assetsPath = Paths.get(uri);
               this.pushRootPath(assetsPath.getParent());
               this.pushPathForType(packType, assetsPath);
            }
         } catch (Exception e) {
            LOGGER.error("Failed to extract path from {}", url, e);
         }
      }

      return this;
   }

   public VanillaPackResourcesBuilder applyDevelopmentConfig() {
      developmentConfig.accept(this);
      return this;
   }

   public VanillaPackResourcesBuilder pushUniversalPath(final Path path) {
      this.pushRootPath(path);

      for (PackType packType : PackType.values()) {
         this.pushPathForType(packType, path.resolve(packType.getDirectory()));
      }

      return this;
   }

   public VanillaPackResourcesBuilder pushAssetPath(final PackType packType, final Path path) {
      this.pushRootPath(path);
      this.pushPathForType(packType, path);
      return this;
   }

   public VanillaPackResourcesBuilder setMetadata(final ResourceMetadata metadata) {
      this.forAllLayers(builder -> builder.setMetadata(metadata));
      this.metadata = metadata;
      return this;
   }

   public VanillaPackResourcesBuilder exposeNamespace(final String... namespaces) {
      List<String> namespaceList = List.of(namespaces);
      this.forAllLayers(builder -> builder.exposeNamespace(namespaceList));
      this.namespaces.addAll(namespaceList);
      return this;
   }

   public VanillaPackResourcesBuilder pushLayer() {
      FixedPathPackResources.Builder newBuilder = new FixedPathPackResources.Builder();
      this.layeredBuilders.add(newBuilder);
      newBuilder.exposeNamespace(this.namespaces);
      if (this.metadata != null) {
         newBuilder.setMetadata(this.metadata);
      }

      return this;
   }

   public VanillaPackResources build(final PackLocationInfo location) {
      return new VanillaPackResources(
         this.fullBuilder.build(location), this.layeredBuilders.stream().map(builder -> builder.build(location)).collect(Collectors.toUnmodifiableList())
      );
   }
}
