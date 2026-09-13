package net.minecraft.server.packs;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
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

public class PathPackResources extends AbstractPackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Joiner PATH_JOINER = Joiner.on("/");
   private final Path root;

   public PathPackResources(PackLocationInfo p_335945_, Path p_256025_) {
      super(p_335945_);
      this.root = p_256025_;
   }

   @Override
   public @Nullable IoSupplier<InputStream> getRootResource(String... p_249041_) {
      FileUtil.validatePath(p_249041_);
      Path path = FileUtil.resolvePath(this.root, List.of(p_249041_));
      return Files.exists(path) ? IoSupplier.create(path) : null;
   }

   public static boolean validatePath(Path p_249579_) {
      if (!SharedConstants.DEBUG_VALIDATE_RESOURCE_PATH_CASE) {
         return true;
      }

      if (p_249579_.getFileSystem() != FileSystems.getDefault()) {
         return true;
      }

      try {
         return p_249579_.toRealPath().endsWith(p_249579_);
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to resolve real path for {}", p_249579_, ioexception);
         return false;
      }
   }

   @Override
   public @Nullable IoSupplier<InputStream> getResource(PackType p_249352_, Identifier p_458454_) {
      Path path = this.root.resolve(p_249352_.getDirectory()).resolve(p_458454_.getNamespace());
      return getResource(p_458454_, path);
   }

   public static @Nullable IoSupplier<InputStream> getResource(Identifier p_452622_, Path p_251046_) {
      return (IoSupplier<InputStream>)FileUtil.decomposePath(p_452622_.getPath()).mapOrElse(p_449162_ -> {
         Path path = FileUtil.resolvePath(p_251046_, p_449162_);
         return returnFileIfExists(path);
      }, p_326463_ -> {
         LOGGER.error("Invalid path {}: {}", p_452622_, p_326463_.message());
         return null;
      });
   }

   private static @Nullable IoSupplier<InputStream> returnFileIfExists(Path p_250506_) {
      return Files.exists(p_250506_) && validatePath(p_250506_) ? IoSupplier.create(p_250506_) : null;
   }

   @Override
   public void listResources(PackType p_251452_, String p_249854_, String p_248650_, PackResources.ResourceOutput p_248572_) {
      FileUtil.decomposePath(p_248650_).ifSuccess(p_250225_ -> {
         Path path = this.root.resolve(p_251452_.getDirectory()).resolve(p_249854_);
         listPath(p_249854_, path, p_250225_, p_248572_);
      }).ifError(p_326465_ -> LOGGER.error("Invalid path {}: {}", p_248650_, p_326465_.message()));
   }

   public static void listPath(String p_249455_, Path p_249514_, List<String> p_251918_, PackResources.ResourceOutput p_249964_) {
      Path path = FileUtil.resolvePath(p_249514_, p_251918_);

      try (Stream<Path> stream = Files.find(path, Integer.MAX_VALUE, PathPackResources::isRegularFile)) {
         stream.forEach(p_449166_ -> {
            String s = PATH_JOINER.join(p_249514_.relativize(p_449166_));
            Identifier identifier = Identifier.tryBuild(p_249455_, s);
            if (identifier == null) {
               Util.logAndPauseIfInIde(String.format(Locale.ROOT, "Invalid path in pack: %s:%s, ignoring", p_249455_, s));
            } else {
               p_249964_.accept(identifier, IoSupplier.create(p_449166_));
            }
         });
      } catch (NotDirectoryException | NoSuchFileException var10) {
      } catch (IOException ioexception) {
         LOGGER.error("Failed to list path {}", path, ioexception);
      }
   }

   private static boolean isRegularFile(Path p_407723_, BasicFileAttributes p_405858_) {
      return !SharedConstants.IS_RUNNING_IN_IDE
         ? p_405858_.isRegularFile()
         : p_405858_.isRegularFile() && !StringUtils.equalsIgnoreCase(p_407723_.getFileName().toString(), ".ds_store");
   }

   @Override
   public Set<String> getNamespaces(PackType p_251896_) {
      Set<String> set = Sets.newHashSet();
      Path path = this.root.resolve(p_251896_.getDirectory());

      try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(path)) {
         for (Path path1 : directorystream) {
            String s = path1.getFileName().toString();
            if (Identifier.isValidNamespace(s)) {
               set.add(s);
            } else {
               LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring", s, this.root);
            }
         }
      } catch (NotDirectoryException | NoSuchFileException var10) {
      } catch (IOException ioexception) {
         LOGGER.error("Failed to list path {}", path, ioexception);
      }

      return set;
   }

   @Override
   public void close() {
   }

   public static class PathResourcesSupplier implements Pack.ResourcesSupplier {
      private final Path content;

      public PathResourcesSupplier(Path p_298516_) {
         this.content = p_298516_;
      }

      @Override
      public PackResources openPrimary(PackLocationInfo p_332278_) {
         return new PathPackResources(p_332278_, this.content);
      }

      @Override
      public PackResources openFull(PackLocationInfo p_329373_, Pack.Metadata p_332015_) {
         PackResources packresources = this.openPrimary(p_329373_);
         List<String> list = p_332015_.overlays();
         if (list.isEmpty()) {
            return packresources;
         }

         List<PackResources> list1 = new ArrayList<>(list.size());

         for (String s : list) {
            Path path = this.content.resolve(s);
            list1.add(new PathPackResources(p_329373_, path));
         }

         return new CompositePackResources(packresources, list1);
      }
   }
}
