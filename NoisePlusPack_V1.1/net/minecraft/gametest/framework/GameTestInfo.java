package net.minecraft.gametest.framework;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class GameTestInfo {
   private final Holder.Reference<GameTestInstance> test;
   private @Nullable BlockPos testBlockPos;
   private final ServerLevel level;
   private final Collection<GameTestListener> listeners = Lists.newArrayList();
   private final int timeoutTicks;
   private final Collection<GameTestSequence> sequences = Lists.newCopyOnWriteArrayList();
   private final Object2LongMap<Runnable> runAtTickTimeMap = new Object2LongOpenHashMap();
   private boolean placedStructure;
   private boolean chunksLoaded;
   private int tickCount;
   private boolean started;
   private final RetryOptions retryOptions;
   private final Stopwatch timer = Stopwatch.createUnstarted();
   private boolean done;
   private final Rotation extraRotation;
   private @Nullable GameTestException error;
   private @Nullable TestInstanceBlockEntity testInstanceBlockEntity;

   public GameTestInfo(Holder.Reference<GameTestInstance> p_396420_, Rotation p_127614_, ServerLevel p_127615_, RetryOptions p_328909_) {
      this.test = p_396420_;
      this.level = p_127615_;
      this.retryOptions = p_328909_;
      this.timeoutTicks = p_396420_.value().maxTicks();
      this.extraRotation = p_127614_;
   }

   public void setTestBlockPos(@Nullable BlockPos p_392856_) {
      this.testBlockPos = p_392856_;
   }

   public GameTestInfo startExecution(int p_329736_) {
      this.tickCount = -(this.test.value().setupTicks() + p_329736_ + 1);
      return this;
   }

   public void placeStructure() {
      if (!this.placedStructure) {
         TestInstanceBlockEntity testinstanceblockentity = this.getTestInstanceBlockEntity();
         if (!testinstanceblockentity.placeStructure()) {
            this.fail(Component.translatable("test.error.structure.failure", testinstanceblockentity.getTestName().getString()));
         }

         this.placedStructure = true;
         testinstanceblockentity.encaseStructure();
         BoundingBox boundingbox = testinstanceblockentity.getStructureBoundingBox();
         this.level.getBlockTicks().clearArea(boundingbox);
         this.level.clearBlockEvents(boundingbox);
         this.listeners.forEach(p_127630_ -> p_127630_.testStructureLoaded(this));
      }
   }

   public void tick(GameTestRunner p_334539_) {
      if (!this.isDone()) {
         if (!this.placedStructure) {
            this.fail(Component.translatable("test.error.ticking_without_structure"));
         }

         if (this.testInstanceBlockEntity == null) {
            this.fail(Component.translatable("test.error.missing_block_entity"));
         }

         if (this.error != null) {
            this.finish();
         }

         if (this.chunksLoaded
            || this.testInstanceBlockEntity.getStructureBoundingBox().intersectingChunks().allMatch(this.level::areEntitiesActuallyLoadedAndTicking)) {
            this.chunksLoaded = true;
            this.tickInternal();
            if (this.isDone()) {
               if (this.error != null) {
                  this.listeners.forEach(p_325940_ -> p_325940_.testFailed(this, p_334539_));
               } else {
                  this.listeners.forEach(p_325938_ -> p_325938_.testPassed(this, p_334539_));
               }
            }
         }
      }
   }

   private void tickInternal() {
      this.tickCount++;
      if (this.tickCount >= 0) {
         if (!this.started) {
            this.startTest();
         }

         ObjectIterator<Entry<Runnable>> objectiterator = this.runAtTickTimeMap.object2LongEntrySet().iterator();

         while (objectiterator.hasNext()) {
            Entry<Runnable> entry = (Entry<Runnable>)objectiterator.next();
            if (entry.getLongValue() <= this.tickCount) {
               try {
                  ((Runnable)entry.getKey()).run();
               } catch (GameTestException gametestexception) {
                  this.fail(gametestexception);
               } catch (Exception exception) {
                  this.fail(new UnknownGameTestException(exception));
               }

               objectiterator.remove();
            }
         }

         if (this.tickCount > this.timeoutTicks) {
            if (this.sequences.isEmpty()) {
               this.fail(new GameTestTimeoutException(Component.translatable("test.error.timeout.no_result", this.test.value().maxTicks())));
            } else {
               this.sequences.forEach(p_389764_ -> p_389764_.tickAndFailIfNotComplete(this.tickCount));
               if (this.error == null) {
                  this.fail(new GameTestTimeoutException(Component.translatable("test.error.timeout.no_sequences_finished", this.test.value().maxTicks())));
               }
            }
         } else {
            this.sequences.forEach(p_389763_ -> p_389763_.tickAndContinue(this.tickCount));
         }
      }
   }

   private void startTest() {
      if (!this.started) {
         this.started = true;
         this.timer.start();
         this.getTestInstanceBlockEntity().setRunning();

         try {
            this.test.value().run(new GameTestHelper(this));
         } catch (GameTestException gametestexception) {
            this.fail(gametestexception);
         } catch (Exception exception) {
            this.fail(new UnknownGameTestException(exception));
         }
      }
   }

   public void setRunAtTickTime(long p_177473_, Runnable p_177474_) {
      this.runAtTickTimeMap.put(p_177474_, p_177473_);
   }

   public Identifier id() {
      return this.test.key().identifier();
   }

   public @Nullable BlockPos getTestBlockPos() {
      return this.testBlockPos;
   }

   public BlockPos getTestOrigin() {
      return this.testInstanceBlockEntity.getStartCorner();
   }

   public AABB getStructureBounds() {
      TestInstanceBlockEntity testinstanceblockentity = this.getTestInstanceBlockEntity();
      return testinstanceblockentity.getStructureBounds();
   }

   public TestInstanceBlockEntity getTestInstanceBlockEntity() {
      if (this.testInstanceBlockEntity == null) {
         if (this.testBlockPos == null) {
            throw new IllegalStateException("This GameTestInfo has no position");
         }

         if (this.level.getBlockEntity(this.testBlockPos) instanceof TestInstanceBlockEntity testinstanceblockentity) {
            this.testInstanceBlockEntity = testinstanceblockentity;
         }

         if (this.testInstanceBlockEntity == null) {
            throw new IllegalStateException("Could not find a test instance block entity at the given coordinate " + this.testBlockPos);
         }
      }

      return this.testInstanceBlockEntity;
   }

   public ServerLevel getLevel() {
      return this.level;
   }

   public boolean hasSucceeded() {
      return this.done && this.error == null;
   }

   public boolean hasFailed() {
      return this.error != null;
   }

   public boolean hasStarted() {
      return this.started;
   }

   public boolean isDone() {
      return this.done;
   }

   public long getRunTime() {
      return this.timer.elapsed(TimeUnit.MILLISECONDS);
   }

   private void finish() {
      if (!this.done) {
         this.done = true;
         if (this.timer.isRunning()) {
            this.timer.stop();
         }
      }
   }

   public void succeed() {
      if (this.error == null) {
         this.finish();
         AABB aabb = this.getStructureBounds();
         List<Entity> list = this.getLevel().getEntitiesOfClass(Entity.class, aabb.inflate(1.0), p_308532_ -> !(p_308532_ instanceof Player));
         list.forEach(p_308534_ -> p_308534_.remove(Entity.RemovalReason.DISCARDED));
      }
   }

   public void fail(Component p_397833_) {
      this.fail(new GameTestAssertException(p_397833_, this.tickCount));
   }

   public void fail(GameTestException p_392195_) {
      this.error = p_392195_;
   }

   public @Nullable GameTestException getError() {
      return this.error;
   }

   @Override
   public String toString() {
      return this.id().toString();
   }

   public void addListener(GameTestListener p_127625_) {
      this.listeners.add(p_127625_);
   }

   public @Nullable GameTestInfo prepareTestStructure() {
      TestInstanceBlockEntity testinstanceblockentity = this.createTestInstanceBlock(Objects.requireNonNull(this.testBlockPos), this.extraRotation, this.level);
      if (testinstanceblockentity != null) {
         this.testInstanceBlockEntity = testinstanceblockentity;
         this.placeStructure();
         return this;
      } else {
         return null;
      }
   }

   private @Nullable TestInstanceBlockEntity createTestInstanceBlock(BlockPos p_393936_, Rotation p_391913_, ServerLevel p_395850_) {
      p_395850_.setBlockAndUpdate(p_393936_, Blocks.TEST_INSTANCE_BLOCK.defaultBlockState());
      if (p_395850_.getBlockEntity(p_393936_) instanceof TestInstanceBlockEntity testinstanceblockentity) {
         ResourceKey<GameTestInstance> resourcekey = this.getTestHolder().key();
         Vec3i vec3i = TestInstanceBlockEntity.getStructureSize(p_395850_, resourcekey).orElse(new Vec3i(1, 1, 1));
         testinstanceblockentity.set(
            new TestInstanceBlockEntity.Data(Optional.of(resourcekey), vec3i, p_391913_, false, TestInstanceBlockEntity.Status.CLEARED, Optional.empty())
         );
         return testinstanceblockentity;
      } else {
         return null;
      }
   }

   int getTick() {
      return this.tickCount;
   }

   GameTestSequence createSequence() {
      GameTestSequence gametestsequence = new GameTestSequence(this);
      this.sequences.add(gametestsequence);
      return gametestsequence;
   }

   public boolean isRequired() {
      return this.test.value().required();
   }

   public boolean isOptional() {
      return !this.test.value().required();
   }

   public Identifier getStructure() {
      return this.test.value().structure();
   }

   public Rotation getRotation() {
      return this.test.value().info().rotation().getRotated(this.extraRotation);
   }

   public GameTestInstance getTest() {
      return this.test.value();
   }

   public Holder.Reference<GameTestInstance> getTestHolder() {
      return this.test;
   }

   public int getTimeoutTicks() {
      return this.timeoutTicks;
   }

   public boolean isFlaky() {
      return this.test.value().maxAttempts() > 1;
   }

   public int maxAttempts() {
      return this.test.value().maxAttempts();
   }

   public int requiredSuccesses() {
      return this.test.value().requiredSuccesses();
   }

   public RetryOptions retryOptions() {
      return this.retryOptions;
   }

   public Stream<GameTestListener> getListeners() {
      return this.listeners.stream();
   }

   public GameTestInfo copyReset() {
      GameTestInfo gametestinfo = new GameTestInfo(this.test, this.extraRotation, this.level, this.retryOptions());
      if (this.testBlockPos != null) {
         gametestinfo.setTestBlockPos(this.testBlockPos);
      }

      return gametestinfo;
   }
}
