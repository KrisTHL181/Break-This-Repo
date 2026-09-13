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

   public EntityStorage(SimpleRegionStorage p_329511_, ServerLevel p_196924_, Executor p_196928_) {
      this.simpleRegionStorage = p_329511_;
      this.level = p_196924_;
      this.entityDeserializerQueue = new ConsecutiveExecutor(p_196928_, "entity-deserializer");
   }

   @Override
   public CompletableFuture<ChunkEntities<Entity>> loadEntities(ChunkPos p_156551_) {
      if (this.emptyChunks.contains(p_156551_.toLong())) {
         return CompletableFuture.completedFuture(emptyChunk(p_156551_));
      }

      CompletableFuture<Optional<CompoundTag>> completablefuture = this.simpleRegionStorage.read(p_156551_);
      this.reportLoadFailureIfPresent(completablefuture, p_156551_);
      return completablefuture.thenApplyAsync(
         p_405762_ -> {
            if (p_405762_.isEmpty()) {
               this.emptyChunks.add(p_156551_.toLong());
               return emptyChunk(p_156551_);
            }

            try {
               ChunkPos chunkpos = p_405762_.get().<ChunkPos>read("Position", ChunkPos.CODEC).orElseThrow();
               if (!Objects.equals(p_156551_, chunkpos)) {
                  LOGGER.error("Chunk file at {} is in the wrong location. (Expected {}, got {})", new Object[]{p_156551_, p_156551_, chunkpos});
                  this.level.getServer().reportMisplacedChunk(chunkpos, p_156551_, this.simpleRegionStorage.storageInfo());
               }
            } catch (Exception exception) {
               LOGGER.warn("Failed to parse chunk {} position info", p_156551_, exception);
               this.level.getServer().reportChunkLoadFailure(exception, this.simpleRegionStorage.storageInfo(), p_156551_);
            }

            CompoundTag compoundtag = this.simpleRegionStorage.upgradeChunkTag(p_405762_.get(), -1);

            try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
                  ChunkAccess.problemPath(p_156551_), LOGGER
               )) {
               ValueInput valueinput = TagValueInput.create(problemreporter$scopedcollector, this.level.registryAccess(), compoundtag);
               ValueInput.ValueInputList valueinput$valueinputlist = valueinput.childrenListOrEmpty("Entities");
               List<Entity> list = EntityType.loadEntitiesRecursive(valueinput$valueinputlist, this.level, EntitySpawnReason.LOAD).toList();
               return new ChunkEntities<>(p_156551_, list);
            }
         },
         this.entityDeserializerQueue::schedule
      );
   }

   private static ChunkEntities<Entity> emptyChunk(ChunkPos p_156569_) {
      return new ChunkEntities<>(p_156569_, List.of());
   }

   @Override
   public void storeEntities(ChunkEntities<Entity> p_156559_) {
      ChunkPos chunkpos = p_156559_.getPos();
      if (p_156559_.isEmpty()) {
         if (this.emptyChunks.add(chunkpos.toLong())) {
            this.reportSaveFailureIfPresent(this.simpleRegionStorage.write(chunkpos, IOWorker.STORE_EMPTY), chunkpos);
         }
      } else {
         try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(ChunkAccess.problemPath(chunkpos), LOGGER)) {
            ListTag listtag = new ListTag();
            p_156559_.getEntities()
               .forEach(
                  p_405760_ -> {
                     TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(
                        problemreporter$scopedcollector.forChild(p_405760_.problemPath()), p_405760_.registryAccess()
                     );
                     if (p_405760_.save(tagvalueoutput)) {
                        CompoundTag compoundtag1 = tagvalueoutput.buildResult();
                        listtag.add(compoundtag1);
                     }
                  }
               );
            CompoundTag compoundtag = NbtUtils.addCurrentDataVersion(new CompoundTag());
            compoundtag.put("Entities", listtag);
            compoundtag.store("Position", ChunkPos.CODEC, chunkpos);
            this.reportSaveFailureIfPresent(this.simpleRegionStorage.write(chunkpos, compoundtag), chunkpos);
            this.emptyChunks.remove(chunkpos.toLong());
         }
      }
   }

   private void reportSaveFailureIfPresent(CompletableFuture<?> p_343321_, ChunkPos p_343781_) {
      p_343321_.exceptionally(p_341884_ -> {
         LOGGER.error("Failed to store entity chunk {}", p_343781_, p_341884_);
         this.level.getServer().reportChunkSaveFailure(p_341884_, this.simpleRegionStorage.storageInfo(), p_343781_);
         return null;
      });
   }

   private void reportLoadFailureIfPresent(CompletableFuture<?> p_344653_, ChunkPos p_345292_) {
      p_344653_.exceptionally(p_341888_ -> {
         LOGGER.error("Failed to load entity chunk {}", p_345292_, p_341888_);
         this.level.getServer().reportChunkLoadFailure(p_341888_, this.simpleRegionStorage.storageInfo(), p_345292_);
         return null;
      });
   }

   @Override
   public void flush(boolean p_182487_) {
      this.simpleRegionStorage.synchronize(p_182487_).join();
      this.entityDeserializerQueue.runAll();
   }

   @Override
   public void close() throws IOException {
      this.simpleRegionStorage.close();
   }
}
