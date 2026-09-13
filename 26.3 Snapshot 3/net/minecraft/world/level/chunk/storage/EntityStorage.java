package net.minecraft.world.level.chunk.storage;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.slf4j.Logger;

public class EntityStorage implements EntityPersistentStorage<Entity> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String ENTITIES_TAG = "Entities";
   private static final String POSITION_TAG = "Position";
   private final ServerLevel level;
   private final SimpleRegionStorage simpleRegionStorage;
   private final LongSet emptyChunks = new LongOpenHashSet();
   private final ConsecutiveExecutor entityDeserializerQueue;

   public EntityStorage(final SimpleRegionStorage simpleRegionStorage, final ServerLevel level, final Executor mainThreadExecutor) {
      this.simpleRegionStorage = simpleRegionStorage;
      this.level = level;
      this.entityDeserializerQueue = new ConsecutiveExecutor(mainThreadExecutor, "entity-deserializer");
   }

   @Override
   public CompletableFuture<ChunkEntities<Entity>> loadEntities(final ChunkPos pos) {
      if (this.emptyChunks.contains(pos.pack())) {
         return CompletableFuture.completedFuture(emptyChunk(pos));
      }

      CompletableFuture<Optional<CompoundTag>> loadFuture = this.simpleRegionStorage.read(pos);
      this.reportLoadFailureIfPresent(loadFuture, pos);
      return loadFuture.thenApplyAsync(tag -> {
         if (tag.isEmpty()) {
            this.emptyChunks.add(pos.pack());
            return emptyChunk(pos);
         }

         try {
            ChunkPos storedPos = tag.get().<ChunkPos>read("Position", ChunkPos.CODEC).orElseThrow();
            if (!Objects.equals(pos, storedPos)) {
               LOGGER.error("Chunk file at {} is in the wrong location. (Expected {}, got {})", new Object[]{pos, pos, storedPos});
               this.level.getServer().reportMisplacedChunk(storedPos, pos, this.simpleRegionStorage.storageInfo());
            }
         } catch (Exception e) {
            LOGGER.warn("Failed to parse chunk {} position info", pos, e);
            this.level.getServer().reportChunkLoadFailure(e, this.simpleRegionStorage.storageInfo(), pos);
         }

         CompoundTag upgradedChunkTag = this.simpleRegionStorage.upgradeChunkTag(tag.get(), -1);

         try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(ChunkAccess.problemPath(pos), LOGGER)) {
            ValueInput chunkRoot = TagValueInput.create(reporter, this.level.registryAccess(), upgradedChunkTag);
            ValueInput.ValueInputList entities = chunkRoot.childrenListOrEmpty("Entities");
            List<Entity> chunkEntities = EntityType.loadEntitiesRecursive(entities, this.level, EntitySpawnReason.LOAD).toList();
            return new ChunkEntities<>(pos, chunkEntities);
         }
      }, this.entityDeserializerQueue::schedule);
   }

   private static ChunkEntities<Entity> emptyChunk(final ChunkPos pos) {
      return new ChunkEntities<>(pos, List.of());
   }

   @Override
   public void storeEntities(final ChunkEntities<Entity> chunk) {
      ChunkPos pos = chunk.getPos();
      if (chunk.isEmpty()) {
         if (this.emptyChunks.add(pos.pack())) {
            this.reportSaveFailureIfPresent(this.simpleRegionStorage.write(pos, IOWorker.STORE_EMPTY), pos);
         }
      } else {
         try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(ChunkAccess.problemPath(pos), LOGGER)) {
            ListTag entities = new ListTag();
            chunk.getEntities().forEach(e -> {
               TagValueOutput output = TagValueOutput.createWithContext(reporter.forChild(e.problemPath()), e.registryAccess());
               if (e.save(output)) {
                  CompoundTag result = output.buildResult();
                  entities.add(result);
               }
            });
            CompoundTag chunkTag = NbtUtils.addCurrentDataVersion(new CompoundTag());
            chunkTag.put("Entities", entities);
            chunkTag.store("Position", ChunkPos.CODEC, pos);
            this.reportSaveFailureIfPresent(this.simpleRegionStorage.write(pos, chunkTag), pos);
            this.emptyChunks.remove(pos.pack());
         }
      }
   }

   private void reportSaveFailureIfPresent(final CompletableFuture<?> operation, final ChunkPos pos) {
      operation.exceptionally(t -> {
         LOGGER.error("Failed to store entity chunk {}", pos, t);
         this.level.getServer().reportChunkSaveFailure(t, this.simpleRegionStorage.storageInfo(), pos);
         return null;
      });
   }

   private void reportLoadFailureIfPresent(final CompletableFuture<?> operation, final ChunkPos pos) {
      operation.exceptionally(t -> {
         LOGGER.error("Failed to load entity chunk {}", pos, t);
         this.level.getServer().reportChunkLoadFailure(t, this.simpleRegionStorage.storageInfo(), pos);
         return null;
      });
   }

   @Override
   public void flush(final boolean flushStorage) {
      this.simpleRegionStorage.synchronize(flushStorage).join();
      this.entityDeserializerQueue.runAll();
   }

   @Override
   public void close() throws IOException {
      this.simpleRegionStorage.close();
   }
}
