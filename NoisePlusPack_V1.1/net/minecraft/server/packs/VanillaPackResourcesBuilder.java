package net.minecraft.server.packs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.util.FileSystemUtil;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public class VanillaPackResourcesBuilder {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static Consumer<VanillaPackResourcesBuilder> developmentConfig = p_251787_ -> {};
   private static final Map<PackType, Path> ROOT_DIR_BY_TYPE = Util.make(() -> {
      synchronized (VanillaPackResources.class) {
         Builder<PackType, Path> builder = ImmutableMap.builder();

         for (PackType packtype : PackType.values()) {
            String s = "/" + packtype.getDirectory() + "/.mcassetsroot";
            URL url = VanillaPackResources.class.getResource(s);
            if (url == null) {
               LOGGER.error("File {} does not exist in classpath", s);
            } else {
               try {
                  URI uri = url.toURI();
                  String s1 = uri.getScheme();
                  if (!"jar".equals(s1) && !"file".equals(s1)) {
                     LOGGER.warn("Assets URL '{}' uses unexpected schema", uri);
                  }

                  Path path = FileSystemUtil.safeGetPath(uri);
                  builder.put(packtype, path.getParent());
               } catch (Exception exception) {
                  LOGGER.error("Couldn't resolve path to vanilla assets", exception);
               }
            }
         }

         return builder.build();
      }
   });
   private final Set<Path> rootPaths = new LinkedHashSet<>();
   private final Map<PackType, Set<Path>> pathsForType = new EnumMap<>(PackType.class);
   private BuiltInMetadata metadata = BuiltInMetadata.of();
   private final Set<String> namespaces = new HashSet<>();

   private boolean validateDirPath(Path p_249112_) {
      if (!Files.exists(p_249112_)) {
         return false;
      } else if (!Files.isDirectory(p_249112_)) {
         throw new IllegalArgumentException("Path " + p_249112_.toAbsolutePath() + " is not directory");
      } else {
         return true;
      }
   }

   private void pushRootPath(Path p_251084_) {
      if (this.validateDirPath(p_251084_)) {
         this.rootPaths.add(p_251084_);
      }
   }

   private void pushPathForType(PackType p_250073_, Path p_252259_) {
      if (this.validateDirPath(p_252259_)) {
         this.pathsForType.computeIfAbsent(p_250073_, p_250639_ -> new LinkedHashSet<>()).add(p_252259_);
      }
   }

   public VanillaPackResourcesBuilder pushJarResources() {
      ROOT_DIR_BY_TYPE.forEach((p_251514_, p_251979_) -> {
         this.pushRootPath(p_251979_.getParent());
         this.pushPathForType(p_251514_, p_251979_);
      });
      return this;
   }

   public VanillaPackResourcesBuilder pushClasspathResources(PackType p_251987_, Class<?> p_249062_) {
      Enumeration<URL> enumeration = null;

      try {
         enumeration = p_249062_.getClassLoader().getResources(p_251987_.getDirectory() + "/");
      } catch (IOException var8) {
      }

      while (enumeration != null && enumeration.hasMoreElements()) {
         URL url = enumeration.nextElement();

         try {
            URI uri = url.toURI();
            if ("file".equals(uri.getScheme())) {
               Path path = Paths.get(uri);
               this.pushRootPath(path.getParent());
               this.pushPathForType(p_251987_, path);
            }
         } catch (Exception exception) {
            LOGGER.error("Failed to extract path from {}", url, exception);
         }
      }

      return this;
   }

   public VanillaPackResourcesBuilder applyDevelopmentConfig() {
      developmentConfig.accept(this);
      return this;
   }

   public VanillaPackResourcesBuilder pushUniversalPath(Path p_249464_) {
      this.pushRootPath(p_249464_);

      for (PackType packtype : PackType.values()) {
         this.pushPathForType(packtype, p_249464_.resolve(packtype.getDirectory()));
      }

      return this;
   }

   public VanillaPackResourcesBuilder pushAssetPath(PackType p_248623_, Path p_250065_) {
      this.pushRootPath(p_250065_);
      this.pushPathForType(p_248623_, p_250065_);
      return this;
   }

   public VanillaPackResourcesBuilder setMetadata(BuiltInMetadata p_249597_) {
      this.metadata = p_249597_;
      return this;
   }

   public VanillaPackResourcesBuilder exposeNamespace(String... p_250838_) {
      this.namespaces.addAll(Arrays.asList(p_250838_));
      return this;
   }

   public VanillaPackResources build(PackLocationInfo p_332000_) {
      return new VanillaPackResources(
         p_332000_,
         this.metadata,
         Set.copyOf(this.namespaces),
         copyAndReverse(this.rootPaths),
         Util.makeEnumMap(PackType.class, p_390159_ -> copyAndReverse(this.pathsForType.getOrDefault(p_390159_, Set.of())))
      );
   }

   private static List<Path> copyAndReverse(Collection<Path> p_252072_) {
      List<Path> list = new ArrayList<>(p_252072_);
      Collections.reverse(list);
      return List.copyOf(list);
   }
}
