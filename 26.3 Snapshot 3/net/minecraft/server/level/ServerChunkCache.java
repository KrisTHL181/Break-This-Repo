package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerChunkCache extends ChunkSource {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final DistanceManager distanceManager;
   private final ServerLevel level;
   private final Thread mainThread;
   private final ThreadedLevelLightEngine lightEngine;
   private final ServerChunkCache.MainThreadExecutor mainThreadProcessor;
   public final ChunkMap chunkMap;
   private final SavedDataStorage savedDataStorage;
   private final TicketStorage ticketStorage;
   private long lastInhabitedUpdate;
   private boolean spawnEnemies = true;
   private static final int CACHE_SIZE = 4;
   private final long[] lastChunkPos = new long[4];
   private final @Nullable ChunkStatus[] lastChunkStatus = new ChunkStatus[4];
   private final @Nullable ChunkAccess[] lastChunk = new ChunkAccess[4];
   private final List<LevelChunk> spawningChunks = new ObjectArrayList();
   private final Set<ChunkHolder> chunkHoldersToBroadcast = new ReferenceOpenHashSet();
   @VisibleForDebug
   private NaturalSpawner.@Nullable SpawnState lastSpawnState;

   public ServerChunkCache(
      final ServerLevel level,
      final LevelStorageSource.LevelStorageAccess levelStorage,
      final DataFixer fixerUpper,
      final StructureTemplateManager structureTemplateManager,
      final Executor executor,
      final ChunkGenerator generator,
      final int viewDistance,
      final int simulationDistance,
      final boolean syncWrites,
      final ChunkStatusUpdateListener chunkStatusListener
   ) {
      this.level = level;
      this.mainThreadProcessor = new ServerChunkCache.MainThreadExecutor(level);
      this.mainThread = Thread.currentThread();
      Path dataFolder = levelStorage.getDimensionPath(level.dimension()).resolve("data");

      try {
         FileUtil.createDirectoriesSafe(dataFolder);
      } catch (IOException e) {
         LOGGER.error("Failed to create dimension data storage directory", e);
      }

      this.savedDataStorage = new SavedDataStorage(dataFolder, fixerUpper, level.registryAccess());
      this.ticketStorage = this.savedDataStorage.computeIfAbsent(TicketStorage.TYPE);
      this.chunkMap = new ChunkMap(
         level,
         levelStorage,
         fixerUpper,
         structureTemplateManager,
         executor,
         this.mainThreadProcessor,
         this,
         generator,
         chunkStatusListener,
         this.ticketStorage,
         viewDistance,
         syncWrites
      );
      this.lightEngine = this.chunkMap.getLightEngine();
      this.distanceManager = this.chunkMap.getDistanceManager();
      this.distanceManager.updateSimulationDistance(simulationDistance);
      this.clearCache();
   }

   public ThreadedLevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   private @Nullable ChunkHolder getVisibleChunkIfPresent(final long key) {
      return this.chunkMap.getVisibleChunkIfPresent(key);
   }

   private void storeInCache(final long pos, final @Nullable ChunkAccess chunk, final ChunkStatus status) {
      for (int i = 3; i > 0; i--) {
         this.lastChunkPos[i] = this.lastChunkPos[i - 1];
         this.lastChunkStatus[i] = this.lastChunkStatus[i - 1];
         this.lastChunk[i] = this.lastChunk[i - 1];
      }

      this.lastChunkPos[0] = pos;
      this.lastChunkStatus[0] = status;
      this.lastChunk[0] = chunk;
   }

   @Override
   public @Nullable ChunkAccess getChunk(final int x, final int z, final ChunkStatus targetStatus, final boolean loadOrGenerate) {
      if (Thread.currentThread() != this.mainThread) {
         return CompletableFuture.<ChunkAccess>supplyAsync(() -> this.getChunk(x, z, targetStatus, loadOrGenerate), this.mainThreadProcessor).join();
      }

      ProfilerFiller profiler = Profiler.get();
      profiler.incrementCounter("getChunk");
      long pos = ChunkPos.pack(x, z);

      for (int i = 0; i < 4; i++) {
         if (pos == this.lastChunkPos[i] && targetStatus == this.lastChunkStatus[i]) {
            ChunkAccess chunkAccess = this.lastChunk[i];
            if (chunkAccess != null || !loadOrGenerate) {
               return chunkAccess;
            }
         }
      }

      profiler.incrementCounter("getChunkCacheMiss");
      CompletableFuture<ChunkResult<ChunkAccess>> serverFuture = this.getChunkFutureMainThread(x, z, targetStatus, loadOrGenerate);
      this.mainThreadProcessor.managedBlock(serverFuture::isDone);
      ChunkResult<ChunkAccess> chunkResult = serverFuture.join();
      ChunkAccess chunk = chunkResult.orElse(null);
      if (chunk == null && loadOrGenerate) {
         throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Chunk not there when requested: " + chunkResult.getError()));
      }

      this.storeInCache(pos, chunk, targetStatus);
      return chunk;
   }

   @Override
   public @Nullable LevelChunk getChunkNow(final int x, final int z) {
      if (Thread.currentThread() != this.mainThread) {
         return null;
      }

      Profiler.get().incrementCounter("getChunkNow");
      long pos = ChunkPos.pack(x, z);

      for (int i = 0; i < 4; i++) {
         if (pos == this.lastChunkPos[i] && this.lastChunkStatus[i] == ChunkStatus.FULL) {
            return this.lastChunk[i] instanceof LevelChunk levelChunk ? levelChunk : null;
         }
      }

      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(pos);
      if (chunkHolder == null) {
         return null;
      }

      ChunkAccess chunk = chunkHolder.getChunkIfPresent(ChunkStatus.FULL);
      if (chunk != null) {
         this.storeInCache(pos, chunk, ChunkStatus.FULL);
         if (chunk instanceof LevelChunk levelChunk) {
            return levelChunk;
         }
      }

      return null;
   }

   private void clearCache() {
      Arrays.fill(this.lastChunkPos, ChunkPos.INVALID_CHUNK_POS);
      Arrays.fill(this.lastChunkStatus, null);
      Arrays.fill(this.lastChunk, null);
   }

   public CompletableFuture<ChunkResult<ChunkAccess>> getChunkFuture(final int x, final int z, final ChunkStatus targetStatus, final boolean loadOrGenerate) {
      boolean isMainThread = Thread.currentThread() == this.mainThread;
      CompletableFuture<ChunkResult<ChunkAccess>> serverFuture;
      if (isMainThread) {
         serverFuture = this.getChunkFutureMainThread(x, z, targetStatus, loadOrGenerate);
         this.mainThreadProcessor.managedBlock(serverFuture::isDone);
      } else {
         serverFuture = CompletableFuture.<CompletableFuture<ChunkResult<ChunkAccess>>>supplyAsync(
               () -> this.getChunkFutureMainThread(x, z, targetStatus, loadOrGenerate), this.mainThreadProcessor
            )
            .thenCompose(chunk -> (CompletionStage<ChunkResult<ChunkAccess>>)chunk);
      }

      return serverFuture;
   }

   private CompletableFuture<ChunkResult<ChunkAccess>> getChunkFutureMainThread(
      final int x, final int z, final ChunkStatus targetStatus, final boolean loadOrGenerate
   ) {
      ChunkPos pos = new ChunkPos(x, z);
      long key = pos.pack();
      int targetTicketLevel = ChunkLevel.byStatus(targetStatus);
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(key);
      if (loadOrGenerate) {
         this.addTicket(new Ticket(TicketType.UNKNOWN, targetTicketLevel), pos);
         if (this.chunkAbsent(chunkHolder, targetTicketLevel)) {
            ProfilerFiller profiler = Profiler.get();
            profiler.push("chunkLoad");
            this.runDistanceManagerUpdates();
            chunkHolder = this.getVisibleChunkIfPresent(key);
            profiler.pop();
            if (this.chunkAbsent(chunkHolder, targetTicketLevel)) {
               throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("No chunk holder after ticket has been added"));
            }
         }
      }

      return this.chunkAbsent(chunkHolder, targetTicketLevel)
         ? GenerationChunkHolder.UNLOADED_CHUNK_FUTURE
         : chunkHolder.scheduleChunkGenerationTask(targetStatus, this.chunkMap);
   }

   private boolean chunkAbsent(final @Nullable ChunkHolder chunkHolder, final int targetTicketLevel) {
      return chunkHolder == null || chunkHolder.getTicketLevel() > targetTicketLevel;
   }

   @Override
   public boolean hasChunk(final int x, final int z) {
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(new ChunkPos(x, z).pack());
      int targetTicketLevel = ChunkLevel.byStatus(ChunkStatus.FULL);
      return !this.chunkAbsent(chunkHolder, targetTicketLevel);
   }

   @Override
   public @Nullable LightChunk getChunkForLighting(final int x, final int z) {
      long key = ChunkPos.pack(x, z);
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(key);
      return chunkHolder == null ? null : chunkHolder.getChunkIfPresentUnchecked(ChunkStatus.INITIALIZE_LIGHT.getParent());
   }

   public Level getLevel() {
      return this.level;
   }

   public boolean pollTask() {
      return this.mainThreadProcessor.pollTask();
   }

   boolean runDistanceManagerUpdates() {
      boolean updated = this.distanceManager.runAllUpdates(this.chunkMap);
      boolean promoted = this.chunkMap.promoteChunkMap();
      this.chunkMap.runGenerationTasks();
      if (!updated && !promoted) {
         return false;
      }

      this.clearCache();
      return true;
   }

   public boolean isPositionTicking(final long chunkKey) {
      if (!this.level.shouldTickBlocksAt(chunkKey)) {
         return false;
      }

      ChunkHolder holder = this.getVisibleChunkIfPresent(chunkKey);
      return holder == null ? false : holder.getTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).isSuccess();
   }

   public void save(final boolean flushStorage) {
      this.runDistanceManagerUpdates();
      this.chunkMap.saveAllChunks(flushStorage);
   }

   @Override
   public void close() throws IOException {
      this.save(true);
      this.savedDataStorage.close();
      this.lightEngine.close();
      this.chunkMap.close();
   }

   @Override
   public void tick(final BooleanSupplier haveTime, final boolean tickChunks) {
      ProfilerFiller profiler = Profiler.get();
      profiler.push("purge");
      if (this.level.tickRateManager().runsNormally() || !tickChunks) {
         this.ticketStorage.purgeStaleTickets(this.chunkMap);
      }

      this.runDistanceManagerUpdates();
      profiler.popPush("chunks");
      if (tickChunks) {
         this.tickChunks();
         this.chunkMap.tick();
      }

      profiler.popPush("unload");
      this.chunkMap.tick(haveTime);
      profiler.pop();
      this.clearCache();
   }

   private void tickChunks() {
      long time = this.level.getGameTime();
      long timeDiff = time - this.lastInhabitedUpdate;
      this.lastInhabitedUpdate = time;
      if (!this.level.isDebug()) {
         ProfilerFiller profiler = Profiler.get();
         profiler.push("pollingChunks");
         if (this.level.tickRateManager().runsNormally()) {
            profiler.push("tickingChunks");
            this.tickChunks(profiler, timeDiff);
            profiler.pop();
         }

         this.broadcastChangedChunks(profiler);
         profiler.pop();
      }
   }

   private void broadcastChangedChunks(final ProfilerFiller profiler) {
      profiler.push("broadcast");

      for (ChunkHolder chunkHolder : this.chunkHoldersToBroadcast) {
         LevelChunk chunk = chunkHolder.getTickingChunk();
         if (chunk != null) {
            chunkHolder.broadcastChanges(chunk);
         }
      }

      this.chunkHoldersToBroadcast.clear();
      profiler.pop();
   }

   private void tickChunks(final ProfilerFiller profiler, final long timeDiff) {
      profiler.push("naturalSpawnCount");
      int chunkCount = this.distanceManager.getNaturalSpawnChunkCount();
      NaturalSpawner.SpawnState spawnCookie = NaturalSpawner.createState(
         chunkCount, this.level.getAllEntities(), this::getFullChunk, new LocalMobCapCalculator(this.chunkMap)
      );
      this.lastSpawnState = spawnCookie;
      boolean doMobSpawning = this.level.getGameRules().get(GameRules.SPAWN_MOBS);
      int tickSpeed = this.level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
      List<MobCategory> spawningCategories;
      if (doMobSpawning) {
         boolean spawnPersistent = this.level.getGameTime() % 400L == 0L;
         spawningCategories = NaturalSpawner.getFilteredSpawningCategories(spawnCookie, this.spawnEnemies, spawnPersistent);
      } else {
         spawningCategories = List.of();
      }

      List<LevelChunk> spawningChunks = this.spawningChunks;

      try {
         profiler.popPush("filteringSpawningChunks");
         this.chunkMap.collectSpawningChunks(spawningChunks);
         profiler.popPush("shuffleSpawningChunks");
         Util.shuffle(spawningChunks, this.level.getRandom());
         profiler.popPush("tickSpawningChunks");

         for (LevelChunk chunk : spawningChunks) {
            this.tickSpawningChunk(chunk, timeDiff, spawningCategories, spawnCookie);
         }
      } finally {
         spawningChunks.clear();
      }

      profiler.popPush("tickTickingChunks");
      this.chunkMap.forEachBlockTickingChunk(chunkx -> this.level.tickChunk(chunkx, tickSpeed));
      if (doMobSpawning) {
         profiler.popPush("customSpawners");
         this.level.tickCustomSpawners(this.spawnEnemies);
      }

      profiler.pop();
   }

   private void tickSpawningChunk(
      final LevelChunk chunk, final long timeDiff, final List<MobCategory> spawningCategories, final NaturalSpawner.SpawnState spawnCookie
   ) {
      ChunkPos chunkPos = chunk.getPos();
      chunk.incrementInhabitedTime(timeDiff);
      if (this.distanceManager.inEntityTickingRange(chunkPos.pack())) {
         this.level.tickThunder(chunk);
      }

      if (!spawningCategories.isEmpty()) {
         if (this.level.canSpawnEntitiesInChunk(chunkPos)) {
            NaturalSpawner.spawnForChunk(this.level, chunk, spawnCookie, spawningCategories);
         }
      }
   }

   private void getFullChunk(final long chunkKey, final Consumer<LevelChunk> output) {
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(chunkKey);
      if (chunkHolder != null) {
         chunkHolder.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).ifSuccess(output);
      }
   }

   @Override
   public String gatherStats() {
      return Integer.toString(this.getLoadedChunksCount());
   }

   @VisibleForTesting
   public int getPendingTasksCount() {
      return this.mainThreadProcessor.getPendingTasksCount();
   }

   public ChunkGenerator getGenerator() {
      return this.chunkMap.generator();
   }

   public ChunkGeneratorStructureState getGeneratorState() {
      return this.chunkMap.generatorState();
   }

   public RandomState randomState() {
      return this.chunkMap.randomState();
   }

   @Override
   public int getLoadedChunksCount() {
      return this.chunkMap.size();
   }

   public void blockChanged(final BlockPos pos) {
      int xc = SectionPos.blockToSectionCoord(pos.getX());
      int zc = SectionPos.blockToSectionCoord(pos.getZ());
      ChunkHolder chunk = this.getVisibleChunkIfPresent(ChunkPos.pack(xc, zc));
      if (chunk != null && chunk.blockChanged(pos)) {
         this.chunkHoldersToBroadcast.add(chunk);
      }
   }

   @Override
   public void onLightUpdate(final LightLayer layer, final SectionPos pos) {
      this.mainThreadProcessor.execute(() -> {
         ChunkHolder chunk = this.getVisibleChunkIfPresent(pos.chunk().pack());
         if (chunk != null && chunk.sectionLightChanged(layer, pos.y())) {
            this.chunkHoldersToBroadcast.add(chunk);
         }
      });
   }

   public boolean hasActiveTickets() {
      return this.ticketStorage.shouldKeepDimensionActive();
   }

   public void addTicket(final Ticket ticket, final ChunkPos pos) {
      this.ticketStorage.addTicket(ticket, pos);
   }

   public CompletableFuture<?> addTicketAndLoadWithRadius(final TicketType type, final ChunkPos pos, final int radius) {
      if (!type.doesLoad()) {
         throw new IllegalStateException("Ticket type " + type + " does not trigger chunk loading");
      }

      if (type.canExpireIfUnloaded()) {
         throw new IllegalStateException("Ticket type " + type + " can expire before it loads, cannot fetch asynchronously");
      }

      this.addTicketWithRadius(type, pos, radius);
      this.runDistanceManagerUpdates();
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(pos.pack());
      Objects.requireNonNull(chunkHolder, "No chunk was scheduled for loading");
      return this.chunkMap.getChunkRangeFuture(chunkHolder, radius, distance -> ChunkStatus.FULL);
   }

   public void addTicketWithRadius(final TicketType type, final ChunkPos pos, final int radius) {
      this.ticketStorage.addTicketWithRadius(type, pos, radius);
   }

   public void removeTicketWithRadius(final TicketType type, final ChunkPos pos, final int radius) {
      this.ticketStorage.removeTicketWithRadius(type, pos, radius);
   }

   @Override
   public boolean updateChunkForced(final ChunkPos pos, final boolean forced) {
      return this.ticketStorage.updateChunkForced(pos, forced);
   }

   @Override
   public LongSet getForceLoadedChunks() {
      return this.ticketStorage.getForceLoadedChunks();
   }

   public void move(final ServerPlayer player) {
      if (!player.isRemoved()) {
         this.chunkMap.move(player);
         if (player.isReceivingWaypoints()) {
            this.level.getWaypointManager().updatePlayer(player);
         }
      }
   }

   public boolean hasEntityWithId(final int id) {
      return this.chunkMap.hasEntityWithId(id);
   }

   public void removeEntity(final Entity entity) {
      this.chunkMap.removeEntity(entity);
   }

   public void addEntity(final Entity entity) {
      this.chunkMap.addEntity(entity);
   }

   public void sendToTrackingPlayersAndSelf(final Entity entity, final Packet<? super ClientGamePacketListener> packet) {
      this.chunkMap.sendToTrackingPlayersAndSelf(entity, packet);
   }

   public void sendToTrackingPlayers(final Entity entity, final Packet<? super ClientGamePacketListener> packet) {
      this.chunkMap.sendToTrackingPlayers(entity, packet);
   }

   public void setViewDistance(final int newDistance) {
      this.chunkMap.setServerViewDistance(newDistance);
   }

   public void setSimulationDistance(final int simulationDistance) {
      this.distanceManager.updateSimulationDistance(simulationDistance);
   }

   @Override
   public void setSpawnSettings(final boolean spawnEnemies) {
      this.spawnEnemies = spawnEnemies;
   }

   public String getChunkDebugData(final ChunkPos pos) {
      return this.chunkMap.getChunkDebugData(pos);
   }

   public SavedDataStorage getDataStorage() {
      return this.savedDataStorage;
   }

   public PoiManager getPoiManager() {
      return this.chunkMap.getPoiManager();
   }

   public ChunkScanAccess chunkScanner() {
      return this.chunkMap.chunkScanner();
   }

   @VisibleForDebug
   public NaturalSpawner.@Nullable SpawnState getLastSpawnState() {
      return this.lastSpawnState;
   }

   public void deactivateTicketsOnClosing() {
      this.ticketStorage.deactivateTicketsOnClosing();
   }

   public void onChunkReadyToSend(final ChunkHolder chunk) {
      if (chunk.hasChangesToBroadcast()) {
         this.chunkHoldersToBroadcast.add(chunk);
      }
   }

   private final class MainThreadExecutor extends BlockableEventLoop<Runnable> {
      private MainThreadExecutor(final Level level) {
         super("Chunk source main thread executor for " + level.dimension().identifier(), false);
      }

      @Override
      public Runnable wrapRunnable(final Runnable runnable) {
         return runnable;
      }

      @Override
      protected boolean shouldRun(final Runnable task) {
         return true;
      }

      @Override
      protected boolean scheduleExecutables() {
         return true;
      }

      @Override
      protected Thread getRunningThread() {
         return ServerChunkCache.this.mainThread;
      }

      @Override
      protected void doRunTask(final Runnable task) {
         Profiler.get().incrementCounter("runTask");
         super.doRunTask(task);
      }

      @Override
      protected boolean pollTask() {
         if (ServerChunkCache.this.runDistanceManagerUpdates()) {
            return true;
         }

         ServerChunkCache.this.lightEngine.tryScheduleUpdate();
         return super.pollTask();
      }
   }
}
