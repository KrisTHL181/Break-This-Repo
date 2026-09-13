package net.minecraft.gametest.framework;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class GameTestRunner {
   public static final int DEFAULT_TESTS_PER_ROW = 8;
   private static final Logger LOGGER = LogUtils.getLogger();
   private final MinecraftServer server;
   private final GameTestTicker testTicker;
   private final List<GameTestInfo> allTestInfos;
   private ImmutableList<GameTestBatch> batches;
   private final List<GameTestBatchListener> batchListeners = Lists.newArrayList();
   private final List<GameTestInfo> scheduledForRerun = Lists.newArrayList();
   private final GameTestRunner.GameTestBatcher testBatcher;
   private boolean stopped = true;
   private TestEnvironmentDefinition.@Nullable Activation<?> currentEnvironment;
   private final GameTestRunner.StructureSpawner existingStructureSpawner;
   private final GameTestRunner.StructureSpawner newStructureSpawner;
   private final boolean haltOnError;
   private final boolean clearBetweenBatches;

   protected GameTestRunner(
      final GameTestRunner.GameTestBatcher batcher,
      final Collection<GameTestBatch> batches,
      final MinecraftServer server,
      final GameTestTicker testTicker,
      final GameTestRunner.StructureSpawner existingStructureSpawner,
      final GameTestRunner.StructureSpawner newStructureSpawner,
      final boolean haltOnError,
      final boolean clearBetweenBatches
   ) {
      this.server = server;
      this.testTicker = testTicker;
      this.testBatcher = batcher;
      this.existingStructureSpawner = existingStructureSpawner;
      this.newStructureSpawner = newStructureSpawner;
      this.batches = ImmutableList.copyOf(batches);
      this.haltOnError = haltOnError;
      this.clearBetweenBatches = clearBetweenBatches;
      this.allTestInfos = this.batches.stream().flatMap(batch -> batch.gameTestInfos().stream()).collect(Util.toMutableList());
      testTicker.setRunner(this);
      this.allTestInfos.forEach(info -> info.addListener(new ReportGameListener()));
   }

   public List<GameTestInfo> getTestInfos() {
      return this.allTestInfos;
   }

   public void start() {
      this.stopped = false;
      this.runBatch(0);
   }

   public void stop() {
      this.stopped = true;
      if (this.currentEnvironment != null) {
         this.endCurrentEnvironment();
      }
   }

   public void rerunTest(final GameTestInfo info) {
      GameTestInfo copy = info.copyReset();
      info.getListeners().forEach(listener -> listener.testAddedForRerun(info, copy, this));
      this.allTestInfos.add(copy);
      this.scheduledForRerun.add(copy);
      if (this.stopped) {
         this.runScheduledRerunTests();
      }
   }

   private void runBatch(final int batchIndex) {
      if (batchIndex >= this.batches.size()) {
         this.endCurrentEnvironment();
         this.runScheduledRerunTests();
      } else {
         if (batchIndex > 0 && this.clearBetweenBatches) {
            GameTestBatch lastBatch = (GameTestBatch)this.batches.get(batchIndex - 1);
            lastBatch.gameTestInfos().forEach(gameTestInfo -> {
               ServerLevel level = gameTestInfo.getLevel();
               TestInstanceBlockEntity testInstanceBlockEntity = gameTestInfo.getTestInstanceBlockEntity();
               StructureUtils.clearSpaceForStructure(testInstanceBlockEntity.getTestBoundingBox(), level);
               level.destroyBlock(testInstanceBlockEntity.getBlockPos(), false);
            });
         }

         final GameTestBatch currentBatch = (GameTestBatch)this.batches.get(batchIndex);
         this.existingStructureSpawner.onBatchStart(this.server);
         this.newStructureSpawner.onBatchStart(this.server);
         Collection<GameTestInfo> testInfosForThisBatch = this.createStructuresForBatch(currentBatch.gameTestInfos());
         LOGGER.info(
            "Running test environment '{}' batch {} ({} tests)...",
            new Object[]{currentBatch.environment().getRegisteredName(), currentBatch.index(), testInfosForThisBatch.size()}
         );
         this.endCurrentEnvironment();
         this.currentEnvironment = TestEnvironmentDefinition.activate(
            currentBatch.environment().value(), this.server.getLevel(TestFinder.Builder.levelForDimension(currentBatch.environment().value()))
         );
         this.batchListeners.forEach(listener -> listener.testBatchStarting(currentBatch));
         final MultipleTestTracker currentBatchTracker = new MultipleTestTracker();
         testInfosForThisBatch.forEach(currentBatchTracker::addTestToTrack);
         currentBatchTracker.addListener(new GameTestListener() {
            private void testCompleted(final GameTestInfo testInfo) {
               if (currentBatchTracker.isDone()) {
                  GameTestRunner.this.batchListeners.forEach(listener -> listener.testBatchFinished(currentBatch));
                  LongSet forcedChunks = new LongArraySet(testInfo.getLevel().getForceLoadedChunks());
                  forcedChunks.forEach(pos -> testInfo.getLevel().setChunkForced(ChunkPos.getX(pos), ChunkPos.getZ(pos), false));
                  GameTestRunner.this.runBatch(batchIndex + 1);
               }
            }

            @Override
            public void testStructureLoaded(final GameTestInfo testInfo) {
            }

            @Override
            public void testPassed(final GameTestInfo testInfo, final GameTestRunner runner) {
               testInfo.getTestInstanceBlockEntity().removeBarriers();
               this.testCompleted(testInfo);
            }

            @Override
            public void testFailed(final GameTestInfo testInfo, final GameTestRunner runner) {
               if (GameTestRunner.this.haltOnError) {
                  GameTestRunner.this.endCurrentEnvironment();
                  LongSet forcedChunks = new LongArraySet(testInfo.getLevel().getForceLoadedChunks());
                  forcedChunks.forEach(pos -> testInfo.getLevel().setChunkForced(ChunkPos.getX(pos), ChunkPos.getZ(pos), false));
                  GameTestTicker.SINGLETON.clear();
               } else {
                  this.testCompleted(testInfo);
               }
            }

            @Override
            public void testAddedForRerun(final GameTestInfo original, final GameTestInfo copy, final GameTestRunner runner) {
            }
         });
         testInfosForThisBatch.forEach(this.testTicker::add);
      }
   }

   private void endCurrentEnvironment() {
      if (this.currentEnvironment != null) {
         this.currentEnvironment.teardown();
         this.currentEnvironment = null;
      }
   }

   private void runScheduledRerunTests() {
      if (!this.scheduledForRerun.isEmpty()) {
         LOGGER.info("Starting re-run of tests: {}", this.scheduledForRerun.stream().map(info -> info.id().toString()).collect(Collectors.joining(", ")));
         this.batches = ImmutableList.copyOf(this.testBatcher.batch(this.scheduledForRerun));
         this.scheduledForRerun.clear();
         this.stopped = false;
         this.runBatch(0);
      } else {
         this.batches = ImmutableList.of();
         this.stopped = true;
      }
   }

   public void addListener(final GameTestBatchListener listener) {
      this.batchListeners.add(listener);
   }

   private Collection<GameTestInfo> createStructuresForBatch(final Collection<GameTestInfo> batch) {
      return batch.stream().map(this::spawn).flatMap(Optional::stream).toList();
   }

   private Optional<GameTestInfo> spawn(final GameTestInfo testInfo) {
      return testInfo.getTestBlockPos() == null ? this.newStructureSpawner.spawnStructure(testInfo) : this.existingStructureSpawner.spawnStructure(testInfo);
   }

   public static class Builder {
      private final MinecraftServer server;
      private final GameTestTicker testTicker = GameTestTicker.SINGLETON;
      private GameTestRunner.GameTestBatcher batcher = GameTestBatchFactory.fromGameTestInfo();
      private GameTestRunner.StructureSpawner existingStructureSpawner = GameTestRunner.StructureSpawner.IN_PLACE;
      private GameTestRunner.StructureSpawner newStructureSpawner = GameTestRunner.StructureSpawner.NOT_SET;
      private final Collection<GameTestBatch> batches;
      private boolean haltOnError = false;
      private boolean clearBetweenBatches = false;

      private Builder(final Collection<GameTestBatch> batches, final MinecraftServer server) {
         this.batches = batches;
         this.server = server;
      }

      public static GameTestRunner.Builder fromBatches(final Collection<GameTestBatch> batches, final MinecraftServer server) {
         return new GameTestRunner.Builder(batches, server);
      }

      public static GameTestRunner.Builder fromInfo(final Collection<GameTestInfo> tests, final MinecraftServer server) {
         return fromBatches(GameTestBatchFactory.fromGameTestInfo().batch(tests), server);
      }

      public GameTestRunner.Builder haltOnError() {
         this.haltOnError = true;
         return this;
      }

      public GameTestRunner.Builder clearBetweenBatches() {
         this.clearBetweenBatches = true;
         return this;
      }

      public GameTestRunner.Builder newStructureSpawner(final GameTestRunner.StructureSpawner structureSpawner) {
         this.newStructureSpawner = structureSpawner;
         return this;
      }

      public GameTestRunner.Builder existingStructureSpawner(final StructureGridSpawner spawner) {
         this.existingStructureSpawner = spawner;
         return this;
      }

      public GameTestRunner.Builder batcher(final GameTestRunner.GameTestBatcher batcher) {
         this.batcher = batcher;
         return this;
      }

      public GameTestRunner build() {
         return new GameTestRunner(
            this.batcher,
            this.batches,
            this.server,
            this.testTicker,
            this.existingStructureSpawner,
            this.newStructureSpawner,
            this.haltOnError,
            this.clearBetweenBatches
         );
      }
   }

   public interface GameTestBatcher {
      Collection<GameTestBatch> batch(Collection<GameTestInfo> infos);
   }

   public interface StructureSpawner {
      GameTestRunner.StructureSpawner IN_PLACE = testInfo -> Optional.ofNullable(testInfo.prepareTestStructure()).map(e -> e.startExecution(1));
      GameTestRunner.StructureSpawner NOT_SET = testInfo -> Optional.empty();

      Optional<GameTestInfo> spawnStructure(GameTestInfo testInfo);

      default void onBatchStart(final MinecraftServer server) {
      }
   }
}
