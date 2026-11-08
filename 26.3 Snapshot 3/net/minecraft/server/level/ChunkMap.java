package net.minecraft.server.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
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
import java.util.stream.Stream;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
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
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
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
   private final ServerLevel level;
   private final ThreadedLevelLightEngine lightEngine;
   private final BlockableEventLoop<Runnable> mainThreadExecutor;
   private final RandomState randomState;
   private final ChunkGeneratorStructureState chunkGeneratorState;
   private final TicketStorage ticketStorage;
   private final PoiManager poiManager;
   private final LongSet toDrop = new LongOpenHashSet();
   private boolean modified;
   private final ChunkTaskDispatcher worldgenTaskDispatcher;
   private final ChunkTaskDispatcher lightTaskDispatcher;
   private final ChunkStatusUpdateListener chunkStatusListener;
   private final ChunkMap.DistanceManager distanceManager;
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
      final ServerLevel level,
      final LevelStorageSource.LevelStorageAccess levelStorage,
      final DataFixer dataFixer,
      final StructureTemplateManager structureManager,
      final Executor executor,
      final BlockableEventLoop<Runnable> mainThreadExecutor,
      final LightChunkGetter chunkGetter,
      final ChunkGenerator generator,
      final ChunkStatusUpdateListener chunkStatusListener,
      final TicketStorage ticketStorage,
      final int serverViewDistance,
      final boolean syncWrites
   ) {
      super(
         new RegionStorageInfo(levelStorage.getLevelId(), level.dimension(), "chunk"),
         levelStorage.getDimensionPath(level.dimension()).resolve("region"),
         dataFixer,
         syncWrites,
         DataFixTypes.CHUNK
      );
      Path storageFolder = levelStorage.getDimensionPath(level.dimension());
      this.level = level;
      RegistryAccess registryAccess = level.registryAccess();
      long levelSeed = level.getSeed();
      if (generator instanceof NoiseBasedChunkGenerator noiseGenerator) {
         this.randomState = RandomState.create(registryAccess.lookupOrThrow(Registries.NOISE), levelSeed, noiseGenerator.generatorSettings().value());
      } else {
         this.randomState = RandomState.create(
            registryAccess.lookupOrThrow(Registries.NOISE), levelSeed, false, Blocks.STONE.defaultBlockState(), 63, NoiseRouterData.none(), List.of()
         );
      }

      this.chunkGeneratorState = generator.createState(registryAccess.lookupOrThrow(Registries.STRUCTURE_SET), this.randomState, levelSeed);
      this.mainThreadExecutor = mainThreadExecutor;
      ConsecutiveExecutor worldgen = new ConsecutiveExecutor(executor, "worldgen");
      this.chunkStatusListener = chunkStatusListener;
      ConsecutiveExecutor light = new ConsecutiveExecutor(executor, "light");
      this.worldgenTaskDispatcher = new ChunkTaskDispatcher(worldgen, executor);
      this.lightTaskDispatcher = new ChunkTaskDispatcher(light, executor);
      this.lightEngine = new ThreadedLevelLightEngine(chunkGetter, this, this.level.dimensionType().hasSkyLight(), light, this.lightTaskDispatcher);
      this.distanceManager = new ChunkMap.DistanceManager(ticketStorage, executor, mainThreadExecutor);
      this.ticketStorage = ticketStorage;
      this.poiManager = new PoiManager(
         new RegionStorageInfo(levelStorage.getLevelId(), level.dimension(), "poi"),
         storageFolder.resolve("poi"),
         dataFixer,
         syncWrites,
         registryAccess,
         level.getServer(),
         level
      );
      this.setServerViewDistance(serverViewDistance);
      this.worldGenContext = new WorldGenContext(level, generator, structureManager, this.lightEngine, mainThreadExecutor, this::setChunkUnsaved);
   }

   private void setChunkUnsaved(final ChunkPos chunkPos) {
      this.chunksToEagerlySave.add(chunkPos.pack());
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

   public boolean isChunkTracked(final ServerPlayer player, final int chunkX, final int chunkZ) {
      return player.getChunkTrackingView().contains(chunkX, chunkZ) && !player.connection.chunkSender.isPending(ChunkPos.pack(chunkX, chunkZ));
   }

   private boolean isChunkOnTrackedBorder(final ServerPlayer player, final int chunkX, final int chunkZ) {
      if (!this.isChunkTracked(player, chunkX, chunkZ)) {
         return false;
      }

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            if ((dx != 0 || dz != 0) && !this.isChunkTracked(player, chunkX + dx, chunkZ + dz)) {
               return true;
            }
         }
      }

      return false;
   }

   protected ThreadedLevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   public @Nullable ChunkHolder getUpdatingChunkIfPresent(final long key) {
      return (ChunkHolder)this.updatingChunkMap.get(key);
   }

   protected @Nullable ChunkHolder getVisibleChunkIfPresent(final long key) {
      return (ChunkHolder)this.visibleChunkMap.get(key);
   }

   public @Nullable ChunkStatus getLatestStatus(final long key) {
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(key);
      return chunkHolder != null ? chunkHolder.getLatestStatus() : null;
   }

   protected IntSupplier getChunkQueueLevel(final long pos) {
      return () -> {
         ChunkHolder chunk = this.getVisibleChunkIfPresent(pos);
         return chunk == null
            ? ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1
            : Math.min(chunk.getQueueLevel(), ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
      };
   }

   public String getChunkDebugData(final ChunkPos pos) {
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(pos.pack());
      if (chunkHolder == null) {
         return "null";
      }

      String result = chunkHolder.getTicketLevel() + "\n";
      ChunkStatus status = chunkHolder.getLatestStatus();
      ChunkAccess chunk = chunkHolder.getLatestChunk();
      if (status != null) {
         result = result + "St: §" + status.getIndex() + status + "§r\n";
      }

      if (chunk != null) {
         result = result + "Ch: §" + chunk.getPersistedStatus().getIndex() + chunk.getPersistedStatus() + "§r\n";
      }

      FullChunkStatus fullStatus = chunkHolder.getFullStatus();
      result = result + '§' + fullStatus.ordinal() + fullStatus;
      return result + "§r";
   }

   CompletableFuture<ChunkResult<List<ChunkAccess>>> getChunkRangeFuture(
      final ChunkHolder centerChunk, final int range, final IntFunction<ChunkStatus> distanceToStatus
   ) {
      if (range == 0) {
         ChunkStatus status = distanceToStatus.apply(0);
         return centerChunk.scheduleChunkGenerationTask(status, this).thenApply(r -> r.map(List::of));
      }

      int chunkCount = Mth.square(range * 2 + 1);
      List<CompletableFuture<ChunkResult<ChunkAccess>>> deps = new ArrayList<>(chunkCount);
      ChunkPos centerPos = centerChunk.getPos();

      for (int z = -range; z <= range; z++) {
         for (int x = -range; x <= range; x++) {
            int distance = Math.max(Math.abs(x), Math.abs(z));
            long chunkNode = ChunkPos.pack(centerPos.x() + x, centerPos.z() + z);
            ChunkHolder chunk = this.getUpdatingChunkIfPresent(chunkNode);
            if (chunk == null) {
               return UNLOADED_CHUNK_LIST_FUTURE;
            }

            ChunkStatus depStatus = distanceToStatus.apply(distance);
            deps.add(chunk.scheduleChunkGenerationTask(depStatus, this));
         }
      }

      return Util.sequence(deps).thenApply(chunkResults -> {
         List<ChunkAccess> chunks = new ArrayList<>(chunkResults.size());

         for (ChunkResult<ChunkAccess> chunkResult : chunkResults) {
            if (chunkResult == null) {
               throw this.debugFuturesAndCreateReportedException(new IllegalStateException("At least one of the chunk futures were null"), "n/a");
            }

            ChunkAccess chunkx = chunkResult.orElse(null);
            if (chunkx == null) {
               return UNLOADED_CHUNK_LIST_RESULT;
            }

            chunks.add(chunkx);
         }

         return ChunkResult.of(chunks);
      });
   }

   public ReportedException debugFuturesAndCreateReportedException(final IllegalStateException exception, final String details) {
      StringBuilder sb = new StringBuilder();
      Consumer<ChunkHolder> addToDebug = holder -> holder.getAllFutures().forEach(pair -> {
         ChunkStatus status = (ChunkStatus)pair.getFirst();
         CompletableFuture<ChunkResult<ChunkAccess>> future = (CompletableFuture<ChunkResult<ChunkAccess>>)pair.getSecond();
         if (future != null && future.isDone() && future.join() == null) {
            sb.append(holder.getPos()).append(" - status: ").append(status).append(" future: ").append(future).append(System.lineSeparator());
         }
      });
      sb.append("Updating:").append(System.lineSeparator());
      this.updatingChunkMap.values().forEach(addToDebug);
      sb.append("Visible:").append(System.lineSeparator());
      this.visibleChunkMap.values().forEach(addToDebug);
      CrashReport report = CrashReport.forThrowable(exception, "Chunk loading");
      CrashReportCategory category = report.addCategory("Chunk loading");
      category.setDetail("Details", details);
      category.setDetail("Futures", sb);
      return new ReportedException(report);
   }

   public CompletableFuture<ChunkResult<LevelChunk>> prepareEntityTickingChunk(final ChunkHolder chunk) {
      return this.getChunkRangeFuture(chunk, 2, distance -> ChunkStatus.FULL)
         .thenApply(chunkResult -> chunkResult.map(list -> (LevelChunk)list.get(list.size() / 2)));
   }

   private @Nullable ChunkHolder updateChunkScheduling(final long node, final int level, @Nullable ChunkHolder chunk, final int oldLevel) {
      if (!ChunkLevel.isLoaded(oldLevel) && !ChunkLevel.isLoaded(level)) {
         return chunk;
      }

      if (chunk != null) {
         chunk.setTicketLevel(level);
      }

      if (chunk != null) {
         if (!ChunkLevel.isLoaded(level)) {
            this.toDrop.add(node);
         } else {
            this.toDrop.remove(node);
         }
      }

      if (ChunkLevel.isLoaded(level) && chunk == null) {
         chunk = (ChunkHolder)this.pendingUnloads.remove(node);
         if (chunk != null) {
            chunk.setTicketLevel(level);
         } else {
            chunk = new ChunkHolder(ChunkPos.unpack(node), level, this.level, this.lightEngine, this::onLevelChange, this);
         }

         this.updatingChunkMap.put(node, chunk);
         this.modified = true;
      }

      return chunk;
   }

   private void onLevelChange(final ChunkPos pos, final IntSupplier oldLevel, final int newLevel, final IntConsumer setQueueLevel) {
      this.worldgenTaskDispatcher.onLevelChange(pos, oldLevel, newLevel, setQueueLevel);
      this.lightTaskDispatcher.onLevelChange(pos, oldLevel, newLevel, setQueueLevel);
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

   protected void saveAllChunks(final boolean flushStorage) {
      if (flushStorage) {
         List<ChunkHolder> chunksToSave = this.visibleChunkMap
            .values()
            .stream()
            .filter(ChunkHolder::wasAccessibleSinceLastSave)
            .peek(ChunkHolder::refreshAccessibility)
            .toList();
         MutableBoolean didWork = new MutableBoolean();

         do {
            didWork.setFalse();
            chunksToSave.stream()
               .map(chunkx -> {
                  this.mainThreadExecutor.managedBlock(chunkx::isReadyForSaving);
                  return chunkx.getLatestChunk();
               })
               .filter(chunkAccess -> chunkAccess instanceof ImposterProtoChunk || chunkAccess instanceof LevelChunk)
               .filter(this::save)
               .forEach(c -> didWork.setTrue());
         } while (didWork.isTrue());

         this.poiManager.flushAll();
         this.processUnloads(() -> true);
         this.synchronize(true).join();
      } else {
         this.nextChunkSaveTime.clear();
         long now = Util.getMillis();
         ObjectIterator var4 = this.visibleChunkMap.values().iterator();

         while (var4.hasNext()) {
            ChunkHolder chunk = (ChunkHolder)var4.next();
            this.saveChunkIfNeeded(chunk, now);
         }
      }
   }

   protected void tick(final BooleanSupplier haveTime) {
      ProfilerFiller profiler = Profiler.get();
      profiler.push("poi");
      this.poiManager.tick(haveTime);
      profiler.popPush("chunk_unload");
      if (!this.level.noSave()) {
         this.processUnloads(haveTime);
      }

      profiler.pop();
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

   private void processUnloads(final BooleanSupplier haveTime) {
      for (LongIterator iterator = this.toDrop.iterator(); iterator.hasNext(); iterator.remove()) {
         long pos = iterator.nextLong();
         ChunkHolder chunkHolder = (ChunkHolder)this.updatingChunkMap.get(pos);
         if (chunkHolder != null) {
            this.updatingChunkMap.remove(pos);
            this.pendingUnloads.put(pos, chunkHolder);
            this.modified = true;
            this.scheduleUnload(pos, chunkHolder);
         }
      }

      int minimalNumberOfChunksToProcess = Math.max(0, this.unloadQueue.size() - 2000);

      Runnable unloadTask;
      while ((minimalNumberOfChunksToProcess > 0 || haveTime.getAsBoolean()) && (unloadTask = this.unloadQueue.poll()) != null) {
         minimalNumberOfChunksToProcess--;
         unloadTask.run();
      }

      this.saveChunksEagerly(haveTime);
   }

   private void saveChunksEagerly(final BooleanSupplier haveTime) {
      long now = Util.getMillis();
      int eagerlySavedCount = 0;
      LongIterator iterator = this.chunksToEagerlySave.iterator();

      while (eagerlySavedCount < 20 && this.activeChunkWrites.get() < 128 && haveTime.getAsBoolean() && iterator.hasNext()) {
         long chunkPos = iterator.nextLong();
         ChunkHolder chunkHolder = (ChunkHolder)this.visibleChunkMap.get(chunkPos);
         ChunkAccess latestChunk = chunkHolder != null ? chunkHolder.getLatestChunk() : null;
         if (latestChunk == null || !latestChunk.isUnsaved()) {
            iterator.remove();
         } else if (this.saveChunkIfNeeded(chunkHolder, now)) {
            eagerlySavedCount++;
            iterator.remove();
         }
      }
   }

   private void scheduleUnload(final long pos, final ChunkHolder chunkHolder) {
      CompletableFuture<?> saveSyncFuture = chunkHolder.getSaveSyncFuture();
      saveSyncFuture.thenRunAsync(() -> {
         CompletableFuture<?> currentFuture = chunkHolder.getSaveSyncFuture();
         if (currentFuture != saveSyncFuture) {
            this.scheduleUnload(pos, chunkHolder);
         } else {
            ChunkAccess chunk = chunkHolder.getLatestChunk();
            if (this.pendingUnloads.remove(pos, chunkHolder) && chunk != null) {
               if (chunk instanceof LevelChunk levelChunk) {
                  levelChunk.setLoaded(false);
               }

               this.save(chunk);
               if (chunk instanceof LevelChunk levelChunk) {
                  this.level.unload(levelChunk);
               }

               this.lightEngine.updateChunkStatus(chunk.getPos());
               this.lightEngine.tryScheduleUpdate();
               this.nextChunkSaveTime.remove(chunk.getPos().pack());
            }
         }
      }, this.unloadQueue::add).whenComplete((ignored, throwable) -> {
         if (throwable != null) {
            LOGGER.error("Failed to save chunk {}", chunkHolder.getPos(), throwable);
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

   private CompletableFuture<ChunkAccess> scheduleChunkLoad(final ChunkPos pos) {
      CompletableFuture<Optional<SerializableChunkData>> chunkDataFuture = this.readChunk(pos).thenApplyAsync(chunkData -> chunkData.map(tag -> {
         SerializableChunkData parsedData = SerializableChunkData.parse(this.level, this.level.palettedContainerFactory(), tag);
         if (parsedData == null) {
            LOGGER.error("Chunk file at {} is missing level data, skipping", pos);
         }

         return parsedData;
      }), Util.backgroundExecutor().forName("parseChunk"));
      CompletableFuture<?> poiFuture = this.poiManager.prefetch(pos);
      return chunkDataFuture.<Object, Optional<SerializableChunkData>>thenCombine(
            (CompletionStage<? extends Object>)poiFuture, (chunkData, ignored) -> chunkData
         )
         .thenApplyAsync(chunkData -> {
            Profiler.get().incrementCounter("chunkLoad");
            if (chunkData.isPresent()) {
               ChunkAccess chunk = chunkData.get().read(this.level, this.poiManager, this.storageInfo(), pos);
               this.markPosition(pos, chunk.getPersistedStatus().getChunkType());
               return chunk;
            } else {
               return this.createEmptyChunk(pos);
            }
         }, this.mainThreadExecutor)
         .exceptionallyAsync(throwable -> this.handleChunkLoadFailure(throwable, pos), this.mainThreadExecutor);
   }

   private ChunkAccess handleChunkLoadFailure(final Throwable throwable, final ChunkPos pos) {
      Throwable unwrapped = throwable instanceof CompletionException e ? e.getCause() : throwable;
      Throwable cause = unwrapped instanceof ReportedException e ? e.getCause() : unwrapped;
      boolean alwaysThrow = cause instanceof Error;
      boolean ioException = cause instanceof IOException || cause instanceof NbtException;
      if (!alwaysThrow && ioException) {
         this.level.getServer().reportChunkLoadFailure(cause, this.storageInfo(), pos);
         return this.createEmptyChunk(pos);
      } else {
         CrashReport report = CrashReport.forThrowable(throwable, "Exception loading chunk");
         CrashReportCategory chunkBeingLoaded = report.addCategory("Chunk being loaded");
         chunkBeingLoaded.setDetail("pos", pos);
         this.markPositionReplaceable(pos);
         throw new ReportedException(report);
      }
   }

   private ChunkAccess createEmptyChunk(final ChunkPos pos) {
      this.markPositionReplaceable(pos);
      return new ProtoChunk(pos, UpgradeData.EMPTY, this.level, this.level.palettedContainerFactory(), null);
   }

   private void markPositionReplaceable(final ChunkPos pos) {
      this.chunkTypeCache.put(pos.pack(), (byte)-1);
   }

   private byte markPosition(final ChunkPos pos, final ChunkType type) {
      return this.chunkTypeCache.put(pos.pack(), (byte)(type == ChunkType.PROTOCHUNK ? -1 : 1));
   }

   @Override
   public GenerationChunkHolder acquireGeneration(final long chunkNode) {
      ChunkHolder chunkHolder = (ChunkHolder)this.updatingChunkMap.get(chunkNode);
      chunkHolder.increaseGenerationRefCount();
      return chunkHolder;
   }

   @Override
   public void releaseGeneration(final GenerationChunkHolder chunkHolder) {
      chunkHolder.decreaseGenerationRefCount();
   }

   @Override
   public CompletableFuture<ChunkAccess> applyStep(
      final GenerationChunkHolder chunkHolder, final ChunkStep step, final StaticCache2D<GenerationChunkHolder> cache
   ) {
      ChunkPos pos = chunkHolder.getPos();
      if (step.targetStatus() == ChunkStatus.EMPTY) {
         return this.scheduleChunkLoad(pos);
      }

      try {
         GenerationChunkHolder holder = cache.get(pos.x(), pos.z());
         ChunkAccess centerChunk = holder.getChunkIfPresentUnchecked(step.targetStatus().getParent());
         if (centerChunk == null) {
            throw new IllegalStateException("Parent chunk missing");
         } else {
            return step.apply(this.worldGenContext, cache, centerChunk);
         }
      } catch (Exception e) {
         e.getStackTrace();
         CrashReport report = CrashReport.forThrowable(e, "Exception generating new chunk");
         CrashReportCategory category = report.addCategory("Chunk to be generated");
         category.setDetail("Status being generated", () -> step.targetStatus().getName());
         category.setDetail("Location", String.format(Locale.ROOT, "%d,%d", pos.x(), pos.z()));
         category.setDetail("Position hash", ChunkPos.pack(pos.x(), pos.z()));
         category.setDetail("Generator", this.generator());
         throw new ReportedException(report);
      }
   }

   @Override
   public ChunkGenerationTask scheduleGenerationTask(final ChunkStatus targetStatus, final ChunkPos pos) {
      ChunkGenerationTask task = ChunkGenerationTask.create(this, targetStatus, pos);
      this.pendingGenerationTasks.add(task);
      return task;
   }

   private void runGenerationTask(final ChunkGenerationTask task) {
      GenerationChunkHolder chunk = task.getCenter();
      this.worldgenTaskDispatcher.submit(() -> {
         CompletableFuture<?> future = task.runUntilWait();
         if (future != null) {
            future.thenRun(() -> this.runGenerationTask(task));
         }
      }, chunk.getPos().pack(), chunk::getQueueLevel);
   }

   @Override
   public void runGenerationTasks() {
      this.pendingGenerationTasks.forEach(this::runGenerationTask);
      this.pendingGenerationTasks.clear();
   }

   public CompletableFuture<ChunkResult<LevelChunk>> prepareTickingChunk(final ChunkHolder chunk) {
      CompletableFuture<ChunkResult<List<ChunkAccess>>> future = this.getChunkRangeFuture(chunk, 1, distance -> ChunkStatus.FULL);
      return future.thenApplyAsync(listResult -> listResult.map(list -> {
         LevelChunk levelChunk = (LevelChunk)list.get(list.size() / 2);
         levelChunk.postProcessGeneration(this.level);
         this.level.startTickingChunk(levelChunk);
         CompletableFuture<?> sendSyncFuture = chunk.getSendSyncFuture();
         if (sendSyncFuture.isDone()) {
            this.onChunkReadyToSend(chunk, levelChunk);
         } else {
            sendSyncFuture.thenAcceptAsync(ignored -> this.onChunkReadyToSend(chunk, levelChunk), this.mainThreadExecutor);
         }

         return levelChunk;
      }), this.mainThreadExecutor);
   }

   private void onChunkReadyToSend(final ChunkHolder chunkHolder, final LevelChunk chunk) {
      ChunkPos chunkPos = chunk.getPos();

      for (ServerPlayer player : this.playerMap.getAllPlayers()) {
         if (player.getChunkTrackingView().contains(chunkPos)) {
            markChunkPendingToSend(player, chunk);
         }
      }

      this.level.getChunkSource().onChunkReadyToSend(chunkHolder);
      this.level.debugSynchronizers().registerChunk(chunk);
   }

   public CompletableFuture<ChunkResult<LevelChunk>> prepareAccessibleChunk(final ChunkHolder chunk) {
      return this.getChunkRangeFuture(chunk, 1, ChunkLevel::getStatusAroundFullChunk)
         .thenApply(chunkResult -> chunkResult.map(list -> (LevelChunk)list.get(list.size() / 2)));
   }

   Stream<ChunkHolder> allChunksWithAtLeastStatus(final ChunkStatus status) {
      int level = ChunkLevel.byStatus(status);
      return this.visibleChunkMap.values().stream().filter(chunk -> chunk.getTicketLevel() <= level);
   }

   private boolean saveChunkIfNeeded(final ChunkHolder chunk, final long now) {
      if (chunk.wasAccessibleSinceLastSave() && chunk.isReadyForSaving()) {
         ChunkAccess chunkAccess = chunk.getLatestChunk();
         if (!(chunkAccess instanceof ImposterProtoChunk) && !(chunkAccess instanceof LevelChunk)) {
            return false;
         }

         if (!chunkAccess.isUnsaved()) {
            return false;
         }

         long chunkPos = chunkAccess.getPos().pack();
         long nextSaveTime = this.nextChunkSaveTime.getOrDefault(chunkPos, -1L);
         if (now < nextSaveTime) {
            return false;
         }

         boolean saved = this.save(chunkAccess);
         chunk.refreshAccessibility();
         if (saved) {
            this.nextChunkSaveTime.put(chunkPos, now + 10000L);
         }

         return saved;
      } else {
         return false;
      }
   }

   private boolean save(final ChunkAccess chunk) {
      this.poiManager.flush(chunk.getPos());
      if (!chunk.tryMarkSaved()) {
         return false;
      }

      ChunkPos pos = chunk.getPos();

      try {
         ChunkStatus status = chunk.getPersistedStatus();
         if (status.getChunkType() != ChunkType.LEVELCHUNK) {
            if (this.isExistingChunkFull(pos)) {
               return false;
            }

            if (status == ChunkStatus.EMPTY && chunk.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
               return false;
            }
         }

         Profiler.get().incrementCounter("chunkSave");
         this.activeChunkWrites.incrementAndGet();
         SerializableChunkData data = SerializableChunkData.copyOf(this.level, chunk);
         CompletableFuture<CompoundTag> encodedData = CompletableFuture.supplyAsync(data::write, Util.backgroundExecutor());
         this.write(pos, encodedData::join).handle((ignored, throwable) -> {
            if (throwable != null) {
               this.level.getServer().reportChunkSaveFailure(throwable, this.storageInfo(), pos);
            }

            this.activeChunkWrites.decrementAndGet();
            return null;
         });
         this.markPosition(pos, status.getChunkType());
         return true;
      } catch (Exception e) {
         this.level.getServer().reportChunkSaveFailure(e, this.storageInfo(), pos);
         return false;
      }
   }

   private boolean isExistingChunkFull(final ChunkPos pos) {
      byte cachedChunkType = this.chunkTypeCache.get(pos.pack());
      if (cachedChunkType != 0) {
         return cachedChunkType == 1;
      }

      CompoundTag currentTag;
      try {
         currentTag = this.readChunk(pos).join().orElse(null);
         if (currentTag == null) {
            this.markPositionReplaceable(pos);
            return false;
         }
      } catch (Exception e) {
         LOGGER.error("Failed to read chunk {}", pos, e);
         this.markPositionReplaceable(pos);
         return false;
      }

      ChunkType chunkType = SerializableChunkData.getChunkStatusFromTag(currentTag).getChunkType();
      return this.markPosition(pos, chunkType) == 1;
   }

   protected void setServerViewDistance(final int newViewDistance) {
      int actualNewDistance = Mth.clamp(newViewDistance, 2, 32);
      if (actualNewDistance != this.serverViewDistance) {
         this.serverViewDistance = actualNewDistance;
         this.distanceManager.updatePlayerTickets(this.serverViewDistance);

         for (ServerPlayer player : this.playerMap.getAllPlayers()) {
            this.updateChunkTracking(player);
         }
      }
   }

   private int getPlayerViewDistance(final ServerPlayer player) {
      return Mth.clamp(player.requestedViewDistance(), 2, this.serverViewDistance);
   }

   private void markChunkPendingToSend(final ServerPlayer player, final ChunkPos pos) {
      LevelChunk chunk = this.getChunkToSend(pos.pack());
      if (chunk != null) {
         markChunkPendingToSend(player, chunk);
      }
   }

   private static void markChunkPendingToSend(final ServerPlayer player, final LevelChunk chunk) {
      player.connection.chunkSender.markChunkPendingToSend(chunk);
   }

   private static void dropChunk(final ServerPlayer player, final ChunkPos pos) {
      player.connection.chunkSender.dropChunk(player, pos);
   }

   public @Nullable LevelChunk getChunkToSend(final long key) {
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(key);
      return chunkHolder == null ? null : chunkHolder.getChunkToSend();
   }

   public int size() {
      return this.visibleChunkMap.size();
   }

   public net.minecraft.server.level.DistanceManager getDistanceManager() {
      return this.distanceManager;
   }

   void dumpChunks(final Writer output) throws IOException {
      CsvOutput csvOutput = CsvOutput.builder()
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
         .build(output);
      ObjectBidirectionalIterator var3 = this.visibleChunkMap.long2ObjectEntrySet().iterator();

      while (var3.hasNext()) {
         Entry<ChunkHolder> entry = (Entry<ChunkHolder>)var3.next();
         long posKey = entry.getLongKey();
         ChunkPos pos = ChunkPos.unpack(posKey);
         ChunkHolder holder = (ChunkHolder)entry.getValue();
         Optional<ChunkAccess> chunk = Optional.ofNullable(holder.getLatestChunk());
         Optional<LevelChunk> fullChunk = chunk.flatMap(
            chunkAccess -> chunkAccess instanceof LevelChunk levelChunk ? Optional.of(levelChunk) : Optional.empty()
         );
         csvOutput.writeRow(
            pos.x(),
            pos.z(),
            holder.getTicketLevel(),
            chunk.isPresent(),
            chunk.map(ChunkAccess::getPersistedStatus).orElse(null),
            fullChunk.map(LevelChunk::getFullStatus).orElse(null),
            printFuture(holder.getFullChunkFuture()),
            printFuture(holder.getTickingChunkFuture()),
            printFuture(holder.getEntityTickingChunkFuture()),
            this.ticketStorage.getTicketDebugString(posKey, false),
            this.anyPlayerCloseEnoughForSpawning(pos),
            fullChunk.<Integer>map(c -> c.getBlockEntities().size()).orElse(0),
            this.ticketStorage.getTicketDebugString(posKey, true),
            this.distanceManager.getChunkLevel(posKey, true),
            fullChunk.<Integer>map(levelChunk -> levelChunk.getBlockTicks().count()).orElse(0),
            fullChunk.<Integer>map(levelChunk -> levelChunk.getFluidTicks().count()).orElse(0)
         );
      }
   }

   private static String printFuture(final CompletableFuture<ChunkResult<LevelChunk>> future) {
      try {
         ChunkResult<LevelChunk> result = future.getNow(null);
         if (result != null) {
            return result.isSuccess() ? "done" : "unloaded";
         } else {
            return "not completed";
         }
      } catch (CompletionException e) {
         return "failed " + e.getCause().getMessage();
      } catch (CancellationException e) {
         return "cancelled";
      }
   }

   private CompletableFuture<Optional<CompoundTag>> readChunk(final ChunkPos pos) {
      return this.read(pos).thenApplyAsync(chunkTag -> chunkTag.map(this::upgradeChunkTag), Util.backgroundExecutor().forName("upgradeChunk"));
   }

   private CompoundTag upgradeChunkTag(final CompoundTag tag) {
      return this.upgradeChunkTag(
         tag,
         -1,
         getChunkDataFixContextTag(this.level.dimension(), this.generator().getTypeNameForDataFixer()),
         SharedConstants.getCurrentVersion().dataVersion().version()
      );
   }

   public static CompoundTag getChunkDataFixContextTag(final ResourceKey<Level> dimension, final Optional<Identifier> generatorIdentifier) {
      CompoundTag contextTag = new CompoundTag();
      contextTag.putString("dimension", dimension.identifier().toString());
      generatorIdentifier.ifPresent(identifier -> contextTag.putString("generator", identifier.toString()));
      return contextTag;
   }

   public void collectSpawningChunks(final List<LevelChunk> output) {
      LongIterator spawnCandidateChunks = this.distanceManager.getSpawnCandidateChunks();

      while (spawnCandidateChunks.hasNext()) {
         ChunkHolder holder = (ChunkHolder)this.visibleChunkMap.get(spawnCandidateChunks.nextLong());
         if (holder != null) {
            LevelChunk chunk = holder.getTickingChunk();
            if (chunk != null && this.anyPlayerCloseEnoughForSpawningInternal(holder.getPos())) {
               output.add(chunk);
            }
         }
      }
   }

   public void forEachBlockTickingChunk(final Consumer<LevelChunk> tickingChunkConsumer) {
      this.distanceManager.forEachEntityTickingChunk(chunkPos -> {
         ChunkHolder holder = (ChunkHolder)this.visibleChunkMap.get(chunkPos);
         if (holder != null) {
            LevelChunk chunk = holder.getTickingChunk();
            if (chunk != null) {
               tickingChunkConsumer.accept(chunk);
            }
         }
      });
   }

   public boolean anyPlayerCloseEnoughForSpawning(final ChunkPos pos) {
      TriState triState = this.distanceManager.hasPlayersNearby(pos.pack());
      return triState == TriState.DEFAULT ? this.anyPlayerCloseEnoughForSpawningInternal(pos) : triState.toBoolean(true);
   }

   public boolean anyPlayerCloseEnoughTo(final BlockPos pos, final int maxDistance) {
      Vec3 target = new Vec3(pos);

      for (ServerPlayer player : this.playerMap.getAllPlayers()) {
         if (this.playerIsCloseEnoughTo(player, target, maxDistance)) {
            return true;
         }
      }

      return false;
   }

   private boolean anyPlayerCloseEnoughForSpawningInternal(final ChunkPos pos) {
      for (ServerPlayer player : this.playerMap.getAllPlayers()) {
         if (this.playerIsCloseEnoughForSpawning(player, pos)) {
            return true;
         }
      }

      return false;
   }

   public List<ServerPlayer> getPlayersCloseForSpawning(final ChunkPos pos) {
      long key = pos.pack();
      if (!this.distanceManager.hasPlayersNearby(key).toBoolean(true)) {
         return List.of();
      }

      Builder<ServerPlayer> builder = ImmutableList.builder();

      for (ServerPlayer player : this.playerMap.getAllPlayers()) {
         if (this.playerIsCloseEnoughForSpawning(player, pos)) {
            builder.add(player);
         }
      }

      return builder.build();
   }

   private boolean playerIsCloseEnoughForSpawning(final ServerPlayer player, final ChunkPos pos) {
      if (player.isSpectator()) {
         return false;
      }

      double distanceToChunk = euclideanDistanceSquared(pos, player.position());
      return distanceToChunk < 16384.0;
   }

   private boolean playerIsCloseEnoughTo(final ServerPlayer player, final Vec3 pos, final int maxDistance) {
      if (player.isSpectator()) {
         return false;
      }

      double distanceToPos = player.position().distanceTo(pos);
      return distanceToPos < maxDistance;
   }

   private static double euclideanDistanceSquared(final ChunkPos chunkPos, final Vec3 pos) {
      double xPos = SectionPos.sectionToBlockCoord(chunkPos.x(), 8);
      double zPos = SectionPos.sectionToBlockCoord(chunkPos.z(), 8);
      double xd = xPos - pos.x;
      double zd = zPos - pos.z;
      return xd * xd + zd * zd;
   }

   private boolean skipPlayer(final ServerPlayer player) {
      return player.isSpectator() && !this.level.getGameRules().get(GameRules.SPECTATORS_GENERATE_CHUNKS);
   }

   private void updatePlayerStatus(final ServerPlayer player, final boolean added) {
      boolean ignored = this.skipPlayer(player);
      boolean wasIgnored = this.playerMap.ignoredOrUnknown(player);
      if (added) {
         this.playerMap.addPlayer(player, ignored);
         this.updatePlayerPos(player);
         if (!ignored) {
            this.distanceManager.addPlayer(SectionPos.of(player), player);
         }

         player.setChunkTrackingView(ChunkTrackingView.EMPTY);
         this.updateChunkTracking(player);
      } else {
         SectionPos lastPos = player.getLastSectionPos();
         this.playerMap.removePlayer(player);
         if (!wasIgnored) {
            this.distanceManager.removePlayer(lastPos, player);
         }

         this.applyChunkTrackingView(player, ChunkTrackingView.EMPTY);
      }
   }

   private void updatePlayerPos(final ServerPlayer player) {
      SectionPos pos = SectionPos.of(player);
      player.setLastSectionPos(pos);
   }

   public void move(final ServerPlayer player) {
      ObjectIterator oldSection = this.entityMap.values().iterator();

      while (oldSection.hasNext()) {
         ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)oldSection.next();
         if (trackedEntity.entity == player) {
            trackedEntity.updatePlayers(this.level.players());
         } else {
            trackedEntity.updatePlayer(player);
         }
      }

      SectionPos oldSectionx = player.getLastSectionPos();
      SectionPos newSection = SectionPos.of(player);
      boolean wasIgnored = this.playerMap.ignored(player);
      boolean ignored = this.skipPlayer(player);
      boolean positionChanged = oldSectionx.asLong() != newSection.asLong();
      if (positionChanged || wasIgnored != ignored) {
         this.updatePlayerPos(player);
         if (!wasIgnored) {
            this.distanceManager.removePlayer(oldSectionx, player);
         }

         if (!ignored) {
            this.distanceManager.addPlayer(newSection, player);
         }

         if (!wasIgnored && ignored) {
            this.playerMap.ignorePlayer(player);
         }

         if (wasIgnored && !ignored) {
            this.playerMap.unIgnorePlayer(player);
         }

         this.updateChunkTracking(player);
      }
   }

   private void updateChunkTracking(final ServerPlayer player) {
      ChunkPos chunkPos = player.chunkPosition();
      int playerViewDistance = this.getPlayerViewDistance(player);
      if (!(
         player.getChunkTrackingView() instanceof ChunkTrackingView.Positioned view
            && view.center().equals(chunkPos)
            && view.viewDistance() == playerViewDistance
      )) {
         this.applyChunkTrackingView(player, ChunkTrackingView.of(chunkPos, playerViewDistance));
      }
   }

   private void applyChunkTrackingView(final ServerPlayer player, final ChunkTrackingView next) {
      if (player.level() == this.level) {
         ChunkTrackingView previous = player.getChunkTrackingView();
         if (next instanceof ChunkTrackingView.Positioned to && !(previous instanceof ChunkTrackingView.Positioned from && from.center().equals(to.center()))) {
            player.connection.send(new ClientboundSetChunkCacheCenterPacket(to.center().x(), to.center().z()));
         }

         ChunkTrackingView.difference(previous, next, pos -> this.markChunkPendingToSend(player, pos), pos -> dropChunk(player, pos));
         player.setChunkTrackingView(next);
      }
   }

   @Override
   public List<ServerPlayer> getPlayers(final ChunkPos pos, final boolean borderOnly) {
      Set<ServerPlayer> allPlayers = this.playerMap.getAllPlayers();
      Builder<ServerPlayer> result = ImmutableList.builder();

      for (ServerPlayer player : allPlayers) {
         if (borderOnly && this.isChunkOnTrackedBorder(player, pos.x(), pos.z()) || !borderOnly && this.isChunkTracked(player, pos.x(), pos.z())) {
            result.add(player);
         }
      }

      return result.build();
   }

   public boolean hasEntityWithId(final int id) {
      return this.entityMap.containsKey(id);
   }

   protected void addEntity(final Entity entity) {
      if (!(entity instanceof EnderDragonPart)) {
         EntityType<?> type = entity.getType();
         int range = type.clientTrackingRange() * 16;
         if (range != 0) {
            int updateInterval = type.updateInterval();
            if (this.entityMap.containsKey(entity.getId())) {
               throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Entity is already tracked!"));
            }

            ChunkMap.TrackedEntity trackedEntity = new ChunkMap.TrackedEntity(entity, range, updateInterval, type.trackDeltas());
            this.entityMap.put(entity.getId(), trackedEntity);
            trackedEntity.updatePlayers(this.level.players());
            if (entity instanceof ServerPlayer player) {
               this.updatePlayerStatus(player, true);
               ObjectIterator var7 = this.entityMap.values().iterator();

               while (var7.hasNext()) {
                  ChunkMap.TrackedEntity e = (ChunkMap.TrackedEntity)var7.next();
                  if (e.entity != player) {
                     e.updatePlayer(player);
                  }
               }
            }
         }
      }
   }

   protected void removeEntity(final Entity entity) {
      if (entity instanceof ServerPlayer player) {
         this.updatePlayerStatus(player, false);
         ObjectIterator var3 = this.entityMap.values().iterator();

         while (var3.hasNext()) {
            ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)var3.next();
            trackedEntity.removePlayer(player);
         }
      }

      ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)this.entityMap.remove(entity.getId());
      if (trackedEntity != null) {
         trackedEntity.broadcastRemoved();
      }
   }

   protected void tick() {
      for (ServerPlayer player : this.playerMap.getAllPlayers()) {
         this.updateChunkTracking(player);
      }

      List<ServerPlayer> movedPlayers = Lists.newArrayList();
      List<ServerPlayer> players = this.level.players();
      ObjectIterator var3 = this.entityMap.values().iterator();

      while (var3.hasNext()) {
         ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)var3.next();
         SectionPos oldPos = trackedEntity.lastSectionPos;
         SectionPos newPos = SectionPos.of(trackedEntity.entity);
         boolean sectionPosChanged = !Objects.equals(oldPos, newPos);
         if (sectionPosChanged) {
            trackedEntity.updatePlayers(players);
            if (trackedEntity.entity instanceof ServerPlayer serverPlayer) {
               movedPlayers.add(serverPlayer);
            }

            trackedEntity.lastSectionPos = newPos;
         }

         if (sectionPosChanged || trackedEntity.entity.needsSync || this.distanceManager.inEntityTickingRange(newPos.chunk().pack())) {
            trackedEntity.serverEntity.sendChanges();
         }
      }

      if (!movedPlayers.isEmpty()) {
         var3 = this.entityMap.values().iterator();

         while (var3.hasNext()) {
            ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)var3.next();
            trackedEntity.updatePlayers(movedPlayers);
         }
      }
   }

   public void sendToTrackingPlayers(final Entity entity, final Packet<? super ClientGamePacketListener> packet) {
      ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)this.entityMap.get(entity.getId());
      if (trackedEntity != null) {
         trackedEntity.sendToTrackingPlayers(packet);
      }
   }

   public void sendToTrackingPlayersFiltered(
      final Entity entity, final Packet<? super ClientGamePacketListener> packet, final Predicate<ServerPlayer> targetPredicate
   ) {
      ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)this.entityMap.get(entity.getId());
      if (trackedEntity != null) {
         trackedEntity.sendToTrackingPlayersFiltered(packet, targetPredicate);
      }
   }

   protected void sendToTrackingPlayersAndSelf(final Entity entity, final Packet<? super ClientGamePacketListener> packet) {
      ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)this.entityMap.get(entity.getId());
      if (trackedEntity != null) {
         trackedEntity.sendToTrackingPlayersAndSelf(packet);
      }
   }

   public boolean isTrackedByAnyPlayer(final Entity entity) {
      ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)this.entityMap.get(entity.getId());
      return trackedEntity != null ? !trackedEntity.seenBy.isEmpty() : false;
   }

   public void forEachEntityTrackedBy(final ServerPlayer player, final Consumer<Entity> consumer) {
      ObjectIterator var3 = this.entityMap.values().iterator();

      while (var3.hasNext()) {
         ChunkMap.TrackedEntity entity = (ChunkMap.TrackedEntity)var3.next();
         if (entity.seenBy.contains(player.connection)) {
            consumer.accept(entity.entity);
         }
      }
   }

   public void resendBiomesForChunks(final List<ChunkAccess> chunks) {
      Map<ServerPlayer, List<LevelChunk>> chunksForPlayers = new HashMap<>();

      for (ChunkAccess chunkAccess : chunks) {
         ChunkPos pos = chunkAccess.getPos();
         LevelChunk chunk;
         if (chunkAccess instanceof LevelChunk levelChunk) {
            chunk = levelChunk;
         } else {
            chunk = this.level.getChunk(pos.x(), pos.z());
         }

         for (ServerPlayer player : this.getPlayers(pos, false)) {
            chunksForPlayers.computeIfAbsent(player, p -> new ArrayList<>()).add(chunk);
         }
      }

      chunksForPlayers.forEach((playerx, chunkList) -> playerx.connection.send(ClientboundChunksBiomesPacket.forChunks((List<LevelChunk>)chunkList)));
   }

   protected PoiManager getPoiManager() {
      return this.poiManager;
   }

   void onFullChunkStatusChange(final ChunkPos pos, final FullChunkStatus status) {
      this.chunkStatusListener.onChunkStatusChange(pos, status);
   }

   public void waitForLightBeforeSending(final ChunkPos centerChunk, final int chunkRadius) {
      int affectedLightChunkRadius = chunkRadius + 1;
      ChunkPos.rangeClosed(centerChunk, affectedLightChunkRadius).forEach(chunkPos -> {
         ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(chunkPos.pack());
         if (chunkHolder != null) {
            chunkHolder.addSendDependency(this.lightEngine.waitForPendingTasks(chunkPos.x(), chunkPos.z()));
         }
      });
   }

   public void forEachReadyToSendChunk(final Consumer<LevelChunk> consumer) {
      ObjectIterator var2 = this.visibleChunkMap.values().iterator();

      while (var2.hasNext()) {
         ChunkHolder chunkHolder = (ChunkHolder)var2.next();
         LevelChunk chunk = chunkHolder.getChunkToSend();
         if (chunk != null) {
            consumer.accept(chunk);
         }
      }
   }

   private class DistanceManager extends net.minecraft.server.level.DistanceManager {
      protected DistanceManager(final TicketStorage ticketStorage, final Executor executor, final Executor mainThreadExecutor) {
         super(ticketStorage, executor, mainThreadExecutor);
      }

      @Override
      protected boolean isChunkToRemove(final long node) {
         return ChunkMap.this.toDrop.contains(node);
      }

      @Override
      protected @Nullable ChunkHolder getChunk(final long node) {
         return ChunkMap.this.getUpdatingChunkIfPresent(node);
      }

      @Override
      protected @Nullable ChunkHolder updateChunkScheduling(final long node, final int level, final @Nullable ChunkHolder chunk, final int oldLevel) {
         return ChunkMap.this.updateChunkScheduling(node, level, chunk, oldLevel);
      }
   }

   private class TrackedEntity implements ServerEntity.Synchronizer {
      private final ServerEntity serverEntity;
      private final Entity entity;
      private final int range;
      private SectionPos lastSectionPos;
      private final Set<ServerPlayerConnection> seenBy = Sets.newIdentityHashSet();

      public TrackedEntity(final Entity entity, final int range, final int updateInterval, final boolean trackDelta) {
         this.serverEntity = new ServerEntity(ChunkMap.this.level, entity, updateInterval, trackDelta, this);
         this.entity = entity;
         this.range = range;
         this.lastSectionPos = SectionPos.of(entity);
      }

      @Override
      public boolean equals(final Object obj) {
         return obj instanceof ChunkMap.TrackedEntity trackedEntity ? trackedEntity.entity.getId() == this.entity.getId() : false;
      }

      @Override
      public int hashCode() {
         return this.entity.getId();
      }

      @Override
      public void sendToTrackingPlayers(final Packet<? super ClientGamePacketListener> packet) {
         for (ServerPlayerConnection connection : this.seenBy) {
            connection.send(packet);
         }
      }

      @Override
      public void sendToTrackingPlayersAndSelf(final Packet<? super ClientGamePacketListener> packet) {
         this.sendToTrackingPlayers(packet);
         if (this.entity instanceof ServerPlayer player) {
            player.connection.send(packet);
         }
      }

      @Override
      public void sendToTrackingPlayersFiltered(final Packet<? super ClientGamePacketListener> packet, final Predicate<ServerPlayer> targetPredicate) {
         for (ServerPlayerConnection connection : this.seenBy) {
            if (targetPredicate.test(connection.getPlayer())) {
               connection.send(packet);
            }
         }
      }

      public void broadcastRemoved() {
         for (ServerPlayerConnection connection : this.seenBy) {
            this.serverEntity.removePairing(connection.getPlayer());
         }
      }

      public void removePlayer(final ServerPlayer player) {
         if (this.seenBy.remove(player.connection)) {
            this.serverEntity.removePairing(player);
            if (this.seenBy.isEmpty()) {
               ChunkMap.this.level.debugSynchronizers().dropEntity(this.entity);
            }
         }
      }

      public void updatePlayer(final ServerPlayer player) {
         if (player != this.entity) {
            Vec3 deltaToPlayer = player.position().subtract(this.entity.position());
            int playerViewDistance = ChunkMap.this.getPlayerViewDistance(player);
            double visibleRange = Math.min(this.getEffectiveRange(), playerViewDistance * 16);
            double distanceSquared = deltaToPlayer.x * deltaToPlayer.x + deltaToPlayer.z * deltaToPlayer.z;
            double rangeSquared = visibleRange * visibleRange;
            boolean visibleToPlayer = distanceSquared <= rangeSquared
               && this.entity.broadcastToPlayer(player)
               && ChunkMap.this.isChunkTracked(player, this.entity.chunkPosition().x(), this.entity.chunkPosition().z());
            if (visibleToPlayer) {
               if (this.seenBy.add(player.connection)) {
                  this.serverEntity.addPairing(player);
                  if (this.seenBy.size() == 1) {
                     ChunkMap.this.level.debugSynchronizers().registerEntity(this.entity);
                  }

                  ChunkMap.this.level.debugSynchronizers().startTrackingEntity(player, this.entity);
               }
            } else {
               this.removePlayer(player);
            }
         }
      }

      private int scaledRange(final int range) {
         return ChunkMap.this.level.getServer().getScaledTrackingDistance(range);
      }

      private int getEffectiveRange() {
         int effectiveRange = this.range;

         for (Entity passenger : this.entity.getIndirectPassengers()) {
            int passengerRange = passenger.getType().clientTrackingRange() * 16;
            if (passengerRange > effectiveRange) {
               effectiveRange = passengerRange;
            }
         }

         return this.scaledRange(effectiveRange);
      }

      public void updatePlayers(final List<ServerPlayer> players) {
         for (ServerPlayer player : players) {
            this.updatePlayer(player);
         }
      }
   }
}
