package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.IdentifierException;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.FileUtil;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class StructureTemplateManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String STRUCTURE_RESOURCE_DIRECTORY_NAME = "structure";
   private static final String STRUCTURE_GENERATED_DIRECTORY_NAME = "structures";
   private static final String STRUCTURE_FILE_EXTENSION = ".nbt";
   private static final String STRUCTURE_TEXT_FILE_EXTENSION = ".snbt";
   private final Map<Identifier, Optional<StructureTemplate>> structureRepository = Maps.newConcurrentMap();
   private final DataFixer fixerUpper;
   private ResourceManager resourceManager;
   private final Path generatedDir;
   private final List<StructureTemplateManager.Source> sources;
   private final HolderGetter<Block> blockLookup;
   private static final FileToIdConverter RESOURCE_LISTER = new FileToIdConverter("structure", ".nbt");

   public StructureTemplateManager(
      ResourceManager p_249872_, LevelStorageSource.LevelStorageAccess p_249864_, DataFixer p_249868_, HolderGetter<Block> p_256126_
   ) {
      this.resourceManager = p_249872_;
      this.fixerUpper = p_249868_;
      this.generatedDir = p_249864_.getLevelPath(LevelResource.GENERATED_DIR).normalize();
      this.blockLookup = p_256126_;
      Builder<StructureTemplateManager.Source> builder = ImmutableList.builder();
      builder.add(new StructureTemplateManager.Source(this::loadFromGenerated, this::listGenerated));
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         builder.add(new StructureTemplateManager.Source(this::loadFromTestStructures, this::listTestStructures));
      }

      builder.add(new StructureTemplateManager.Source(this::loadFromResource, this::listResources));
      this.sources = builder.build();
   }

   public StructureTemplate getOrCreate(Identifier p_453409_) {
      Optional<StructureTemplate> optional = this.get(p_453409_);
      if (optional.isPresent()) {
         return optional.get();
      }

      StructureTemplate structuretemplate = new StructureTemplate();
      this.structureRepository.put(p_453409_, Optional.of(structuretemplate));
      return structuretemplate;
   }

   public Optional<StructureTemplate> get(Identifier p_450607_) {
      return this.structureRepository.computeIfAbsent(p_450607_, this::tryLoad);
   }

   public Stream<Identifier> listTemplates() {
      return this.sources.stream().flatMap(p_230376_ -> p_230376_.lister().get()).distinct();
   }

   private Optional<StructureTemplate> tryLoad(Identifier p_454662_) {
      for (StructureTemplateManager.Source structuretemplatemanager$source : this.sources) {
         try {
            Optional<StructureTemplate> optional = structuretemplatemanager$source.loader().apply(p_454662_);
            if (optional.isPresent()) {
               return optional;
            }
         } catch (Exception var5) {
         }
      }

      return Optional.empty();
   }

   public void onResourceManagerReload(ResourceManager p_230371_) {
      this.resourceManager = p_230371_;
      this.structureRepository.clear();
   }

   private Optional<StructureTemplate> loadFromResource(Identifier p_460225_) {
      Identifier identifier = RESOURCE_LISTER.idToFile(p_460225_);
      return this.load(() -> this.resourceManager.open(identifier), p_230366_ -> LOGGER.error("Couldn't load structure {}", p_460225_, p_230366_));
   }

   private Stream<Identifier> listResources() {
      return RESOURCE_LISTER.listMatchingResources(this.resourceManager).keySet().stream().map(RESOURCE_LISTER::fileToId);
   }

   private Optional<StructureTemplate> loadFromTestStructures(Identifier p_455863_) {
      return this.loadFromSnbt(p_455863_, StructureUtils.testStructuresDir);
   }

   private Stream<Identifier> listTestStructures() {
      if (!Files.isDirectory(StructureUtils.testStructuresDir)) {
         return Stream.empty();
      }

      List<Identifier> list = new ArrayList<>();
      this.listFolderContents(StructureUtils.testStructuresDir, "minecraft", ".snbt", list::add);
      return list.stream();
   }

   private Optional<StructureTemplate> loadFromGenerated(Identifier p_450277_) {
      if (!Files.isDirectory(this.generatedDir)) {
         return Optional.empty();
      }

      Path path = this.createAndValidatePathToGeneratedStructure(p_450277_, ".nbt");
      return this.load(() -> new FileInputStream(path.toFile()), p_230400_ -> LOGGER.error("Couldn't load structure from {}", path, p_230400_));
   }

   private Stream<Identifier> listGenerated() {
      if (!Files.isDirectory(this.generatedDir)) {
         return Stream.empty();
      }

      try {
         List<Identifier> list = new ArrayList<>();

         try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(this.generatedDir, p_230419_ -> Files.isDirectory(p_230419_))) {
            for (Path path : directorystream) {
               String s = path.getFileName().toString();
               Path path1 = path.resolve("structures");
               this.listFolderContents(path1, s, ".nbt", list::add);
            }
         }

         return list.stream();
      } catch (IOException ioexception) {
         return Stream.empty();
      }
   }

   private void listFolderContents(Path p_230395_, String p_230396_, String p_230397_, Consumer<Identifier> p_342318_) {
      int i = p_230397_.length();
      Function<String, String> function = p_230358_ -> p_230358_.substring(0, p_230358_.length() - i);

      try (Stream<Path> stream = Files.find(
            p_230395_, Integer.MAX_VALUE, (p_341961_, p_341962_) -> p_341962_.isRegularFile() && p_341961_.toString().endsWith(p_230397_)
         )) {
         stream.forEach(p_450051_ -> {
            try {
               p_342318_.accept(Identifier.fromNamespaceAndPath(p_230396_, function.apply(this.relativize(p_230395_, p_450051_))));
            } catch (IdentifierException identifierexception) {
               LOGGER.error("Invalid location while listing folder {} contents", p_230395_, identifierexception);
            }
         });
      } catch (IOException ioexception) {
         LOGGER.error("Failed to list folder {} contents", p_230395_, ioexception);
      }
   }

   private String relativize(Path p_230402_, Path p_230403_) {
      return p_230402_.relativize(p_230403_).toString().replace(File.separator, "/");
   }

   private Optional<StructureTemplate> loadFromSnbt(Identifier p_458049_, Path p_230369_) {
      if (!Files.isDirectory(p_230369_)) {
         return Optional.empty();
      }

      Path path = FileUtil.createPathToResource(p_230369_, p_458049_.getPath(), ".snbt");

      try (BufferedReader bufferedreader = Files.newBufferedReader(path)) {
         String s = IOUtils.toString(bufferedreader);
         return Optional.of(this.readStructure(NbtUtils.snbtToStructure(s)));
      } catch (NoSuchFileException nosuchfileexception) {
         return Optional.empty();
      } catch (CommandSyntaxException | IOException ioexception) {
         LOGGER.error("Couldn't load structure from {}", path, ioexception);
         return Optional.empty();
      }
   }

   private Optional<StructureTemplate> load(StructureTemplateManager.InputStreamOpener p_230373_, Consumer<Throwable> p_230374_) {
      try (
         InputStream inputstream = p_230373_.open();
         InputStream inputstream1 = new FastBufferedInputStream(inputstream);
      ) {
         return Optional.of(this.readStructure(inputstream1));
      } catch (FileNotFoundException filenotfoundexception) {
         return Optional.empty();
      } catch (Throwable throwable1) {
         p_230374_.accept(throwable1);
         return Optional.empty();
      }
   }

   private StructureTemplate readStructure(InputStream p_230378_) throws IOException {
      CompoundTag compoundtag = NbtIo.readCompressed(p_230378_, NbtAccounter.unlimitedHeap());
      return this.readStructure(compoundtag);
   }

   public StructureTemplate readStructure(CompoundTag p_230405_) {
      StructureTemplate structuretemplate = new StructureTemplate();
      int i = NbtUtils.getDataVersion(p_230405_, 500);
      structuretemplate.load(this.blockLookup, DataFixTypes.STRUCTURE.updateToCurrentVersion(this.fixerUpper, p_230405_, i));
      return structuretemplate;
   }

   public boolean save(Identifier p_455272_) {
      Optional<StructureTemplate> optional = this.structureRepository.get(p_455272_);
      if (optional.isEmpty()) {
         return false;
      }

      StructureTemplate structuretemplate = optional.get();
      Path path = this.createAndValidatePathToGeneratedStructure(p_455272_, SharedConstants.DEBUG_SAVE_STRUCTURES_AS_SNBT ? ".snbt" : ".nbt");
      Path path1 = path.getParent();
      if (path1 == null) {
         return false;
      }

      try {
         Files.createDirectories(Files.exists(path1) ? path1.toRealPath() : path1);
      } catch (IOException ioexception) {
         LOGGER.error("Failed to create parent directory: {}", path1);
         return false;
      }

      CompoundTag compoundtag = structuretemplate.save(new CompoundTag());
      if (SharedConstants.DEBUG_SAVE_STRUCTURES_AS_SNBT) {
         try {
            NbtToSnbt.writeSnbt(CachedOutput.NO_CACHE, path, NbtUtils.structureToSnbt(compoundtag));
         } catch (Throwable throwable1) {
            return false;
         }
      } else {
         try (OutputStream outputstream = new FileOutputStream(path.toFile())) {
            NbtIo.writeCompressed(compoundtag, outputstream);
         } catch (Throwable throwable) {
            return false;
         }
      }

      return true;
   }

   public Path createAndValidatePathToGeneratedStructure(Identifier p_457072_, String p_345223_) {
      if (p_457072_.getPath().contains("//")) {
         throw new IdentifierException("Invalid resource path: " + p_457072_);
      }

      try {
         Path path = this.generatedDir.resolve(p_457072_.getNamespace());
         Path path1 = path.resolve("structures");
         Path path2 = FileUtil.createPathToResource(path1, p_457072_.getPath(), p_345223_);
         if (path2.startsWith(this.generatedDir) && FileUtil.isPathNormalized(path2) && FileUtil.isPathPortable(path2)) {
            return path2;
         } else {
            throw new IdentifierException("Invalid resource path: " + path2);
         }
      } catch (InvalidPathException invalidpathexception) {
         throw new IdentifierException("Invalid resource path: " + p_457072_, invalidpathexception);
      }
   }

   public void remove(Identifier p_452141_) {
      this.structureRepository.remove(p_452141_);
   }

   @FunctionalInterface
   interface InputStreamOpener {
      InputStream open() throws IOException;
   }

   record Source(Function<Identifier, Optional<StructureTemplate>> loader, Supplier<Stream<Identifier>> lister) {
   }
}
