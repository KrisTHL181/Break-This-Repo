package net.minecraft.util.worldupdate;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Reference2FloatMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatMaps;
import it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.LegacyTagFixer;
import net.minecraft.world.level.chunk.storage.RecreatingSimpleRegionStorage;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.LegacyStructureDataHandler;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class WorldUpgrader implements AutoCloseable {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder().setDaemon(true).build();
   private static final String NEW_DIRECTORY_PREFIX = "new_";
   static final Component STATUS_UPGRADING_POI = Component.translatable("optimizeWorld.stage.upgrading.poi");
   static final Component STATUS_FINISHED_POI = Component.translatable("optimizeWorld.stage.finished.poi");
   static final Component STATUS_UPGRADING_ENTITIES = Component.translatable("optimizeWorld.stage.upgrading.entities");
   static final Component STATUS_FINISHED_ENTITIES = Component.translatable("optimizeWorld.stage.finished.entities");
   static final Component STATUS_UPGRADING_CHUNKS = Component.translatable("optimizeWorld.stage.upgrading.chunks");
   static final Component STATUS_FINISHED_CHUNKS = Component.translatable("optimizeWorld.stage.finished.chunks");
   final Registry<LevelStem> dimensions;
   final Set<ResourceKey<Level>> levels;
   final boolean eraseCache;
   final boolean recreateRegionFiles;
   final LevelStorageSource.LevelStorageAccess levelStorage;
   private final Thread thread;
   final DataFixer dataFixer;
   volatile boolean running = true;
   private volatile boolean finished;
   volatile float progress;
   volatile int totalChunks;
   volatile int totalFiles;
   volatile int converted;
   volatile int skipped;
   final Reference2FloatMap<ResourceKey<Level>> progressMap = Reference2FloatMaps.synchronize(new Reference2FloatOpenHashMap());
   volatile Component status = Component.translatable("optimizeWorld.stage.counting");
   static final Pattern REGEX = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");
   final DimensionDataStorage overworldDataStorage;

   public WorldUpgrader(
      LevelStorageSource.LevelStorageAccess p_249922_, DataFixer p_250273_, WorldData p_395352_, RegistryAccess p_334652_, boolean p_250738_, boolean p_335488_
   ) {
      this.dimensions = p_334652_.lookupOrThrow(Registries.LEVEL_STEM);
      this.levels = this.dimensions.registryKeySet().stream().map(Registries::levelStemToLevel).collect(Collectors.toUnmodifiableSet());
      this.eraseCache = p_250738_;
      this.dataFixer = p_250273_;
      this.levelStorage = p_249922_;
      this.overworldDataStorage = new DimensionDataStorage(this.levelStorage.getDimensionPath(Level.OVERWORLD).resolve("data"), p_250273_, p_334652_);
      this.recreateRegionFiles = p_335488_;
      this.thread = THREAD_FACTORY.newThread(this::work);
      this.thread.setUncaughtExceptionHandler((p_18825_, p_18826_) -> {
         LOGGER.error("Error upgrading world", p_18826_);
         this.status = Component.translatable("optimizeWorld.stage.failed");
         this.finished = true;
      });
      this.thread.start();
   }

   public void cancel() {
      this.running = false;

      try {
         this.thread.join();
      } catch (InterruptedException var2) {
      }
   }

   private void work() {
      long i = Util.getMillis();
      LOGGER.info("Upgrading entities");
      new WorldUpgrader.EntityUpgrader().upgrade();
      LOGGER.info("Upgrading POIs");
      new WorldUpgrader.PoiUpgrader().upgrade();
      LOGGER.info("Upgrading blocks");
      new WorldUpgrader.ChunkUpgrader().upgrade();
      this.overworldDataStorage.saveAndJoin();
      i = Util.getMillis() - i;
      LOGGER.info("World optimizaton finished after {} seconds", i / 1000L);
      this.finished = true;
   }

   public boolean isFinished() {
      return this.finished;
   }

   public Set<ResourceKey<Level>> levels() {
      return this.levels;
   }

   public float dimensionProgress(ResourceKey<Level> p_18828_) {
      return this.progressMap.getFloat(p_18828_);
   }

   public float getProgress() {
      return this.progress;
   }

   public int getTotalChunks() {
      return this.totalChunks;
   }

   public int getConverted() {
      return this.converted;
   }

   public int getSkipped() {
      return this.skipped;
   }

   public Component getStatus() {
      return this.status;
   }

   @Override
   public void close() {
      this.overworldDataStorage.close();
   }

   static Path resolveRecreateDirectory(Path p_330107_) {
      return p_330107_.resolveSibling("new_" + p_330107_.getFileName().toString());
   }

   abstract class AbstractUpgrader {
      private final Component upgradingStatus;
      private final Component finishedStatus;
      private final String type;
      private final String folderName;
      protected @Nullable CompletableFuture<Void> previousWriteFuture;
      protected final DataFixTypes dataFixType;

      AbstractUpgrader(final DataFixTypes p_332379_, final String p_334432_, final String p_334138_, final Component p_370034_, final Component p_366745_) {
         this.dataFixType = p_332379_;
         this.type = p_334432_;
         this.folderName = p_334138_;
         this.upgradingStatus = p_370034_;
         this.finishedStatus = p_366745_;
      }

      public void upgrade() {
         WorldUpgrader.this.totalFiles = 0;
         WorldUpgrader.this.totalChunks = 0;
         WorldUpgrader.this.converted = 0;
         WorldUpgrader.this.skipped = 0;
         List<WorldUpgrader.DimensionToUpgrade> list = this.getDimensionsToUpgrade();
         if (WorldUpgrader.this.totalChunks != 0) {
            float f = WorldUpgrader.this.totalFiles;
            WorldUpgrader.this.status = this.upgradingStatus;

            while (WorldUpgrader.this.running) {
               boolean flag = false;
               float f1 = 0.0F;

               for (WorldUpgrader.DimensionToUpgrade worldupgrader$dimensiontoupgrade : list) {
                  ResourceKey<Level> resourcekey = worldupgrader$dimensiontoupgrade.dimensionKey;
                  ListIterator<WorldUpgrader.FileToUpgrade> listiterator = worldupgrader$dimensiontoupgrade.files;
                  SimpleRegionStorage simpleregionstorage = worldupgrader$dimensiontoupgrade.storage;
                  if (listiterator.hasNext()) {
                     WorldUpgrader.FileToUpgrade worldupgrader$filetoupgrade = listiterator.next();
                     boolean flag1 = true;

                     for (ChunkPos chunkpos : worldupgrader$filetoupgrade.chunksToUpgrade) {
                        flag1 = flag1 && this.processOnePosition(resourcekey, simpleregionstorage, chunkpos);
                        flag = true;
                     }

                     if (WorldUpgrader.this.recreateRegionFiles) {
                        if (flag1) {
                           this.onFileFinished(worldupgrader$filetoupgrade.file);
                        } else {
                           WorldUpgrader.LOGGER.error("Failed to convert region file {}", worldupgrader$filetoupgrade.file.getPath());
                        }
                     }
                  }

                  float f2 = listiterator.nextIndex() / f;
                  WorldUpgrader.this.progressMap.put(resourcekey, f2);
                  f1 += f2;
               }

               WorldUpgrader.this.progress = f1;
               if (!flag) {
                  break;
               }
            }

            WorldUpgrader.this.status = this.finishedStatus;

            for (WorldUpgrader.DimensionToUpgrade worldupgrader$dimensiontoupgrade1 : list) {
               try {
                  worldupgrader$dimensiontoupgrade1.storage.close();
               } catch (Exception exception) {
                  WorldUpgrader.LOGGER.error("Error upgrading chunk", exception);
               }
            }
         }
      }

      private List<WorldUpgrader.DimensionToUpgrade> getDimensionsToUpgrade() {
         List<WorldUpgrader.DimensionToUpgrade> list = Lists.newArrayList();

         for (ResourceKey<Level> resourcekey : WorldUpgrader.this.levels) {
            RegionStorageInfo regionstorageinfo = new RegionStorageInfo(WorldUpgrader.this.levelStorage.getLevelId(), resourcekey, this.type);
            Path path = WorldUpgrader.this.levelStorage.getDimensionPath(resourcekey).resolve(this.folderName);
            SimpleRegionStorage simpleregionstorage = this.createStorage(regionstorageinfo, path);
            ListIterator<WorldUpgrader.FileToUpgrade> listiterator = this.getFilesToProcess(regionstorageinfo, path);
            list.add(new WorldUpgrader.DimensionToUpgrade(resourcekey, simpleregionstorage, listiterator));
         }

         return list;
      }

      protected abstract SimpleRegionStorage createStorage(RegionStorageInfo var1, Path var2);

      private ListIterator<WorldUpgrader.FileToUpgrade> getFilesToProcess(RegionStorageInfo p_332870_, Path p_331013_) {
         List<WorldUpgrader.FileToUpgrade> list = getAllChunkPositions(p_332870_, p_331013_);
         WorldUpgrader.this.totalFiles = WorldUpgrader.this.totalFiles + list.size();
         WorldUpgrader.this.totalChunks = WorldUpgrader.this.totalChunks + list.stream().mapToInt(p_328536_ -> p_328536_.chunksToUpgrade.size()).sum();
         return list.listIterator();
      }

      private static List<WorldUpgrader.FileToUpgrade> getAllChunkPositions(RegionStorageInfo p_330333_, Path p_330743_) {
         File[] afile = p_330743_.toFile().listFiles((p_336334_, p_329184_) -> p_329184_.endsWith(".mca"));
         if (afile == null) {
            return List.of();
         }

         List<WorldUpgrader.FileToUpgrade> list = Lists.newArrayList();

         for (File file1 : afile) {
            Matcher matcher = WorldUpgrader.REGEX.matcher(file1.getName());
            if (matcher.matches()) {
               int i = Integer.parseInt(matcher.group(1)) << 5;
               int j = Integer.parseInt(matcher.group(2)) << 5;
               List<ChunkPos> list1 = Lists.newArrayList();

               try (RegionFile regionfile = new RegionFile(p_330333_, file1.toPath(), p_330743_, true)) {
                  for (int k = 0; k < 32; k++) {
                     for (int l = 0; l < 32; l++) {
                        ChunkPos chunkpos = new ChunkPos(k + i, l + j);
                        if (regionfile.doesChunkExist(chunkpos)) {
                           list1.add(chunkpos);
                        }
                     }
                  }

                  if (!list1.isEmpty()) {
                     list.add(new WorldUpgrader.FileToUpgrade(regionfile, list1));
                  }
               } catch (Throwable throwable) {
                  WorldUpgrader.LOGGER.error("Failed to read chunks from region file {}", file1.toPath(), throwable);
               }
            }
         }

         return list;
      }

      private boolean processOnePosition(ResourceKey<Level> p_328452_, SimpleRegionStorage p_451726_, ChunkPos p_332028_) {
         boolean flag = false;

         try {
            flag = this.tryProcessOnePosition(p_451726_, p_332028_, p_328452_);
         } catch (CompletionException | ReportedException reportedexception) {
            Throwable throwable = reportedexception.getCause();
            if (!(throwable instanceof IOException)) {
               throw reportedexception;
            }

            WorldUpgrader.LOGGER.error("Error upgrading chunk {}", p_332028_, throwable);
         }

         if (flag) {
            WorldUpgrader.this.converted++;
         } else {
            WorldUpgrader.this.skipped++;
         }

         return flag;
      }

      protected abstract boolean tryProcessOnePosition(SimpleRegionStorage var1, ChunkPos var2, ResourceKey<Level> var3);

      private void onFileFinished(RegionFile p_332836_) {
         if (WorldUpgrader.this.recreateRegionFiles) {
            if (this.previousWriteFuture != null) {
               this.previousWriteFuture.join();
            }

            Path path = p_332836_.getPath();
            Path path1 = path.getParent();
            Path path2 = WorldUpgrader.resolveRecreateDirectory(path1).resolve(path.getFileName().toString());

            try {
               if (path2.toFile().exists()) {
                  Files.delete(path);
                  Files.move(path2, path);
               } else {
                  WorldUpgrader.LOGGER.error("Failed to replace an old region file. New file {} does not exist.", path2);
               }
            } catch (IOException ioexception) {
               WorldUpgrader.LOGGER.error("Failed to replace an old region file", ioexception);
            }
         }
      }
   }

   class ChunkUpgrader extends WorldUpgrader.AbstractUpgrader {
      ChunkUpgrader() {
         super(DataFixTypes.CHUNK, "chunk", "region", WorldUpgrader.STATUS_UPGRADING_CHUNKS, WorldUpgrader.STATUS_FINISHED_CHUNKS);
      }

      @Override
      protected boolean tryProcessOnePosition(SimpleRegionStorage p_455953_, ChunkPos p_331086_, ResourceKey<Level> p_327850_) {
         CompoundTag compoundtag = p_455953_.read(p_331086_).join().orElse(null);
         if (compoundtag != null) {
            int i = NbtUtils.getDataVersion(compoundtag);
            ChunkGenerator chunkgenerator = WorldUpgrader.this.dimensions.getValueOrThrow(Registries.levelToLevelStem(p_327850_)).generator();
            CompoundTag compoundtag1 = p_455953_.upgradeChunkTag(
               compoundtag, -1, ChunkMap.getChunkDataFixContextTag(p_327850_, chunkgenerator.getTypeNameForDataFixer())
            );
            ChunkPos chunkpos = new ChunkPos(compoundtag1.getIntOr("xPos", 0), compoundtag1.getIntOr("zPos", 0));
            if (!chunkpos.equals(p_331086_)) {
               WorldUpgrader.LOGGER.warn("Chunk {} has invalid position {}", p_331086_, chunkpos);
            }

            boolean flag = i < SharedConstants.getCurrentVersion().dataVersion().version();
            if (WorldUpgrader.this.eraseCache) {
               flag = flag || compoundtag1.contains("Heightmaps");
               compoundtag1.remove("Heightmaps");
               flag = flag || compoundtag1.contains("isLightOn");
               compoundtag1.remove("isLightOn");
               ListTag listtag = compoundtag1.getListOrEmpty("sections");

               for (int j = 0; j < listtag.size(); j++) {
                  Optional<CompoundTag> optional = listtag.getCompound(j);
                  if (!optional.isEmpty()) {
                     CompoundTag compoundtag2 = optional.get();
                     flag = flag || compoundtag2.contains("BlockLight");
                     compoundtag2.remove("BlockLight");
                     flag = flag || compoundtag2.contains("SkyLight");
                     compoundtag2.remove("SkyLight");
                  }
               }
            }

            if (flag || WorldUpgrader.this.recreateRegionFiles) {
               if (this.previousWriteFuture != null) {
                  this.previousWriteFuture.join();
               }

               this.previousWriteFuture = p_455953_.write(p_331086_, compoundtag1);
               return true;
            }
         }

         return false;
      }

      @Override
      protected SimpleRegionStorage createStorage(RegionStorageInfo p_328952_, Path p_330137_) {
         Supplier<LegacyTagFixer> supplier = LegacyStructureDataHandler.getLegacyTagFixer(
            p_328952_.dimension(), () -> WorldUpgrader.this.overworldDataStorage, WorldUpgrader.this.dataFixer
         );
         return WorldUpgrader.this.recreateRegionFiles
            ? new RecreatingSimpleRegionStorage(
               p_328952_.withTypeSuffix("source"),
               p_330137_,
               p_328952_.withTypeSuffix("target"),
               WorldUpgrader.resolveRecreateDirectory(p_330137_),
               WorldUpgrader.this.dataFixer,
               true,
               DataFixTypes.CHUNK,
               supplier
            )
            : new SimpleRegionStorage(p_328952_, p_330137_, WorldUpgrader.this.dataFixer, true, DataFixTypes.CHUNK, supplier);
      }
   }

   record DimensionToUpgrade(ResourceKey<Level> dimensionKey, SimpleRegionStorage storage, ListIterator<WorldUpgrader.FileToUpgrade> files) {
   }

   class EntityUpgrader extends WorldUpgrader.SimpleRegionStorageUpgrader {
      EntityUpgrader() {
         super(DataFixTypes.ENTITY_CHUNK, "entities", WorldUpgrader.STATUS_UPGRADING_ENTITIES, WorldUpgrader.STATUS_FINISHED_ENTITIES);
      }

      @Override
      protected CompoundTag upgradeTag(SimpleRegionStorage p_334286_, CompoundTag p_335346_) {
         return p_334286_.upgradeChunkTag(p_335346_, -1);
      }
   }

   record FileToUpgrade(RegionFile file, List<ChunkPos> chunksToUpgrade) {
   }

   class PoiUpgrader extends WorldUpgrader.SimpleRegionStorageUpgrader {
      PoiUpgrader() {
         super(DataFixTypes.POI_CHUNK, "poi", WorldUpgrader.STATUS_UPGRADING_POI, WorldUpgrader.STATUS_FINISHED_POI);
      }

      @Override
      protected CompoundTag upgradeTag(SimpleRegionStorage p_329642_, CompoundTag p_336180_) {
         return p_329642_.upgradeChunkTag(p_336180_, 1945);
      }
   }

   abstract class SimpleRegionStorageUpgrader extends WorldUpgrader.AbstractUpgrader {
      SimpleRegionStorageUpgrader(final DataFixTypes p_332054_, final String p_328150_, final Component p_363471_, final Component p_366494_) {
         super(p_332054_, p_328150_, p_328150_, p_363471_, p_366494_);
      }

      @Override
      protected SimpleRegionStorage createStorage(RegionStorageInfo p_328549_, Path p_333111_) {
         return WorldUpgrader.this.recreateRegionFiles
            ? new RecreatingSimpleRegionStorage(
               p_328549_.withTypeSuffix("source"),
               p_333111_,
               p_328549_.withTypeSuffix("target"),
               WorldUpgrader.resolveRecreateDirectory(p_333111_),
               WorldUpgrader.this.dataFixer,
               true,
               this.dataFixType,
               LegacyTagFixer.EMPTY
            )
            : new SimpleRegionStorage(p_328549_, p_333111_, WorldUpgrader.this.dataFixer, true, this.dataFixType);
      }

      @Override
      protected boolean tryProcessOnePosition(SimpleRegionStorage p_327888_, ChunkPos p_328250_, ResourceKey<Level> p_329996_) {
         CompoundTag compoundtag = p_327888_.read(p_328250_).join().orElse(null);
         if (compoundtag != null) {
            int i = NbtUtils.getDataVersion(compoundtag);
            CompoundTag compoundtag1 = this.upgradeTag(p_327888_, compoundtag);
            boolean flag = i < SharedConstants.getCurrentVersion().dataVersion().version();
            if (flag || WorldUpgrader.this.recreateRegionFiles) {
               if (this.previousWriteFuture != null) {
                  this.previousWriteFuture.join();
               }

               this.previousWriteFuture = p_327888_.write(p_328250_, compoundtag1);
               return true;
            }
         }

         return false;
      }

      protected abstract CompoundTag upgradeTag(SimpleRegionStorage var1, CompoundTag var2);
   }
}
