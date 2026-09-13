package net.minecraft.server.packs;

import com.google.common.base.Joiner;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class PathPackResources extends AbstractPackMetadataResources implements PackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Joiner PATH_JOINER = Joiner.on("/");
   private final Path root;

   public PathPackResources(final PackLocationInfo location, final Path root) {
      super(location);
      this.root = root;
   }

   @Override
   public @Nullable IoSupplier<InputStream> getRootResource(final String... path) {
      FileUtil.validatePath(path);
      Path pathInRoot = FileUtil.resolvePath(this.root, List.of(path));
      return Files.exists(pathInRoot) ? IoSupplier.create(pathInRoot) : null;
   }

   public static boolean validatePath(final Path path) {
      if (!SharedConstants.DEBUG_VALIDATE_RESOURCE_PATH_CASE) {
         return true;
      }

      if (path.getFileSystem() != FileSystems.getDefault()) {
         return true;
      }

      try {
         return path.toRealPath().endsWith(path);
      } catch (IOException e) {
         LOGGER.warn("Failed to resolve real path for {}", path, e);
         return false;
      }
   }

   private Path topPackDir(final PackType type) {
      return this.root.resolve(type.getDirectory());
   }

   @Override
   public @Nullable IoSupplier<InputStream> getResource(final PackType type, final Identifier location) {
      Path topDir = this.topPackDir(type);
      return getResource(topDir, location);
   }

   public static @Nullable IoSupplier<InputStream> getResource(final Path topDir, final Identifier location) {
      Path namespaceDir = topDir.resolve(location.getNamespace());
      return (IoSupplier<InputStream>)FileUtil.decomposePath(location.getPath()).mapOrElse(decomposedPath -> {
         Path resolvedPath = FileUtil.resolvePath(namespaceDir, decomposedPath);
         return returnFileIfExists(resolvedPath);
      }, error -> {
         LOGGER.error("Invalid path {}: {}", location, error.message());
         return null;
      });
   }

   private static @Nullable IoSupplier<InputStream> returnFileIfExists(final Path resolvedPath) {
      return Files.exists(resolvedPath) && validatePath(resolvedPath) ? IoSupplier.create(resolvedPath) : null;
   }

   @Override
   public void listResources(final PackType type, final String namespace, final String directory, final PackResources.ResourceOutput output) {
      Path topDir = this.topPackDir(type);
      listResources(topDir, namespace, directory, output);
   }

   public static void listResources(final Path topPath, final String namespace, final String directory, final PackResources.ResourceOutput output) {
      FileUtil.decomposePath(directory).ifSuccess(decomposedPath -> {
         Path namespaceDir = topPath.resolve(namespace);
         listPath(namespace, namespaceDir, decomposedPath, output);
      }).ifError(error -> LOGGER.error("Invalid path {}: {}", directory, error.message()));
   }

   public static void listPath(final String namespace, final Path topDir, final List<String> decomposedPrefixPath, final PackResources.ResourceOutput output) {
      Path targetPath = FileUtil.resolvePath(topDir, decomposedPrefixPath);

      try (Stream<Path> files = Files.find(targetPath, Integer.MAX_VALUE, PathPackResources::isRegularFile)) {
         files.forEach(file -> {
            String resourcePath = PATH_JOINER.join(topDir.relativize(file));
            Identifier identifier = Identifier.tryBuild(namespace, resourcePath);
            if (identifier == null) {
               Util.logAndPauseIfInIde(String.format(Locale.ROOT, "Invalid path in pack: %s:%s, ignoring", namespace, resourcePath));
            } else {
               output.accept(identifier, IoSupplier.create(file));
            }
         });
      } catch (NoSuchFileException | NotDirectoryException var10) {
      } catch (IOException e) {
         LOGGER.error("Failed to list path {}", targetPath, e);
      }
   }

   private static boolean isRegularFile(final Path file, final BasicFileAttributes attributes) {
      return !SharedConstants.IS_RUNNING_IN_IDE
         ? attributes.isRegularFile()
         : attributes.isRegularFile() && !StringUtils.equalsIgnoreCase(file.getFileName().toString(), ".ds_store");
   }

   @Override
   public Set<String> getNamespaces(final PackType type) {
      Path assetRoot = this.topPackDir(type);
      return getNamespaces(assetRoot);
   }

   public static Set<String> getNamespaces(final Path rootDir) {
      Set<String> namespaces = new HashSet<>();

      try (DirectoryStream<Path> directDirs = Files.newDirectoryStream(rootDir)) {
         for (Path directDir : directDirs) {
            if (!Files.isDirectory(directDir)) {
               LOGGER.warn("Non-directory entry {} found in namespace directory, rejecting", directDir);
            } else {
               String namespace = directDir.getFileName().toString();
               if (Identifier.isValidNamespace(namespace)) {
                  namespaces.add(namespace);
               } else {
                  LOGGER.warn("Non {} character in namespace {} in pack directory {}, ignoring", new Object[]{"[a-z0-9_.-]", namespace, rootDir});
               }
            }
         }
      } catch (NoSuchFileException | NotDirectoryException var8) {
      } catch (IOException e) {
         LOGGER.error("Failed to list path {}", rootDir, e);
      }

      return namespaces;
   }

   @Override
   public void close() {
   }

   public static class PathResourcesSupplier implements Pack.ResourcesSupplier {
      private final Path content;

      public PathResourcesSupplier(final Path content) {
         this.content = content;
      }

      private PathPackResources openPrimaryResources(final PackLocationInfo location) {
         return new PathPackResources(location, this.content);
      }

      @Override
      public PackMetadataResources openMetadata(final PackLocationInfo location) {
         return this.openPrimaryResources(location);
      }

      @Override
      public Stream<PackResources> openResources(final PackLocationInfo location, final Pack.Metadata metadata) {
         PackResources primary = this.openPrimaryResources(location);
         List<String> overlays = metadata.overlays();
         if (overlays.isEmpty()) {
            return Stream.of(primary);
         }

         List<PackResources> overlayResources = new ArrayList<>(overlays.size());

         for (String overlay : overlays) {
            Path overlayRoot = this.content.resolve(overlay);
            overlayResources.add(new PathPackResources(location, overlayRoot));
         }

         return Stream.of(new OverlayedPackResources(primary, overlayResources));
      }
   }
}
