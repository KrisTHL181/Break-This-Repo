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
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final PackSelectionConfig DISCOVERED_PACK_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);
   private final Path folder;
   private final PackType packType;
   private final PackSource packSource;
   private final DirectoryValidator validator;

   public FolderRepositorySource(final Path folder, final PackType packType, final PackSource packSource, final DirectoryValidator validator) {
      this.folder = folder;
      this.packType = packType;
      this.packSource = packSource;
      this.validator = validator;
   }

   private static String nameFromPath(final Path content) {
      return content.getFileName().toString();
   }

   @Override
   public void loadPacks(final Consumer<Pack> result) {
      try {
         FileUtil.createDirectoriesSafe(this.folder);
         discoverPacks(this.folder, this.validator, (content, resources) -> {
            PackLocationInfo locationInfo = this.createDiscoveredFilePackInfo(content);
            Pack pack = Pack.readMetaAndCreate(locationInfo, resources, this.packType, DISCOVERED_PACK_SELECTION_CONFIG);
            if (pack != null) {
               result.accept(pack);
            }
         });
      } catch (IOException e) {
         LOGGER.warn("Failed to list packs in {}", this.folder, e);
      }
   }

   private PackLocationInfo createDiscoveredFilePackInfo(final Path content) {
      String name = nameFromPath(content);
      return new PackLocationInfo("file/" + name, Component.literal(name), this.packSource, Optional.empty());
   }

   public static void discoverPacks(final Path folder, final DirectoryValidator validator, final BiConsumer<Path, Pack.ResourcesSupplier> result) throws IOException {
      FolderRepositorySource.FolderPackDetector detector = new FolderRepositorySource.FolderPackDetector(validator);

      try (DirectoryStream<Path> contents = Files.newDirectoryStream(folder)) {
         for (Path content : contents) {
            try {
               List<ForbiddenSymlinkInfo> validationIssues = new ArrayList<>();
               Pack.ResourcesSupplier resources = detector.detectPackResources(content, validationIssues);
               if (!validationIssues.isEmpty()) {
                  LOGGER.warn("Ignoring potential pack entry: {}", ContentValidationException.getMessage(content, validationIssues));
               } else if (resources != null) {
                  result.accept(content, resources);
               } else {
                  LOGGER.info("Found non-pack entry '{}', ignoring", content);
               }
            } catch (IOException e) {
               LOGGER.warn("Failed to read properties of '{}', ignoring", content, e);
            }
         }
      }
   }

   private static class FolderPackDetector extends PackDetector<Pack.ResourcesSupplier> {
      protected FolderPackDetector(final DirectoryValidator validator) {
         super(validator);
      }

      protected Pack.@Nullable ResourcesSupplier createZipPack(final Path content) {
         FileSystem fileSystem = content.getFileSystem();
         if (fileSystem != FileSystems.getDefault() && !(fileSystem instanceof LinkFileSystem)) {
            FolderRepositorySource.LOGGER.info("Can't open pack archive at {}", content);
            return null;
         } else {
            return new FilePackResources.FileResourcesSupplier(content);
         }
      }

      protected Pack.ResourcesSupplier createDirectoryPack(final Path content) {
         return new PathPackResources.PathResourcesSupplier(content);
      }
   }
}
