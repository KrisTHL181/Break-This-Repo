package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FileUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.DirectoryTemplateSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.ResourceManagerTemplateSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.TemplatePathFactory;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.TemplateSource;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class StructureTemplateManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String STRUCTURE_FILE_EXTENSION = ".nbt";
   private static final String STRUCTURE_TEXT_FILE_EXTENSION = ".snbt";
   public static final String STRUCTURE_DIRECTORY_NAME = "structure";
   public static final FileToIdConverter WORLD_STRUCTURE_LISTER = new FileToIdConverter("structure", ".nbt");
   private static final FileToIdConverter WORLD_TEXT_STRUCTURE_LISTER = new FileToIdConverter("structure", ".snbt");
   private static final FileToIdConverter RESOURCE_STRUCTURE_LISTER = new FileToIdConverter("structure", ".nbt");
   public static final FileToIdConverter RESOURCE_TEXT_STRUCTURE_LISTER = new FileToIdConverter("structure", ".snbt");
   private final Map<Identifier, Optional<StructureTemplate>> structureRepository = new ConcurrentHashMap<>();
   private final ResourceManagerTemplateSource resourceManagerSource;
   private final List<TemplateSource> sources;
   private final TemplatePathFactory worldTemplates;
   private final @Nullable TemplatePathFactory testTemplates;

   public StructureTemplateManager(
      final ResourceManager resourceManager,
      final LevelStorageSource.LevelStorageAccess storage,
      final DataFixer fixerUpper,
      final HolderGetter<Block> blockLookup
   ) {
      this.resourceManagerSource = new ResourceManagerTemplateSource(fixerUpper, blockLookup, resourceManager, RESOURCE_STRUCTURE_LISTER);
      Path generatedDir = storage.getLevelPath(LevelResource.GENERATED_DIR).normalize();
      this.worldTemplates = new TemplatePathFactory(generatedDir);
      if (StructureUtils.testStructuresTargetDir != null) {
         this.testTemplates = new TemplatePathFactory(StructureUtils.testStructuresTargetDir, PackType.SERVER_DATA);
      } else {
         this.testTemplates = null;
      }

      Builder<TemplateSource> sources = ImmutableList.builder();
      sources.add(new DirectoryTemplateSource(fixerUpper, blockLookup, generatedDir, WORLD_STRUCTURE_LISTER, false));
      if (StructureUtils.testStructuresSourceDir != null) {
         sources.add(
            new DirectoryTemplateSource(
               fixerUpper, blockLookup, StructureUtils.testStructuresSourceDir, PackType.SERVER_DATA, RESOURCE_TEXT_STRUCTURE_LISTER, true
            )
         );
      }

      sources.add(this.resourceManagerSource);
      this.sources = sources.build();
   }

   public StructureTemplate getOrCreate(final Identifier id) {
      Optional<StructureTemplate> cachedTemplate = this.get(id);
      if (cachedTemplate.isPresent()) {
         return cachedTemplate.get();
      }

      StructureTemplate template = new StructureTemplate();
      this.structureRepository.put(id, Optional.of(template));
      return template;
   }

   public Optional<StructureTemplate> get(final Identifier id) {
      return this.structureRepository.computeIfAbsent(id, this::tryLoad);
   }

   public Stream<Identifier> listTemplates() {
      return this.sources.stream().flatMap(TemplateSource::list).distinct();
   }

   private Optional<StructureTemplate> tryLoad(final Identifier id) {
      for (TemplateSource source : this.sources) {
         try {
            Optional<StructureTemplate> loaded = source.load(id);
            if (loaded.isPresent()) {
               return loaded;
            }
         } catch (Exception var5) {
         }
      }

      return Optional.empty();
   }

   public void onResourceManagerReload(final ResourceManager resourceManager) {
      this.resourceManagerSource.setResourceManager(resourceManager);
      this.structureRepository.clear();
   }

   public boolean save(final Identifier id) {
      Optional<StructureTemplate> structureTemplate = this.structureRepository.get(id);
      if (structureTemplate.isEmpty()) {
         return false;
      }

      Path file;
      boolean saveAsText;
      if (SharedConstants.DEBUG_SAVE_STRUCTURES_AS_SNBT) {
         file = this.worldTemplates.createAndValidatePathToStructure(id, WORLD_TEXT_STRUCTURE_LISTER);
         saveAsText = true;
      } else {
         file = this.worldTemplates.createAndValidatePathToStructure(id, WORLD_STRUCTURE_LISTER);
         saveAsText = false;
      }

      try {
         return save(file, structureTemplate.get(), saveAsText);
      } catch (Exception e) {
         LOGGER.warn("Failed to save structure file {} to {}", new Object[]{id, file, e});
         return false;
      }
   }

   public static boolean save(final Path file, final StructureTemplate structureTemplate, final boolean asText) throws IOException {
      Path parent = file.getParent();
      if (parent == null) {
         return false;
      }

      FileUtil.createDirectoriesSafe(parent);
      CompoundTag tag = structureTemplate.save(new CompoundTag());
      if (asText) {
         NbtToSnbt.writeSnbt(CachedOutput.NO_CACHE, file, NbtUtils.structureToSnbt(tag));
      } else {
         try (OutputStream output = new FileOutputStream(file.toFile())) {
            NbtIo.writeCompressed(tag, output);
         }
      }

      return true;
   }

   public TemplatePathFactory worldTemplates() {
      return this.worldTemplates;
   }

   public @Nullable TemplatePathFactory testTemplates() {
      return this.testTemplates;
   }

   public void remove(final Identifier id) {
      this.structureRepository.remove(id);
   }
}
