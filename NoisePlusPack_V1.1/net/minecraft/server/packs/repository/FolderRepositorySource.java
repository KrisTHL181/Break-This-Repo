package net.minecraft.server.packs.repository;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.linkfs.LinkFileSystem;
import net.minecraft.util.FileUtil;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FolderRepositorySource implements RepositorySource {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final PackSelectionConfig DISCOVERED_PACK_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);
   private final Path folder;
   private final PackType packType;
   private final PackSource packSource;
   private final DirectoryValidator validator;

   public FolderRepositorySource(Path p_251796_, PackType p_251664_, PackSource p_250854_, DirectoryValidator p_300828_) {
      this.folder = p_251796_;
      this.packType = p_251664_;
      this.packSource = p_250854_;
      this.validator = p_300828_;
   }

   private static String nameFromPath(Path p_248745_) {
      return p_248745_.getFileName().toString();
   }

   @Override
   public void loadPacks(Consumer<Pack> p_250965_) {
      try {
         FileUtil.createDirectoriesSafe(this.folder);
         discoverPacks(this.folder, this.validator, (p_326474_, p_326475_) -> {
            PackLocationInfo packlocationinfo = this.createDiscoveredFilePackInfo(p_326474_);
            Pack pack = Pack.readMetaAndCreate(packlocationinfo, p_326475_, this.packType, DISCOVERED_PACK_SELECTION_CONFIG);
            if (pack != null) {
               p_250965_.accept(pack);
            }
         });
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to list packs in {}", this.folder, ioexception);
      }
   }

   private PackLocationInfo createDiscoveredFilePackInfo(Path p_328477_) {
      String s = nameFromPath(p_328477_);
      return new PackLocationInfo("file/" + s, Component.literal(s), this.packSource, Optional.empty());
   }

   public static void discoverPacks(Path p_248794_, DirectoryValidator p_299329_, BiConsumer<Path, Pack.ResourcesSupplier> p_248580_) throws IOException {
      FolderRepositorySource.FolderPackDetector folderrepositorysource$folderpackdetector = new FolderRepositorySource.FolderPackDetector(p_299329_);

      try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(p_248794_)) {
         for (Path path : directorystream) {
            try {
               List<ForbiddenSymlinkInfo> list = new ArrayList<>();
               Pack.ResourcesSupplier pack$resourcessupplier = folderrepositorysource$folderpackdetector.detectPackResources(path, list);
               if (!list.isEmpty()) {
                  LOGGER.warn("Ignoring potential pack entry: {}", ContentValidationException.getMessage(path, list));
               } else if (pack$resourcessupplier != null) {
                  p_248580_.accept(path, pack$resourcessupplier);
               } else {
                  LOGGER.info("Found non-pack entry '{}', ignoring", path);
               }
            } catch (IOException ioexception) {
               LOGGER.warn("Failed to read properties of '{}', ignoring", path, ioexception);
            }
         }
      }
   }

   static class FolderPackDetector extends PackDetector<Pack.ResourcesSupplier> {
      protected FolderPackDetector(DirectoryValidator p_301187_) {
         super(p_301187_);
      }

      protected Pack.@Nullable ResourcesSupplier createZipPack(Path p_299114_) {
         FileSystem filesystem = p_299114_.getFileSystem();
         if (filesystem != FileSystems.getDefault() && !(filesystem instanceof LinkFileSystem)) {
            FolderRepositorySource.LOGGER.info("Can't open pack archive at {}", p_299114_);
            return null;
         } else {
            return new FilePackResources.FileResourcesSupplier(p_299114_);
         }
      }

      protected Pack.ResourcesSupplier createDirectoryPack(Path p_300765_) {
         return new PathPackResources.PathResourcesSupplier(p_300765_);
      }
   }
}
