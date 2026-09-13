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
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class GameTestRunner {
   public static final int DEFAULT_TESTS_PER_ROW = 8;
   private static final Logger LOGGER = LogUtils.getLogger();
   final ServerLevel level;
   private final GameTestTicker testTicker;
   private final List<GameTestInfo> allTestInfos;
   private ImmutableList<GameTestBatch> batches;
   final List<GameTestBatchListener> batchListeners = Lists.newArrayList();
   private final List<GameTestInfo> scheduledForRerun = Lists.newArrayList();
   private final GameTestRunner.GameTestBatcher testBatcher;
   private boolean stopped = true;
   private @Nullable Holder<TestEnvironmentDefinition> currentEnvironment;
   private final GameTestRunner.StructureSpawner existingStructureSpawner;
   private final GameTestRunner.StructureSpawner newStructureSpawner;
   final boolean haltOnError;
   private final boolean clearBetweenBatches;

   protected GameTestRunner(
      GameTestRunner.GameTestBatcher p_332546_,
      Collection<GameTestBatch> p_332555_,
      ServerLevel p_331060_,
      GameTestTicker p_335326_,
      GameTestRunner.StructureSpawner p_336219_,
      GameTestRunner.StructureSpawner p_330306_,
      boolean p_342567_,
      boolean p_428488_
   ) {
      this.level = p_331060_;
      this.testTicker = p_335326_;
      this.testBatcher = p_332546_;
      this.existingStructureSpawner = p_336219_;
      this.newStructureSpawner = p_330306_;
      this.batches = ImmutableList.copyOf(p_332555_);
      this.haltOnError = p_342567_;
      this.clearBetweenBatches = p_428488_;
      this.allTestInfos = this.batches.stream().flatMap(p_325950_ -> p_325950_.gameTestInfos().stream()).collect(Util.toMutableList());
      p_335326_.setRunner(this);
      this.allTestInfos.forEach(p_325945_ -> p_325945_.addListener(new ReportGameListener()));
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

   public void rerunTest(GameTestInfo p_335947_) {
      GameTestInfo gametestinfo = p_335947_.copyReset();
      p_335947_.getListeners().forEach(p_325948_ -> p_325948_.testAddedForRerun(p_335947_, gametestinfo, this));
      this.allTestInfos.add(gametestinfo);
      this.scheduledForRerun.add(gametestinfo);
      if (this.stopped) {
         this.runScheduledRerunTests();
      }
   }

   void runBatch(final int p_336310_) {
      if (p_336310_ >= this.batches.size()) {
         this.endCurrentEnvironment();
         this.runScheduledRerunTests();
      } else {
         if (p_336310_ > 0 && this.clearBetweenBatches) {
            GameTestBatch gametestbatch = (GameTestBatch)this.batches.get(p_336310_ - 1);
            gametestbatch.gameTestInfos().forEach(p_421246_ -> {
               TestInstanceBlockEntity testinstanceblockentity = p_421246_.getTestInstanceBlockEntity();
               StructureUtils.clearSpaceForStructure(testinstanceblockentity.getStructureBoundingBox(), this.level);
               this.level.destroyBlock(testinstanceblockentity.getBlockPos(), false);
            });
         }

         final GameTestBatch gametestbatch1 = (GameTestBatch)this.batches.get(p_336310_);
         this.existingStructureSpawner.onBatchStart(this.level);
         this.newStructureSpawner.onBatchStart(this.level);
         Collection<GameTestInfo> collection = this.createStructuresForBatch(gametestbatch1.gameTestInfos());
         LOGGER.info(
            "Running test environment '{}' batch {} ({} tests)...",
            new Object[]{gametestbatch1.environment().getRegisteredName(), gametestbatch1.index(), collection.size()}
         );
         this.endCurrentEnvironment();
         this.currentEnvironment = gametestbatch1.environment();
         this.currentEnvironment.value().setup(this.level);
         this.batchListeners.forEach(p_389766_ -> p_389766_.testBatchStarting(gametestbatch1));
         final MultipleTestTracker multipletesttracker = new MultipleTestTracker();
         collection.forEach(multipletesttracker::addTestToTrack);
         multipletesttracker.addListener(new GameTestListener() {
            private void testCompleted(GameTestInfo p_422746_) {
               p_422746_.getTestInstanceBlockEntity().removeBarriers();
               if (multipletesttracker.isDone()) {
                  GameTestRunner.this.batchListeners.forEach(p_389769_ -> p_389769_.testBatchFinished(gametestbatch1));
                  LongSet longset = new LongArraySet(GameTestRunner.this.level.getForceLoadedChunks());
                  longset.forEach(p_328493_ -> GameTestRunner.this.level.setChunkForced(ChunkPos.getX(p_328493_), ChunkPos.getZ(p_328493_), false));
                  GameTestRunner.this.runBatch(p_336310_ + 1);
               }
            }

            @Override
            public void testStructureLoaded(GameTestInfo p_336002_) {
            }

            @Override
            public void testPassed(GameTestInfo p_334410_, GameTestRunner p_329201_) {
               this.testCompleted(p_334410_);
            }

            @Override
            public void testFailed(GameTestInfo p_335430_, GameTestRunner p_330830_) {
               if (GameTestRunner.this.haltOnError) {
                  GameTestRunner.this.endCurrentEnvironment();
                  LongSet longset = new LongArraySet(GameTestRunner.this.level.getForceLoadedChunks());
                  longset.forEach(p_341095_ -> GameTestRunner.this.level.setChunkForced(ChunkPos.getX(p_341095_), ChunkPos.getZ(p_341095_), false));
                  GameTestTicker.SINGLETON.clear();
                  p_335430_.getTestInstanceBlockEntity().removeBarriers();
               } else {
                  this.testCompleted(p_335430_);
               }
            }

            @Override
            public void testAddedForRerun(GameTestInfo p_329460_, GameTestInfo p_328079_, GameTestRunner p_334962_) {
            }
         });
         collection.forEach(this.testTicker::add);
      }
   }

   void endCurrentEnvironment() {
      if (this.currentEnvironment != null) {
         this.currentEnvironment.value().teardown(this.level);
         this.currentEnvironment = null;
      }
   }

   private void runScheduledRerunTests() {
      if (!this.scheduledForRerun.isEmpty()) {
         LOGGER.info(
            "Starting re-run of tests: {}", this.scheduledForRerun.stream().map(p_448753_ -> p_448753_.id().toString()).collect(Collectors.joining(", "))
         );
         this.batches = ImmutableList.copyOf(this.testBatcher.batch(this.scheduledForRerun));
         this.scheduledForRerun.clear();
         this.stopped = false;
         this.runBatch(0);
      } else {
         this.batches = ImmutableList.of();
         this.stopped = true;
      }
   }

   public void addListener(GameTestBatchListener p_329355_) {
      this.batchListeners.add(p_329355_);
   }

   private Collection<GameTestInfo> createStructuresForBatch(Collection<GameTestInfo> p_335557_) {
      return p_335557_.stream().map(this::spawn).flatMap(Optional::stream).toList();
   }

   private Optional<GameTestInfo> spawn(GameTestInfo p_330408_) {
      return p_330408_.getTestBlockPos() == null ? this.newStructureSpawner.spawnStructure(p_330408_) : this.existingStructureSpawner.spawnStructure(p_330408_);
   }

   public static class Builder {
      private final ServerLevel level;
      private final GameTestTicker testTicker = GameTestTicker.SINGLETON;
      private GameTestRunner.GameTestBatcher batcher = GameTestBatchFactory.fromGameTestInfo();
      private GameTestRunner.StructureSpawner existingStructureSpawner = GameTestRunner.StructureSpawner.IN_PLACE;
      private GameTestRunner.StructureSpawner newStructureSpawner = GameTestRunner.StructureSpawner.NOT_SET;
      private final Collection<GameTestBatch> batches;
      private boolean haltOnError = false;
      private boolean clearBetweenBatches = false;

      private Builder(Collection<GameTestBatch> p_329875_, ServerLevel p_329864_) {
         this.batches = p_329875_;
         this.level = p_329864_;
      }

      public static GameTestRunner.Builder fromBatches(Collection<GameTestBatch> p_329486_, ServerLevel p_336096_) {
         return new GameTestRunner.Builder(p_329486_, p_336096_);
      }

      public static GameTestRunner.Builder fromInfo(Collection<GameTestInfo> p_335004_, ServerLevel p_328084_) {
         return fromBatches(GameTestBatchFactory.fromGameTestInfo().batch(p_335004_), p_328084_);
      }

      public GameTestRunner.Builder haltOnError() {
         this.haltOnError = true;
         return this;
      }

      public GameTestRunner.Builder clearBetweenBatches() {
         this.clearBetweenBatches = true;
         return this;
      }

      public GameTestRunner.Builder newStructureSpawner(GameTestRunner.StructureSpawner p_329789_) {
         this.newStructureSpawner = p_329789_;
         return this;
      }

      public GameTestRunner.Builder existingStructureSpawner(StructureGridSpawner p_342571_) {
         this.existingStructureSpawner = p_342571_;
         return this;
      }

      public GameTestRunner.Builder batcher(GameTestRunner.GameTestBatcher p_344402_) {
         this.batcher = p_344402_;
         return this;
      }

      public GameTestRunner build() {
         return new GameTestRunner(
            this.batcher,
            this.batches,
            this.level,
            this.testTicker,
            this.existingStructureSpawner,
            this.newStructureSpawner,
            this.haltOnError,
            this.clearBetweenBatches
         );
      }
   }

   public interface GameTestBatcher {
      Collection<GameTestBatch> batch(Collection<GameTestInfo> var1);
   }

   public interface StructureSpawner {
      GameTestRunner.StructureSpawner IN_PLACE = p_405072_ -> Optional.ofNullable(p_405072_.prepareTestStructure())
         .map(p_405073_ -> p_405073_.startExecution(1));
      GameTestRunner.StructureSpawner NOT_SET = p_329287_ -> Optional.empty();

      Optional<GameTestInfo> spawnStructure(GameTestInfo var1);

      default void onBatchStart(ServerLevel p_343975_) {
      }
   }
}
