package net.minecraft.server.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtException;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Mth;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.LegacyStructureDataHandler;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ChunkMap extends SimpleRegionStorage implements ChunkHolder.PlayerProvider, GeneratingChunkMap {
   private static final ChunkResult<List<ChunkAccess>> UNLOADED_CHUNK_LIST_RESULT = ChunkResult.error("Unloaded chunks found in range");
   private static final CompletableFuture<ChunkResult<List<ChunkAccess>>> UNLOADED_CHUNK_LIST_FUTURE = CompletableFuture.completedFuture(
      UNLOADED_CHUNK_LIST_RESULT
   );
   private static final byte CHUNK_TYPE_REPLACEABLE = -1;
   private static final byte CHUNK_TYPE_UNKNOWN = 0;
   private static final byte CHUNK_TYPE_FULL = 1;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int CHUNK_SAVED_PER_TICK = 200;
   private static final int CHUNK_SAVED_EAGERLY_PER_TICK = 20;
   private static final int EAGER_CHUNK_SAVE_COOLDOWN_IN_MILLIS = 10000;
   private static final int MAX_ACTIVE_CHUNK_WRITES = 128;
   public static final int MIN_VIEW_DISTANCE = 2;
   public static final int MAX_VIEW_DISTANCE = 32;
   public static final int FORCED_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);
   private final Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingChunkMap = new Long2ObjectLinkedOpenHashMap();
   private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap = this.updatingChunkMap.clone();
   private final Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads = new Long2ObjectLinkedOpenHashMap();
   private final List<ChunkGenerationTask> pendingGenerationTasks = new ArrayList<>();
   final ServerLevel level;
   private final ThreadedLevelLightEngine lightEngine;
   private final BlockableEventLoop<Runnable> mainThreadExecutor;
   private final RandomState randomState;
   private final ChunkGeneratorStructureState chunkGeneratorState;
   private final TicketStorage ticketStorage;
   private final PoiManager poiManager;
   final LongSet toDrop = new LongOpenHashSet();
   private boolean modified;
   private final ChunkTaskDispatcher worldgenTaskDispatcher;
   private final ChunkTaskDispatcher lightTaskDispatcher;
   private final ChunkStatusUpdateListener chunkStatusListener;
   private final ChunkMap.DistanceManager distanceManager;
   private final String storageName;
   private final PlayerMap playerMap = new PlayerMap();
   private final Int2ObjectMap<ChunkMap.TrackedEntity> entityMap = new Int2ObjectOpenHashMap();
   private final Long2ByteMap chunkTypeCache = new Long2ByteOpenHashMap();
   private final Long2LongMap nextChunkSaveTime = new Long2LongOpenHashMap();
   private final LongSet chunksToEagerlySave = new LongLinkedOpenHashSet();
   private final Queue<Runnable> unloadQueue = Queues.newConcurrentLinkedQueue();
   private final AtomicInteger activeChunkWrites = new AtomicInteger();
   private int serverViewDistance;
   private final WorldGenContext worldGenContext;

   public ChunkMap(
      ServerLevel p_214836_,
      LevelStorageSource.LevelStorageAccess p_214837_,
      DataFixer p_214838_,
      StructureTemplateManager p_214839_,
      Executor p_214840_,
      BlockableEventLoop<Runnable> p_214841_,
      LightChunkGetter p_214842_,
      ChunkGenerator p_214843_,
      ChunkStatusUpdateListener p_214845_,
      Supplier<DimensionDataStorage> p_214846_,
      TicketStorage p_397431_,
      int p_214847_,
      boolean p_214848_
   ) {
      super(
         new RegionStorageInfo(p_214837_.getLevelId(), p_214836_.dimension(), "chunk"),
         p_214837_.getDimensionPath(p_214836_.dimension()).resolve("region"),
         p_214838_,
         p_214848_,
         DataFixTypes.CHUNK,
         LegacyStructureDataHandler.getLegacyTagFixer(p_214836_.dimension(), p_214846_, p_214838_)
      );
      Path path = p_214837_.getDimensionPath(p_214836_.dimension());
      this.storageName = path.getFileName().toString();
      this.level = p_214836_;
      RegistryAccess registryaccess = p_214836_.registryAccess();
      long i = p_214836_.getSeed();
      if (p_214843_ instanceof NoiseBasedChunkGenerator noisebasedchunkgenerator) {
         this.randomState = RandomState.create(noisebasedchunkgenerator.generatorSettings().value(), registryaccess.lookupOrThrow(Registries.NOISE), i);
      } else {
         this.randomState = RandomState.create(NoiseGeneratorSettings.dummy(), registryaccess.lookupOrThrow(Registries.NOISE), i);
      }

      this.chunkGeneratorState = p_214843_.createState(registryaccess.lookupOrThrow(Registries.STRUCTURE_SET), this.randomState, i);
      this.mainThreadExecutor = p_214841_;
      ConsecutiveExecutor consecutiveexecutor1 = new ConsecutiveExecutor(p_214840_, "worldgen");
      this.chunkStatusListener = p_214845_;
      ConsecutiveExecutor consecutiveexecutor = new ConsecutiveExecutor(p_214840_, "light");
      this.worldgenTaskDispatcher = new ChunkTaskDispatcher(consecutiveexecutor1, p_214840_);
      this.lightTaskDispatcher = new ChunkTaskDispatcher(consecutiveexecutor, p_214840_);
      this.lightEngine = new ThreadedLevelLightEngine(p_214842_, this, this.level.dimensionType().hasSkyLight(), consecutiveexecutor, this.lightTaskDispatcher);
      this.distanceManager = new ChunkMap.DistanceManager(p_397431_, p_214840_, p_214841_);
      this.ticketStorage = p_397431_;
      this.poiManager = new PoiManager(
         new RegionStorageInfo(p_214837_.getLevelId(), p_214836_.dimension(), "poi"),
         path.resolve("poi"),
         p_214838_,
         p_214848_,
         registryaccess,
         p_214836_.getServer(),
         p_214836_
      );
      this.setServerViewDistance(p_214847_);
      this.worldGenContext = new WorldGenContext(p_214836_, p_214843_, p_214839_, this.lightEngine, p_214841_, this::setChunkUnsaved);
   }

   private void setChunkUnsaved(ChunkPos p_366347_) {
      this.chunksToEagerlySave.add(p_366347_.toLong());
   }

   protected ChunkGenerator generator() {
      return this.worldGenContext.generator();
   }

   protected ChunkGeneratorStructureState generatorState() {
      return this.chunkGeneratorState;
   }

   protected RandomState randomState() {
      return this.randomState;
   }

   public boolean isChunkTracked(ServerPlayer p_297550_, int p_301041_, int p_300379_) {
      return p_297550_.getChunkTrackingView().contains(p_301041_, p_300379_)
         && !p_297550_.connection.chunkSender.isPending(ChunkPos.asLong(p_301041_, p_300379_));
   }

   private boolean isChunkOnTrackedBorder(ServerPlayer p_299796_, int p_300477_, int p_298067_) {
      if (!this.isChunkTracked(p_299796_, p_300477_, p_298067_)) {
         return false;
      }

      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            if ((i != 0 || j != 0) && !this.isChunkTracked(p_299796_, p_300477_ + i, p_298067_ + j)) {
               return true;
            }
         }
      }

      return false;
   }

   protected ThreadedLevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   public @Nullable ChunkHolder getUpdatingChunkIfPresent(long p_140175_) {
      return (ChunkHolder)this.updatingChunkMap.get(p_140175_);
   }

   protected @Nullable ChunkHolder getVisibleChunkIfPresent(long p_140328_) {
      return (ChunkHolder)this.visibleChunkMap.get(p_140328_);
   }

   public @Nullable ChunkStatus getLatestStatus(long p_422359_) {
      ChunkHolder chunkholder = this.getVisibleChunkIfPresent(p_422359_);
      return chunkholder != null ? chunkholder.getLatestStatus() : null;
   }

   protected IntSupplier getChunkQueueLevel(long p_140372_) {
      return () -> {
         ChunkHolder chunkholder = this.getVisibleChunkIfPresent(p_140372_);
         return chunkholder == null
            ? ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1
            : Math.min(chunkholder.getQueueLevel(), ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
      };
   }

   public String getChunkDebugData(ChunkPos p_140205_) {
      ChunkHolder chunkholder = this.getVisibleChunkIfPresent(p_140205_.toLong());
      if (chunkholder == null) {
         return "null";
      }

      String s = chunkholder.getTicketLevel() + "\n";
      ChunkStatus chunkstatus = chunkholder.getLatestStatus();
      ChunkAccess chunkaccess = chunkholder.getLatestChunk();
      if (chunkstatus != null) {
         s = s + "St: §" + chunkstatus.getIndex() + chunkstatus + "§r\n";
      }

      if (chunkaccess != null) {
         s = s + "Ch: §" + chunkaccess.getPersistedStatus().getIndex() + chunkaccess.getPersistedStatus() + "§r\n";
      }

      FullChunkStatus fullchunkstatus = chunkholder.getFullStatus();
      s = s + "§" + fullchunkstatus.ordinal() + fullchunkstatus;
      return s + "§r";
   }

   CompletableFuture<ChunkResult<List<ChunkAccess>>> getChunkRangeFuture(ChunkHolder p_281446_, int p_282030_, IntFunction<ChunkStatus> p_282923_) {
      if (p_282030_ == 0) {
         ChunkStatus chunkstatus1 = p_282923_.apply(0);
         return p_281446_.scheduleChunkGenerationTask(chunkstatus1, this).thenApply(p_326378_ -> p_326378_.map(List::of));
      }

      int i = Mth.square(p_282030_ * 2 + 1);
      List<CompletableFuture<ChunkResult<ChunkAccess>>> list = new ArrayList<>(i);
      ChunkPos chunkpos = p_281446_.getPos();

      for (int j = -p_282030_; j <= p_282030_; j++) {
         for (int k = -p_282030_; k <= p_282030_; k++) {
            int l = Math.max(Math.abs(k), Math.abs(j));
            long i1 = ChunkPos.asLong(chunkpos.x + k, chunkpos.z + j);
            ChunkHolder chunkholder = this.getUpdatingChunkIfPresent(i1);
            if (chunkholder == null) {
               return UNLOADED_CHUNK_LIST_FUTURE;
            }

            ChunkStatus chunkstatus = p_282923_.apply(l);
            list.add(chunkholder.scheduleChunkGenerationTask(chunkstatus, this));
         }
      }

      return Util.sequence(list).thenApply(p_341212_ -> {
         List<ChunkAccess> list1 = new ArrayList<>(p_341212_.size());

         for (ChunkResult<ChunkAccess> chunkresult : p_341212_) {
            if (chunkresult == null) {
               throw this.debugFuturesAndCreateReportedException(new IllegalStateException("At least one of the chunk futures were null"), "n/a");
            }

            ChunkAccess chunkaccess = chunkresult.orElse(null);
            if (chunkaccess == null) {
               return UNLOADED_CHUNK_LIST_RESULT;
            }

            list1.add(chunkaccess);
         }

         return ChunkResult.of(list1);
      });
   }

   public ReportedException debugFuturesAndCreateReportedException(IllegalStateException p_203752_, String p_203753_) {
      StringBuilder stringbuilder = new StringBuilder();
      Consumer<ChunkHolder> consumer = p_358691_ -> p_358691_.getAllFutures()
         .forEach(
            p_358674_ -> {
               ChunkStatus chunkstatus = (ChunkStatus)p_358674_.getFirst();
               CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = (CompletableFuture<ChunkResult<ChunkAccess>>)p_358674_.getSecond();
               if (completablefuture != null && completablefuture.isDone() && completablefuture.join() == null) {
                  stringbuilder.append(p_358691_.getPos())
                     .append(" - status: ")
                     .append(chunkstatus)
                     .append(" future: ")
                     .append(completablefuture)
                     .append(System.lineSeparator());
               }
            }
         );
      stringbuilder.append("Updating:").append(System.lineSeparator());
      this.updatingChunkMap.values().forEach(consumer);
      stringbuilder.append("Visible:").append(System.lineSeparator());
      this.visibleChunkMap.values().forEach(consumer);
      CrashReport crashreport = CrashReport.forThrowable(p_203752_, "Chunk loading");
      CrashReportCategory crashreportcategory = crashreport.addCategory("Chunk loading");
      crashreportcategory.setDetail("Details", p_203753_);
      crashreportcategory.setDetail("Futures", stringbuilder);
      return new ReportedException(crashreport);
   }

   public CompletableFuture<ChunkResult<LevelChunk>> prepareEntityTickingChunk(ChunkHolder p_281455_) {
      return this.getChunkRangeFuture(p_281455_, 2, p_326375_ -> ChunkStatus.FULL)
         .thenApply(p_326417_ -> p_326417_.map(p_214939_ -> (LevelChunk)p_214939_.get(p_214939_.size() / 2)));
   }

   @Nullable ChunkHolder updateChunkScheduling(long p_140177_, int p_140178_, @Nullable ChunkHolder p_140179_, int p_140180_) {
      if (!ChunkLevel.isLoaded(p_140180_) && !ChunkLevel.isLoaded(p_140178_)) {
         return p_140179_;
      }

      if (p_140179_ != null) {
         p_140179_.setTicketLevel(p_140178_);
      }

      if (p_140179_ != null) {
         if (!ChunkLevel.isLoaded(p_140178_)) {
            this.toDrop.add(p_140177_);
         } else {
            this.toDrop.remove(p_140177_);
         }
      }

      if (ChunkLevel.isLoaded(p_140178_) && p_140179_ == null) {
         p_140179_ = (ChunkHolder)this.pendingUnloads.remove(p_140177_);
         if (p_140179_ != null) {
            p_140179_.setTicketLevel(p_140178_);
         } else {
            p_140179_ = new ChunkHolder(new ChunkPos(p_140177_), p_140178_, this.level, this.lightEngine, this::onLevelChange, this);
         }

         this.updatingChunkMap.put(p_140177_, p_140179_);
         this.modified = true;
      }

      return p_140179_;
   }

   private void onLevelChange(ChunkPos p_364597_, IntSupplier p_362051_, int p_368537_, IntConsumer p_367036_) {
      this.worldgenTaskDispatcher.onLevelChange(p_364597_, p_362051_, p_368537_, p_367036_);
      this.lightTaskDispatcher.onLevelChange(p_364597_, p_362051_, p_368537_, p_367036_);
   }

   @Override
   public void close() throws IOException {
      try {
         this.worldgenTaskDispatcher.close();
         this.lightTaskDispatcher.close();
         this.poiManager.close();
      } finally {
         super.close();
      }
   }

   protected void saveAllChunks(boolean p_140319_) {
      if (p_140319_) {
         List<ChunkHolder> list = this.visibleChunkMap
            .values()
            .stream()
            .filter(ChunkHolder::wasAccessibleSinceLastSave)
            .peek(ChunkHolder::refreshAccessibility)
            .toList();
         MutableBoolean mutableboolean = new MutableBoolean();

         do {
            mutableboolean.setFalse();
            list.stream()
               .map(p_358685_ -> {
                  this.mainThreadExecutor.managedBlock(p_358685_::isReadyForSaving);
                  return p_358685_.getLatestChunk();
               })
               .filter(p_203088_ -> p_203088_ instanceof ImposterProtoChunk || p_203088_ instanceof LevelChunk)
               .filter(this::save)
               .forEach(p_203051_ -> mutableboolean.setTrue());
         } while (mutableboolean.isTrue());

         this.poiManager.flushAll();
         this.processUnloads(() -> true);
         this.synchronize(true).join();
      } else {
         this.nextChunkSaveTime.clear();
         long i = Util.getMillis();
         ObjectIterator var4 = this.visibleChunkMap.values().iterator();

         while (var4.hasNext()) {
            ChunkHolder chunkholder = (ChunkHolder)var4.next();
            this.saveChunkIfNeeded(chunkholder, i);
         }
      }
   }

   protected void tick(BooleanSupplier p_140281_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("poi");
      this.poiManager.tick(p_140281_);
      profilerfiller.popPush("chunk_unload");
      if (!this.level.noSave()) {
         this.processUnloads(p_140281_);
      }

      profilerfiller.pop();
   }

   public boolean hasWork() {
      return this.lightEngine.hasLightWork()
         || !this.pendingUnloads.isEmpty()
         || !this.updatingChunkMap.isEmpty()
         || this.poiManager.hasWork()
         || !this.toDrop.isEmpty()
         || !this.unloadQueue.isEmpty()
         || this.worldgenTaskDispatcher.hasWork()
         || this.lightTaskDispatcher.hasWork()
         || this.distanceManager.hasTickets();
   }

   private void processUnloads(BooleanSupplier p_140354_) {
      for (LongIterator longiterator = this.toDrop.iterator(); longiterator.hasNext(); longiterator.remove()) {
         long i = longiterator.nextLong();
         ChunkHolder chunkholder = (ChunkHolder)this.updatingChunkMap.get(i);
         if (chunkholder != null) {
            this.updatingChunkMap.remove(i);
            this.pendingUnloads.put(i, chunkholder);
            this.modified = true;
            this.scheduleUnload(i, chunkholder);
         }
      }

      int j = Math.max(0, this.unloadQueue.size() - 2000);

      Runnable runnable;
      while ((j > 0 || p_140354_.getAsBoolean()) && (runnable = this.unloadQueue.poll()) != null) {
         j--;
         runnable.run();
      }

      this.saveChunksEagerly(p_140354_);
   }

   private void saveChunksEagerly(BooleanSupplier p_362354_) {
      long i = Util.getMillis();
      int j = 0;
      LongIterator longiterator = this.chunksToEagerlySave.iterator();

      while (j < 20 && this.activeChunkWrites.get() < 128 && p_362354_.getAsBoolean() && longiterator.hasNext()) {
         long k = longiterator.nextLong();
         ChunkHolder chunkholder = (ChunkHolder)this.visibleChunkMap.get(k);
         ChunkAccess chunkaccess = chunkholder != null ? chunkholder.getLatestChunk() : null;
         if (chunkaccess == null || !chunkaccess.isUnsaved()) {
            longiterator.remove();
         } else if (this.saveChunkIfNeeded(chunkholder, i)) {
            j++;
            longiterator.remove();
         }
      }
   }

   private void scheduleUnload(long p_140182_, ChunkHolder p_140183_) {
      CompletableFuture<?> completablefuture = p_140183_.getSaveSyncFuture();
      completablefuture.thenRunAsync(() -> {
         CompletableFuture<?> completablefuture1 = p_140183_.getSaveSyncFuture();
         if (completablefuture1 != completablefuture) {
            this.scheduleUnload(p_140182_, p_140183_);
         } else {
            ChunkAccess chunkaccess = p_140183_.getLatestChunk();
            if (this.pendingUnloads.remove(p_140182_, p_140183_) && chunkaccess != null) {
               if (chunkaccess instanceof LevelChunk levelchunk) {
                  levelchunk.setLoaded(false);
               }

               this.save(chunkaccess);
               if (chunkaccess instanceof LevelChunk levelchunk1) {
                  this.level.unload(levelchunk1);
               }

               this.lightEngine.updateChunkStatus(chunkaccess.getPos());
               this.lightEngine.tryScheduleUpdate();
               this.nextChunkSaveTime.remove(chunkaccess.getPos().toLong());
            }
         }
      }, this.unloadQueue::add).whenComplete((p_358668_, p_358669_) -> {
         if (p_358669_ != null) {
            LOGGER.error("Failed to save chunk {}", p_140183_.getPos(), p_358669_);
         }
      });
   }

   protected boolean promoteChunkMap() {
      if (!this.modified) {
         return false;
      }

      this.visibleChunkMap = this.updatingChunkMap.clone();
      this.modified = false;
      return true;
   }

   private CompletableFuture<ChunkAccess> scheduleChunkLoad(ChunkPos p_140418_) {
      CompletableFuture<Optional<SerializableChunkData>> completablefuture = this.readChunk(p_140418_).thenApplyAsync(p_358681_ -> p_358681_.map(p_449141_ -> {
         SerializableChunkData serializablechunkdata = SerializableChunkData.parse(this.level, this.level.palettedContainerFactory(), p_449141_);
         if (serializablechunkdata == null) {
            LOGGER.error("Chunk file at {} is missing level data, skipping", p_140418_);
         }

         return serializablechunkdata;
      }), Util.backgroundExecutor().forName("parseChunk"));
      CompletableFuture<?> completablefuture1 = this.poiManager.prefetch(p_140418_);
      return completablefuture.<Object, Optional<SerializableChunkData>>thenCombine(
            (CompletionStage<? extends Object>)completablefuture1, (p_358678_, p_358679_) -> p_358678_
         )
         .thenApplyAsync(p_449136_ -> {
            Profiler.get().incrementCounter("chunkLoad");
            if (p_449136_.isPresent()) {
               ChunkAccess chunkaccess = p_449136_.get().read(this.level, this.poiManager, this.storageInfo(), p_140418_);
               this.markPosition(p_140418_, chunkaccess.getPersistedStatus().getChunkType());
               return chunkaccess;
            } else {
               return this.createEmptyChunk(p_140418_);
            }
         }, this.mainThreadExecutor)
         .exceptionallyAsync(p_326410_ -> this.handleChunkLoadFailure(p_326410_, p_140418_), this.mainThreadExecutor);
   }

   private ChunkAccess handleChunkLoadFailure(Throwable p_214902_, ChunkPos p_214903_) {
      Throwable throwable = p_214902_ instanceof CompletionException completionexception ? completionexception.getCause() : p_214902_;
      Throwable throwable1 = throwable instanceof ReportedException reportedexception ? reportedexception.getCause() : throwable;
      boolean flag1 = throwable1 instanceof Error;
      boolean flag = throwable1 instanceof IOException || throwable1 instanceof NbtException;
      if (!flag1) {
         if (!flag) {
         }

         this.level.getServer().reportChunkLoadFailure(throwable1, this.storageInfo(), p_214903_);
         return this.createEmptyChunk(p_214903_);
      } else {
         CrashReport crashreport = CrashReport.forThrowable(p_214902_, "Exception loading chunk");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Chunk being loaded");
         crashreportcategory.setDetail("pos", p_214903_);
         this.markPositionReplaceable(p_214903_);
         throw new ReportedException(crashreport);
      }
   }

   private ChunkAccess createEmptyChunk(ChunkPos p_214962_) {
      this.markPositionReplaceable(p_214962_);
      return new ProtoChunk(p_214962_, UpgradeData.EMPTY, this.level, this.level.palettedContainerFactory(), null);
   }

   private void markPositionReplaceable(ChunkPos p_140423_) {
      this.chunkTypeCache.put(p_140423_.toLong(), (byte)-1);
   }

   private byte markPosition(ChunkPos p_140230_, ChunkType p_328844_) {
      return this.chunkTypeCache.put(p_140230_.toLong(), (byte)(p_328844_ == ChunkType.PROTOCHUNK ? -1 : 1));
   }

   @Override
   public GenerationChunkHolder acquireGeneration(long p_344717_) {
      ChunkHolder chunkholder = (ChunkHolder)this.updatingChunkMap.get(p_344717_);
      chunkholder.increaseGenerationRefCount();
      return chunkholder;
   }

   @Override
   public void releaseGeneration(GenerationChunkHolder p_343926_) {
      p_343926_.decreaseGenerationRefCount();
   }

   @Override
   public CompletableFuture<ChunkAccess> applyStep(GenerationChunkHolder p_344519_, ChunkStep p_344471_, StaticCache2D<GenerationChunkHolder> p_343410_) {
      ChunkPos chunkpos = p_344519_.getPos();
      if (p_344471_.targetStatus() == ChunkStatus.EMPTY) {
         return this.scheduleChunkLoad(chunkpos);
      }

      try {
         GenerationChunkHolder generationchunkholder = p_343410_.get(chunkpos.x, chunkpos.z);
         ChunkAccess chunkaccess = generationchunkholder.getChunkIfPresentUnchecked(p_344471_.targetStatus().getParent());
         if (chunkaccess == null) {
            throw new IllegalStateException("Parent chunk missing");
         } else {
            return p_344471_.apply(this.worldGenContext, p_343410_, chunkaccess);
         }
      } catch (Exception exception) {
         exception.getStackTrace();
         CrashReport crashreport = CrashReport.forThrowable(exception, "Exception generating new chunk");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Chunk to be generated");
         crashreportcategory.setDetail("Status being generated", () -> p_344471_.targetStatus().getName());
         crashreportcategory.setDetail("Location", String.format(Locale.ROOT, "%d,%d", chunkpos.x, chunkpos.z));
         crashreportcategory.setDetail("Position hash", ChunkPos.asLong(chunkpos.x, chunkpos.z));
         crashreportcategory.setDetail("Generator", this.generator());
         this.mainThreadExecutor.execute(() -> {
            throw new ReportedException(crashreport);
         });
         throw new ReportedException(crashreport);
      }
   }

   @Override
   public ChunkGenerationTask scheduleGenerationTask(ChunkStatus p_345229_, ChunkPos p_342957_) {
      ChunkGenerationTask chunkgenerationtask = ChunkGenerationTask.create(this, p_345229_, p_342957_);
      this.pendingGenerationTasks.add(chunkgenerationtask);
      return chunkgenerationtask;
   }

   private void runGenerationTask(ChunkGenerationTask p_344392_) {
      GenerationChunkHolder generationchunkholder = p_344392_.getCenter();
      this.worldgenTaskDispatcher.submit(() -> {
         CompletableFuture<?> completablefuture = p_344392_.runUntilWait();
         if (completablefuture != null) {
            completablefuture.thenRun(() -> this.runGenerationTask(p_344392_));
         }
      }, generationchunkholder.getPos().toLong(), generationchunkholder::getQueueLevel);
   }

   @Override
   public void runGenerationTasks() {
      this.pendingGenerationTasks.forEach(this::runGenerationTask);
      this.pendingGenerationTasks.clear();
   }

   public CompletableFuture<ChunkResult<LevelChunk>> prepareTickingChunk(ChunkHolder p_143054_) {
      CompletableFuture<ChunkResult<List<ChunkAccess>>> completablefuture = this.getChunkRangeFuture(p_143054_, 1, p_326384_ -> ChunkStatus.FULL);
      return completablefuture.thenApplyAsync(p_358666_ -> p_358666_.map(p_358689_ -> {
         LevelChunk levelchunk = (LevelChunk)p_358689_.get(p_358689_.size() / 2);
         levelchunk.postProcessGeneration(this.level);
         this.level.startTickingChunk(levelchunk);
         CompletableFuture<?> completablefuture1 = p_143054_.getSendSyncFuture();
         if (completablefuture1.isDone()) {
            this.onChunkReadyToSend(p_143054_, levelchunk);
         } else {
            completablefuture1.thenAcceptAsync(p_296572_ -> this.onChunkReadyToSend(p_143054_, levelchunk), this.mainThreadExecutor);
         }

         return levelchunk;
      }), this.mainThreadExecutor);
   }

   private void onChunkReadyToSend(ChunkHolder p_370260_, LevelChunk p_299599_) {
      ChunkPos chunkpos = p_299599_.getPos();

      for (ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
         if (serverplayer.getChunkTrackingView().contains(chunkpos)) {
            markChunkPendingToSend(serverplayer, p_299599_);
         }
      }

      this.level.getChunkSource().onChunkReadyToSend(p_370260_);
      this.level.debugSynchronizers().registerChunk(p_299599_);
   }

   public CompletableFuture<ChunkResult<LevelChunk>> prepareAccessibleChunk(ChunkHolder p_143110_) {
      return this.getChunkRangeFuture(p_143110_, 1, ChunkLevel::getStatusAroundFullChunk)
         .thenApply(p_326418_ -> p_326418_.map(p_203092_ -> (LevelChunk)p_203092_.get(p_203092_.size() / 2)));
   }

   Stream<ChunkHolder> allChunksWithAtLeastStatus(ChunkStatus p_425975_) {
      int i = ChunkLevel.byStatus(p_425975_);
      return this.visibleChunkMap.values().stream().filter(p_421438_ -> p_421438_.getTicketLevel() <= i);
   }

   private boolean saveChunkIfNeeded(ChunkHolder p_198875_, long p_368144_) {
      if (p_198875_.wasAccessibleSinceLastSave() && p_198875_.isReadyForSaving()) {
         ChunkAccess chunkaccess = p_198875_.getLatestChunk();
         if (!(chunkaccess instanceof ImposterProtoChunk) && !(chunkaccess instanceof LevelChunk)) {
            return false;
         }

         if (!chunkaccess.isUnsaved()) {
            return false;
         }

         long i = chunkaccess.getPos().toLong();
         long j = this.nextChunkSaveTime.getOrDefault(i, -1L);
         if (p_368144_ < j) {
            return false;
         }

         boolean flag = this.save(chunkaccess);
         p_198875_.refreshAccessibility();
         if (flag) {
            this.nextChunkSaveTime.put(i, p_368144_ + 10000L);
         }

         return flag;
      } else {
         return false;
      }
   }

   private boolean save(ChunkAccess p_140259_) {
      this.poiManager.flush(p_140259_.getPos());
      if (!p_140259_.tryMarkSaved()) {
         return false;
      }

      ChunkPos chunkpos = p_140259_.getPos();

      try {
         ChunkStatus chunkstatus = p_140259_.getPersistedStatus();
         if (chunkstatus.getChunkType() != ChunkType.LEVELCHUNK) {
            if (this.isExistingChunkFull(chunkpos)) {
               return false;
            }

            if (chunkstatus == ChunkStatus.EMPTY && p_140259_.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
               return false;
            }
         }

         Profiler.get().incrementCounter("chunkSave");
         this.activeChunkWrites.incrementAndGet();
         SerializableChunkData serializablechunkdata = SerializableChunkData.copyOf(this.level, p_140259_);
         CompletableFuture<CompoundTag> completablefuture = CompletableFuture.supplyAsync(serializablechunkdata::write, Util.backgroundExecutor());
         this.write(chunkpos, completablefuture::join).handle((p_449138_, p_449139_) -> {
            if (p_449139_ != null) {
               this.level.getServer().reportChunkSaveFailure(p_449139_, this.storageInfo(), chunkpos);
            }

            this.activeChunkWrites.decrementAndGet();
            return null;
         });
         this.markPosition(chunkpos, chunkstatus.getChunkType());
         return true;
      } catch (Exception exception) {
         this.level.getServer().reportChunkSaveFailure(exception, this.storageInfo(), chunkpos);
         return false;
      }
   }

   private boolean isExistingChunkFull(ChunkPos p_140426_) {
      byte b0 = this.chunkTypeCache.get(p_140426_.toLong());
      if (b0 != 0) {
         return b0 == 1;
      }

      CompoundTag compoundtag;
      try {
         compoundtag = this.readChunk(p_140426_).join().orElse(null);
         if (compoundtag == null) {
            this.markPositionReplaceable(p_140426_);
            return false;
         }
      } catch (Exception exception) {
         LOGGER.error("Failed to read chunk {}", p_140426_, exception);
         this.markPositionReplaceable(p_140426_);
         return false;
      }

      ChunkType chunktype = SerializableChunkData.getChunkStatusFromTag(compoundtag).getChunkType();
      return this.markPosition(p_140426_, chunktype) == 1;
   }

   protected void setServerViewDistance(int p_300944_) {
      int i = Mth.clamp(p_300944_, 2, 32);
      if (i != this.serverViewDistance) {
         this.serverViewDistance = i;
         this.distanceManager.updatePlayerTickets(this.serverViewDistance);

         for (ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
            this.updateChunkTracking(serverplayer);
         }
      }
   }

   int getPlayerViewDistance(ServerPlayer p_298592_) {
      return Mth.clamp(p_298592_.requestedViewDistance(), 2, this.serverViewDistance);
   }

   private void markChunkPendingToSend(ServerPlayer p_297974_, ChunkPos p_298062_) {
      LevelChunk levelchunk = this.getChunkToSend(p_298062_.toLong());
      if (levelchunk != null) {
         markChunkPendingToSend(p_297974_, levelchunk);
      }
   }

   private static void markChunkPendingToSend(ServerPlayer p_299135_, LevelChunk p_301128_) {
      p_299135_.connection.chunkSender.markChunkPendingToSend(p_301128_);
   }

   private static void dropChunk(ServerPlayer p_300364_, ChunkPos p_299541_) {
      p_300364_.connection.chunkSender.dropChunk(p_300364_, p_299541_);
   }

   public @Nullable LevelChunk getChunkToSend(long p_299683_) {
      ChunkHolder chunkholder = this.getVisibleChunkIfPresent(p_299683_);
      return chunkholder == null ? null : chunkholder.getChunkToSend();
   }

   public int size() {
      return this.visibleChunkMap.size();
   }

   public net.minecraft.server.level.DistanceManager getDistanceManager() {
      return this.distanceManager;
   }

   void dumpChunks(Writer p_140275_) throws IOException {
      CsvOutput csvoutput = CsvOutput.builder()
         .addColumn("x")
         .addColumn("z")
         .addColumn("level")
         .addColumn("in_memory")
         .addColumn("status")
         .addColumn("full_status")
         .addColumn("accessible_ready")
         .addColumn("ticking_ready")
         .addColumn("entity_ticking_ready")
         .addColumn("ticket")
         .addColumn("spawning")
         .addColumn("block_entity_count")
         .addColumn("ticking_ticket")
         .addColumn("ticking_level")
         .addColumn("block_ticks")
         .addColumn("fluid_ticks")
         .build(p_140275_);
      ObjectBidirectionalIterator var3 = this.visibleChunkMap.long2ObjectEntrySet().iterator();

      while (var3.hasNext()) {
         Entry<ChunkHolder> entry = (Entry<ChunkHolder>)var3.next();
         long i = entry.getLongKey();
         ChunkPos chunkpos = new ChunkPos(i);
         ChunkHolder chunkholder = (ChunkHolder)entry.getValue();
         Optional<ChunkAccess> optional = Optional.ofNullable(chunkholder.getLatestChunk());
         Optional<LevelChunk> optional1 = optional.flatMap(p_214932_ -> p_214932_ instanceof LevelChunk ? Optional.of((LevelChunk)p_214932_) : Optional.empty());
         csvoutput.writeRow(
            chunkpos.x,
            chunkpos.z,
            chunkholder.getTicketLevel(),
            optional.isPresent(),
            optional.map(ChunkAccess::getPersistedStatus).orElse(null),
            optional1.map(LevelChunk::getFullStatus).orElse(null),
            printFuture(chunkholder.getFullChunkFuture()),
            printFuture(chunkholder.getTickingChunkFuture()),
            printFuture(chunkholder.getEntityTickingChunkFuture()),
            this.ticketStorage.getTicketDebugString(i, false),
            this.anyPlayerCloseEnoughForSpawning(chunkpos),
            optional1.<Integer>map(p_214953_ -> p_214953_.getBlockEntities().size()).orElse(0),
            this.ticketStorage.getTicketDebugString(i, true),
            this.distanceManager.getChunkLevel(i, true),
            optional1.<Integer>map(p_214946_ -> p_214946_.getBlockTicks().count()).orElse(0),
            optional1.<Integer>map(p_214937_ -> p_214937_.getFluidTicks().count()).orElse(0)
         );
      }
   }

   private static String printFuture(CompletableFuture<ChunkResult<LevelChunk>> p_140279_) {
      try {
         ChunkResult<LevelChunk> chunkresult = p_140279_.getNow(null);
         if (chunkresult != null) {
            return chunkresult.isSuccess() ? "done" : "unloaded";
         } else {
            return "not completed";
         }
      } catch (CompletionException completionexception) {
         return "failed " + completionexception.getCause().getMessage();
      } catch (CancellationException cancellationexception) {
         return "cancelled";
      }
   }

   private CompletableFuture<Optional<CompoundTag>> readChunk(ChunkPos p_214964_) {
      return this.read(p_214964_).thenApplyAsync(p_214907_ -> p_214907_.map(this::upgradeChunkTag), Util.backgroundExecutor().forName("upgradeChunk"));
   }

   private CompoundTag upgradeChunkTag(CompoundTag p_214948_) {
      return this.upgradeChunkTag(p_214948_, -1, getChunkDataFixContextTag(this.level.dimension(), this.generator().getTypeNameForDataFixer()));
   }

   public static CompoundTag getChunkDataFixContextTag(ResourceKey<Level> p_455737_, Optional<ResourceKey<MapCodec<? extends ChunkGenerator>>> p_460440_) {
      CompoundTag compoundtag = new CompoundTag();
      compoundtag.putString("dimension", p_455737_.identifier().toString());
      p_460440_.ifPresent(p_449143_ -> compoundtag.putString("generator", p_449143_.identifier().toString()));
      return compoundtag;
   }

   void collectSpawningChunks(List<LevelChunk> p_391803_) {
      LongIterator longiterator = this.distanceManager.getSpawnCandidateChunks();

      while (longiterator.hasNext()) {
         ChunkHolder chunkholder = (ChunkHolder)this.visibleChunkMap.get(longiterator.nextLong());
         if (chunkholder != null) {
            LevelChunk levelchunk = chunkholder.getTickingChunk();
            if (levelchunk != null && this.anyPlayerCloseEnoughForSpawningInternal(chunkholder.getPos())) {
               p_391803_.add(levelchunk);
            }
         }
      }
   }

   void forEachBlockTickingChunk(Consumer<LevelChunk> p_394956_) {
      this.distanceManager.forEachEntityTickingChunk(p_390134_ -> {
         ChunkHolder chunkholder = (ChunkHolder)this.visibleChunkMap.get(p_390134_);
         if (chunkholder != null) {
            LevelChunk levelchunk = chunkholder.getTickingChunk();
            if (levelchunk != null) {
               p_394956_.accept(levelchunk);
            }
         }
      });
   }

   boolean anyPlayerCloseEnoughForSpawning(ChunkPos p_183880_) {
      TriState tristate = this.distanceManager.hasPlayersNearby(p_183880_.toLong());
      return tristate == TriState.DEFAULT ? this.anyPlayerCloseEnoughForSpawningInternal(p_183880_) : tristate.toBoolean(true);
   }

   boolean anyPlayerCloseEnoughTo(BlockPos p_451713_, int p_450896_) {
      Vec3 vec3 = new Vec3(p_451713_);

      for (ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
         if (this.playerIsCloseEnoughTo(serverplayer, vec3, p_450896_)) {
            return true;
         }
      }

      return false;
   }

   private boolean anyPlayerCloseEnoughForSpawningInternal(ChunkPos p_365445_) {
      for (ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
         if (this.playerIsCloseEnoughForSpawning(serverplayer, p_365445_)) {
            return true;
         }
      }

      return false;
   }

   public List<ServerPlayer> getPlayersCloseForSpawning(ChunkPos p_183889_) {
      long i = p_183889_.toLong();
      if (!this.distanceManager.hasPlayersNearby(i).toBoolean(true)) {
         return List.of();
      }

      Builder<ServerPlayer> builder = ImmutableList.builder();

      for (ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
         if (this.playerIsCloseEnoughForSpawning(serverplayer, p_183889_)) {
            builder.add(serverplayer);
         }
      }

      return builder.build();
   }

   private boolean playerIsCloseEnoughForSpawning(ServerPlayer p_183752_, ChunkPos p_183753_) {
      if (p_183752_.isSpectator()) {
         return false;
      }

      double d0 = euclideanDistanceSquared(p_183753_, p_183752_.position());
      return d0 < 16384.0;
   }

   private boolean playerIsCloseEnoughTo(ServerPlayer p_454182_, Vec3 p_453823_, int p_459133_) {
      if (p_454182_.isSpectator()) {
         return false;
      }

      double d0 = p_454182_.position().distanceTo(p_453823_);
      return d0 < p_459133_;
   }

   private static double euclideanDistanceSquared(ChunkPos p_140227_, Vec3 p_395153_) {
      double d0 = SectionPos.sectionToBlockCoord(p_140227_.x, 8);
      double d1 = SectionPos.sectionToBlockCoord(p_140227_.z, 8);
      double d2 = d0 - p_395153_.x;
      double d3 = d1 - p_395153_.z;
      return d2 * d2 + d3 * d3;
   }

   private boolean skipPlayer(ServerPlayer p_140330_) {
      return p_140330_.isSpectator() && !this.level.getGameRules().get(GameRules.SPECTATORS_GENERATE_CHUNKS);
   }

   void updatePlayerStatus(ServerPlayer p_140193_, boolean p_140194_) {
      boolean flag = this.skipPlayer(p_140193_);
      boolean flag1 = this.playerMap.ignoredOrUnknown(p_140193_);
      if (p_140194_) {
         this.playerMap.addPlayer(p_140193_, flag);
         this.updatePlayerPos(p_140193_);
         if (!flag) {
            this.distanceManager.addPlayer(SectionPos.of(p_140193_), p_140193_);
         }

         p_140193_.setChunkTrackingView(ChunkTrackingView.EMPTY);
         this.updateChunkTracking(p_140193_);
      } else {
         SectionPos sectionpos = p_140193_.getLastSectionPos();
         this.playerMap.removePlayer(p_140193_);
         if (!flag1) {
            this.distanceManager.removePlayer(sectionpos, p_140193_);
         }

         this.applyChunkTrackingView(p_140193_, ChunkTrackingView.EMPTY);
      }
   }

   private void updatePlayerPos(ServerPlayer p_140374_) {
      SectionPos sectionpos = SectionPos.of(p_140374_);
      p_140374_.setLastSectionPos(sectionpos);
   }

   public void move(ServerPlayer p_140185_) {
      ObjectIterator sectionpos = this.entityMap.values().iterator();

      while (sectionpos.hasNext()) {
         ChunkMap.TrackedEntity chunkmap$trackedentity = (ChunkMap.TrackedEntity)sectionpos.next();
         if (chunkmap$trackedentity.entity == p_140185_) {
            chunkmap$trackedentity.updatePlayers(this.level.players());
         } else {
            chunkmap$trackedentity.updatePlayer(p_140185_);
         }
      }

      SectionPos sectionposx = p_140185_.getLastSectionPos();
      SectionPos sectionpos1 = SectionPos.of(p_140185_);
      boolean flag = this.playerMap.ignored(p_140185_);
      boolean flag1 = this.skipPlayer(p_140185_);
      boolean flag2 = sectionposx.asLong() != sectionpos1.asLong();
      if (flag2 || flag != flag1) {
         this.updatePlayerPos(p_140185_);
         if (!flag) {
            this.distanceManager.removePlayer(sectionposx, p_140185_);
         }

         if (!flag1) {
            this.distanceManager.addPlayer(sectionpos1, p_140185_);
         }

         if (!flag && flag1) {
            this.playerMap.ignorePlayer(p_140185_);
         }

         if (flag && !flag1) {
            this.playerMap.unIgnorePlayer(p_140185_);
         }

         this.updateChunkTracking(p_140185_);
      }
   }

   private void updateChunkTracking(ServerPlayer p_183755_) {
      ChunkPos chunkpos = p_183755_.chunkPosition();
      int i = this.getPlayerViewDistance(p_183755_);
      if (!(
         p_183755_.getChunkTrackingView() instanceof ChunkTrackingView.Positioned chunktrackingview$positioned
            && chunktrackingview$positioned.center().equals(chunkpos)
            && chunktrackingview$positioned.viewDistance() == i
      )) {
         this.applyChunkTrackingView(p_183755_, ChunkTrackingView.of(chunkpos, i));
      }
   }

   private void applyChunkTrackingView(ServerPlayer p_301380_, ChunkTrackingView p_301057_) {
      if (p_301380_.level() == this.level) {
         ChunkTrackingView chunktrackingview = p_301380_.getChunkTrackingView();
         if (p_301057_ instanceof ChunkTrackingView.Positioned chunktrackingview$positioned
            && !(
               chunktrackingview instanceof ChunkTrackingView.Positioned chunktrackingview$positioned1
                  && chunktrackingview$positioned1.center().equals(chunktrackingview$positioned.center())
            )) {
            p_301380_.connection
               .send(new ClientboundSetChunkCacheCenterPacket(chunktrackingview$positioned.center().x, chunktrackingview$positioned.center().z));
         }

         ChunkTrackingView.difference(
            chunktrackingview, p_301057_, p_296566_ -> this.markChunkPendingToSend(p_301380_, p_296566_), p_296568_ -> dropChunk(p_301380_, p_296568_)
         );
         p_301380_.setChunkTrackingView(p_301057_);
      }
   }

   @Override
   public List<ServerPlayer> getPlayers(ChunkPos p_183801_, boolean p_183802_) {
      Set<ServerPlayer> set = this.playerMap.getAllPlayers();
      Builder<ServerPlayer> builder = ImmutableList.builder();

      for (ServerPlayer serverplayer : set) {
         if (p_183802_ && this.isChunkOnTrackedBorder(serverplayer, p_183801_.x, p_183801_.z)
            || !p_183802_ && this.isChunkTracked(serverplayer, p_183801_.x, p_183801_.z)) {
            builder.add(serverplayer);
         }
      }

      return builder.build();
   }

   protected void addEntity(Entity p_140200_) {
      if (!(p_140200_ instanceof EnderDragonPart)) {
         EntityType<?> entitytype = p_140200_.getType();
         int i = entitytype.clientTrackingRange() * 16;
         if (i != 0) {
            int j = entitytype.updateInterval();
            if (this.entityMap.containsKey(p_140200_.getId())) {
               throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Entity is already tracked!"));
            }

            ChunkMap.TrackedEntity chunkmap$trackedentity = new ChunkMap.TrackedEntity(p_140200_, i, j, entitytype.trackDeltas());
            this.entityMap.put(p_140200_.getId(), chunkmap$trackedentity);
            chunkmap$trackedentity.updatePlayers(this.level.players());
            if (p_140200_ instanceof ServerPlayer serverplayer) {
               this.updatePlayerStatus(serverplayer, true);
               ObjectIterator var7 = this.entityMap.values().iterator();

               while (var7.hasNext()) {
                  ChunkMap.TrackedEntity chunkmap$trackedentity1 = (ChunkMap.TrackedEntity)var7.next();
                  if (chunkmap$trackedentity1.entity != serverplayer) {
                     chunkmap$trackedentity1.updatePlayer(serverplayer);
                  }
               }
            }
         }
      }
   }

   protected void removeEntity(Entity p_140332_) {
      if (p_140332_ instanceof ServerPlayer serverplayer) {
         this.updatePlayerStatus(serverplayer, false);
         ObjectIterator var3 = this.entityMap.values().iterator();

         while (var3.hasNext()) {
            ChunkMap.TrackedEntity chunkmap$trackedentity = (ChunkMap.TrackedEntity)var3.next();
            chunkmap$trackedentity.removePlayer(serverplayer);
         }
      }

      ChunkMap.TrackedEntity chunkmap$trackedentity1 = (ChunkMap.TrackedEntity)this.entityMap.remove(p_140332_.getId());
      if (chunkmap$trackedentity1 != null) {
         chunkmap$trackedentity1.broadcastRemoved();
      }
   }

   protected void tick() {
      for (ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
         this.updateChunkTracking(serverplayer);
      }

      List<ServerPlayer> list = Lists.newArrayList();
      List<ServerPlayer> list1 = this.level.players();
      ObjectIterator var3 = this.entityMap.values().iterator();

      while (var3.hasNext()) {
         ChunkMap.TrackedEntity chunkmap$trackedentity = (ChunkMap.TrackedEntity)var3.next();
         SectionPos sectionpos = chunkmap$trackedentity.lastSectionPos;
         SectionPos sectionpos1 = SectionPos.of(chunkmap$trackedentity.entity);
         boolean flag = !Objects.equals(sectionpos, sectionpos1);
         if (flag) {
            chunkmap$trackedentity.updatePlayers(list1);
            Entity entity = chunkmap$trackedentity.entity;
            if (entity instanceof ServerPlayer) {
               list.add((ServerPlayer)entity);
            }

            chunkmap$trackedentity.lastSectionPos = sectionpos1;
         }

         if (flag || chunkmap$trackedentity.entity.needsSync || this.distanceManager.inEntityTickingRange(sectionpos1.chunk().toLong())) {
            chunkmap$trackedentity.serverEntity.sendChanges();
         }
      }

      if (!list.isEmpty()) {
         var3 = this.entityMap.values().iterator();

         while (var3.hasNext()) {
            ChunkMap.TrackedEntity chunkmap$trackedentity1 = (ChunkMap.TrackedEntity)var3.next();
            chunkmap$trackedentity1.updatePlayers(list);
         }
      }
   }

   public void sendToTrackingPlayers(Entity p_429776_, Packet<? super ClientGamePacketListener> p_423882_) {
      ChunkMap.TrackedEntity chunkmap$trackedentity = (ChunkMap.TrackedEntity)this.entityMap.get(p_429776_.getId());
      if (chunkmap$trackedentity != null) {
         chunkmap$trackedentity.sendToTrackingPlayers(p_423882_);
      }
   }

   public void sendToTrackingPlayersFiltered(Entity p_427109_, Packet<? super ClientGamePacketListener> p_429658_, Predicate<ServerPlayer> p_431114_) {
      ChunkMap.TrackedEntity chunkmap$trackedentity = (ChunkMap.TrackedEntity)this.entityMap.get(p_427109_.getId());
      if (chunkmap$trackedentity != null) {
         chunkmap$trackedentity.sendToTrackingPlayersFiltered(p_429658_, p_431114_);
      }
   }

   protected void sendToTrackingPlayersAndSelf(Entity p_423309_, Packet<? super ClientGamePacketListener> p_428484_) {
      ChunkMap.TrackedEntity chunkmap$trackedentity = (ChunkMap.TrackedEntity)this.entityMap.get(p_423309_.getId());
      if (chunkmap$trackedentity != null) {
         chunkmap$trackedentity.sendToTrackingPlayersAndSelf(p_428484_);
      }
   }

   public boolean isTrackedByAnyPlayer(Entity p_422766_) {
      ChunkMap.TrackedEntity chunkmap$trackedentity = (ChunkMap.TrackedEntity)this.entityMap.get(p_422766_.getId());
      return chunkmap$trackedentity != null ? !chunkmap$trackedentity.seenBy.isEmpty() : false;
   }

   public void forEachEntityTrackedBy(ServerPlayer p_422911_, Consumer<Entity> p_425947_) {
      ObjectIterator var3 = this.entityMap.values().iterator();

      while (var3.hasNext()) {
         ChunkMap.TrackedEntity chunkmap$trackedentity = (ChunkMap.TrackedEntity)var3.next();
         if (chunkmap$trackedentity.seenBy.contains(p_422911_.connection)) {
            p_425947_.accept(chunkmap$trackedentity.entity);
         }
      }
   }

   public void resendBiomesForChunks(List<ChunkAccess> p_275577_) {
      Map<ServerPlayer, List<LevelChunk>> map = new HashMap<>();

      for (ChunkAccess chunkaccess : p_275577_) {
         ChunkPos chunkpos = chunkaccess.getPos();
         LevelChunk levelchunk;
         if (chunkaccess instanceof LevelChunk levelchunk1) {
            levelchunk = levelchunk1;
         } else {
            levelchunk = this.level.getChunk(chunkpos.x, chunkpos.z);
         }

         for (ServerPlayer serverplayer : this.getPlayers(chunkpos, false)) {
            map.computeIfAbsent(serverplayer, p_274834_ -> new ArrayList<>()).add(levelchunk);
         }
      }

      map.forEach((p_296569_, p_296570_) -> p_296569_.connection.send(ClientboundChunksBiomesPacket.forChunks((List<LevelChunk>)p_296570_)));
   }

   protected PoiManager getPoiManager() {
      return this.poiManager;
   }

   public String getStorageName() {
      return this.storageName;
   }

   void onFullChunkStatusChange(ChunkPos p_287612_, FullChunkStatus p_287685_) {
      this.chunkStatusListener.onChunkStatusChange(p_287612_, p_287685_);
   }

   public void waitForLightBeforeSending(ChunkPos p_297696_, int p_300649_) {
      int i = p_300649_ + 1;
      ChunkPos.rangeClosed(p_297696_, i).forEach(p_296576_ -> {
         ChunkHolder chunkholder = this.getVisibleChunkIfPresent(p_296576_.toLong());
         if (chunkholder != null) {
            chunkholder.addSendDependency(this.lightEngine.waitForPendingTasks(p_296576_.x, p_296576_.z));
         }
      });
   }

   public void forEachReadyToSendChunk(Consumer<LevelChunk> p_426957_) {
      ObjectIterator var2 = this.visibleChunkMap.values().iterator();

      while (var2.hasNext()) {
         ChunkHolder chunkholder = (ChunkHolder)var2.next();
         LevelChunk levelchunk = chunkholder.getChunkToSend();
         if (levelchunk != null) {
            p_426957_.accept(levelchunk);
         }
      }
   }

   class DistanceManager extends net.minecraft.server.level.DistanceManager {
      protected DistanceManager(final TicketStorage p_397181_, final Executor p_140459_, final Executor p_140460_) {
         super(p_397181_, p_140459_, p_140460_);
      }

      @Override
      protected boolean isChunkToRemove(long p_140462_) {
         return ChunkMap.this.toDrop.contains(p_140462_);
      }

      @Override
      protected @Nullable ChunkHolder getChunk(long p_140469_) {
         return ChunkMap.this.getUpdatingChunkIfPresent(p_140469_);
      }

      @Override
      protected @Nullable ChunkHolder updateChunkScheduling(long p_140464_, int p_140465_, @Nullable ChunkHolder p_140466_, int p_140467_) {
         return ChunkMap.this.updateChunkScheduling(p_140464_, p_140465_, p_140466_, p_140467_);
      }
   }

   class TrackedEntity implements ServerEntity.Synchronizer {
      final ServerEntity serverEntity;
      final Entity entity;
      private final int range;
      SectionPos lastSectionPos;
      final Set<ServerPlayerConnection> seenBy = Sets.newIdentityHashSet();

      public TrackedEntity(final Entity p_140478_, final int p_140479_, final int p_140480_, final boolean p_140481_) {
         this.serverEntity = new ServerEntity(ChunkMap.this.level, p_140478_, p_140480_, p_140481_, this);
         this.entity = p_140478_;
         this.range = p_140479_;
         this.lastSectionPos = SectionPos.of(p_140478_);
      }

      @Override
      public boolean equals(Object p_140506_) {
         return p_140506_ instanceof ChunkMap.TrackedEntity ? ((ChunkMap.TrackedEntity)p_140506_).entity.getId() == this.entity.getId() : false;
      }

      @Override
      public int hashCode() {
         return this.entity.getId();
      }

      @Override
      public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> p_140490_) {
         for (ServerPlayerConnection serverplayerconnection : this.seenBy) {
            serverplayerconnection.send(p_140490_);
         }
      }

      @Override
      public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> p_430953_) {
         this.sendToTrackingPlayers(p_430953_);
         if (this.entity instanceof ServerPlayer serverplayer) {
            serverplayer.connection.send(p_430953_);
         }
      }

      @Override
      public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> p_426072_, Predicate<ServerPlayer> p_424589_) {
         for (ServerPlayerConnection serverplayerconnection : this.seenBy) {
            if (p_424589_.test(serverplayerconnection.getPlayer())) {
               serverplayerconnection.send(p_426072_);
            }
         }
      }

      public void broadcastRemoved() {
         for (ServerPlayerConnection serverplayerconnection : this.seenBy) {
            this.serverEntity.removePairing(serverplayerconnection.getPlayer());
         }
      }

      public void removePlayer(ServerPlayer p_140486_) {
         if (this.seenBy.remove(p_140486_.connection)) {
            this.serverEntity.removePairing(p_140486_);
            if (this.seenBy.isEmpty()) {
               ChunkMap.this.level.debugSynchronizers().dropEntity(this.entity);
            }
         }
      }

      public void updatePlayer(ServerPlayer p_140498_) {
         if (p_140498_ != this.entity) {
            Vec3 vec3 = p_140498_.position().subtract(this.entity.position());
            int i = ChunkMap.this.getPlayerViewDistance(p_140498_);
            double d0 = Math.min(this.getEffectiveRange(), i * 16);
            double d1 = vec3.x * vec3.x + vec3.z * vec3.z;
            double d2 = d0 * d0;
            boolean flag = d1 <= d2
               && this.entity.broadcastToPlayer(p_140498_)
               && ChunkMap.this.isChunkTracked(p_140498_, this.entity.chunkPosition().x, this.entity.chunkPosition().z);
            if (flag) {
               if (this.seenBy.add(p_140498_.connection)) {
                  this.serverEntity.addPairing(p_140498_);
                  if (this.seenBy.size() == 1) {
                     ChunkMap.this.level.debugSynchronizers().registerEntity(this.entity);
                  }

                  ChunkMap.this.level.debugSynchronizers().startTrackingEntity(p_140498_, this.entity);
               }
            } else {
               this.removePlayer(p_140498_);
            }
         }
      }

      private int scaledRange(int p_140484_) {
         return ChunkMap.this.level.getServer().getScaledTrackingDistance(p_140484_);
      }

      private int getEffectiveRange() {
         int i = this.range;

         for (Entity entity : this.entity.getIndirectPassengers()) {
            int j = entity.getType().clientTrackingRange() * 16;
            if (j > i) {
               i = j;
            }
         }

         return this.scaledRange(i);
      }

      public void updatePlayers(List<ServerPlayer> p_140488_) {
         for (ServerPlayer serverplayer : p_140488_) {
            this.updatePlayer(serverplayer);
         }
      }
   }
}
