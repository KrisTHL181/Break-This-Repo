package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientChunkCache extends ChunkSource {
   static final Logger LOGGER = LogUtils.getLogger();
   private final LevelChunk emptyChunk;
   private final LevelLightEngine lightEngine;
   volatile ClientChunkCache.Storage storage;
   final ClientLevel level;

   public ClientChunkCache(ClientLevel p_104414_, int p_104415_) {
      this.level = p_104414_;
      this.emptyChunk = new EmptyLevelChunk(p_104414_, new ChunkPos(0, 0), p_104414_.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS));
      this.lightEngine = new LevelLightEngine(this, true, p_104414_.dimensionType().hasSkyLight());
      this.storage = new ClientChunkCache.Storage(calculateStorageRange(p_104415_));
   }

   @Override
   public LevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   private static boolean isValidChunk(@Nullable LevelChunk p_104439_, int p_104440_, int p_104441_) {
      if (p_104439_ == null) {
         return false;
      }

      ChunkPos chunkpos = p_104439_.getPos();
      return chunkpos.x == p_104440_ && chunkpos.z == p_104441_;
   }

   public void drop(ChunkPos p_298665_) {
      if (this.storage.inRange(p_298665_.x, p_298665_.z)) {
         int i = this.storage.getIndex(p_298665_.x, p_298665_.z);
         LevelChunk levelchunk = this.storage.getChunk(i);
         if (isValidChunk(levelchunk, p_298665_.x, p_298665_.z)) {
            this.storage.drop(i, levelchunk);
         }
      }
   }

   public @Nullable LevelChunk getChunk(int p_104451_, int p_104452_, ChunkStatus p_334602_, boolean p_104454_) {
      if (this.storage.inRange(p_104451_, p_104452_)) {
         LevelChunk levelchunk = this.storage.getChunk(this.storage.getIndex(p_104451_, p_104452_));
         if (isValidChunk(levelchunk, p_104451_, p_104452_)) {
            return levelchunk;
         }
      }

      return p_104454_ ? this.emptyChunk : null;
   }

   @Override
   public BlockGetter getLevel() {
      return this.level;
   }

   public void replaceBiomes(int p_275374_, int p_275226_, FriendlyByteBuf p_275745_) {
      if (!this.storage.inRange(p_275374_, p_275226_)) {
         LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", p_275374_, p_275226_);
      } else {
         int i = this.storage.getIndex(p_275374_, p_275226_);
         LevelChunk levelchunk = this.storage.chunks.get(i);
         if (!isValidChunk(levelchunk, p_275374_, p_275226_)) {
            LOGGER.warn("Ignoring chunk since it's not present: {}, {}", p_275374_, p_275226_);
         } else {
            levelchunk.replaceBiomes(p_275745_);
         }
      }
   }

   public @Nullable LevelChunk replaceWithPacketData(
      int p_194117_,
      int p_194118_,
      FriendlyByteBuf p_194119_,
      Map<Heightmap.Types, long[]> p_392080_,
      Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> p_194121_
   ) {
      if (!this.storage.inRange(p_194117_, p_194118_)) {
         LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", p_194117_, p_194118_);
         return null;
      }

      int i = this.storage.getIndex(p_194117_, p_194118_);
      LevelChunk levelchunk = this.storage.chunks.get(i);
      ChunkPos chunkpos = new ChunkPos(p_194117_, p_194118_);
      if (!isValidChunk(levelchunk, p_194117_, p_194118_)) {
         levelchunk = new LevelChunk(this.level, chunkpos);
         levelchunk.replaceWithPacketData(p_194119_, p_392080_, p_194121_);
         this.storage.replace(i, levelchunk);
      } else {
         levelchunk.replaceWithPacketData(p_194119_, p_392080_, p_194121_);
         this.storage.refreshEmptySections(levelchunk);
      }

      this.level.onChunkLoaded(chunkpos);
      return levelchunk;
   }

   @Override
   public void tick(BooleanSupplier p_202421_, boolean p_202422_) {
   }

   public void updateViewCenter(int p_104460_, int p_104461_) {
      this.storage.viewCenterX = p_104460_;
      this.storage.viewCenterZ = p_104461_;
   }

   public void updateViewRadius(int p_104417_) {
      int i = this.storage.chunkRadius;
      int j = calculateStorageRange(p_104417_);
      if (i != j) {
         ClientChunkCache.Storage clientchunkcache$storage = new ClientChunkCache.Storage(j);
         clientchunkcache$storage.viewCenterX = this.storage.viewCenterX;
         clientchunkcache$storage.viewCenterZ = this.storage.viewCenterZ;

         for (int k = 0; k < this.storage.chunks.length(); k++) {
            LevelChunk levelchunk = this.storage.chunks.get(k);
            if (levelchunk != null) {
               ChunkPos chunkpos = levelchunk.getPos();
               if (clientchunkcache$storage.inRange(chunkpos.x, chunkpos.z)) {
                  clientchunkcache$storage.replace(clientchunkcache$storage.getIndex(chunkpos.x, chunkpos.z), levelchunk);
               }
            }
         }

         this.storage = clientchunkcache$storage;
      }
   }

   private static int calculateStorageRange(int p_104449_) {
      return Math.max(2, p_104449_) + 3;
   }

   @Override
   public String gatherStats() {
      return this.storage.chunks.length() + ", " + this.getLoadedChunksCount();
   }

   @Override
   public int getLoadedChunksCount() {
      return this.storage.chunkCount;
   }

   @Override
   public void onLightUpdate(LightLayer p_104436_, SectionPos p_104437_) {
      Minecraft.getInstance().levelRenderer.setSectionDirty(p_104437_.x(), p_104437_.y(), p_104437_.z());
   }

   public LongOpenHashSet getLoadedEmptySections() {
      return this.storage.loadedEmptySections;
   }

   @Override
   public void onSectionEmptinessChanged(int p_366771_, int p_363867_, int p_364686_, boolean p_362705_) {
      this.storage.onSectionEmptinessChanged(p_366771_, p_363867_, p_364686_, p_362705_);
   }

   @OnlyIn(Dist.CLIENT)
   final class Storage {
      final AtomicReferenceArray<@Nullable LevelChunk> chunks;
      final LongOpenHashSet loadedEmptySections = new LongOpenHashSet();
      final int chunkRadius;
      private final int viewRange;
      volatile int viewCenterX;
      volatile int viewCenterZ;
      int chunkCount;

      Storage(final int p_104474_) {
         this.chunkRadius = p_104474_;
         this.viewRange = p_104474_ * 2 + 1;
         this.chunks = new AtomicReferenceArray<>(this.viewRange * this.viewRange);
      }

      int getIndex(int p_104482_, int p_104483_) {
         return Math.floorMod(p_104483_, this.viewRange) * this.viewRange + Math.floorMod(p_104482_, this.viewRange);
      }

      void replace(int p_104485_, @Nullable LevelChunk p_104486_) {
         LevelChunk levelchunk = this.chunks.getAndSet(p_104485_, p_104486_);
         if (levelchunk != null) {
            this.chunkCount--;
            this.dropEmptySections(levelchunk);
            ClientChunkCache.this.level.unload(levelchunk);
         }

         if (p_104486_ != null) {
            this.chunkCount++;
            this.addEmptySections(p_104486_);
         }
      }

      void drop(int p_363490_, LevelChunk p_364643_) {
         if (this.chunks.compareAndSet(p_363490_, p_364643_, null)) {
            this.chunkCount--;
            this.dropEmptySections(p_364643_);
         }

         ClientChunkCache.this.level.unload(p_364643_);
      }

      public void onSectionEmptinessChanged(int p_366132_, int p_369453_, int p_368987_, boolean p_370106_) {
         if (this.inRange(p_366132_, p_368987_)) {
            long i = SectionPos.asLong(p_366132_, p_369453_, p_368987_);
            if (p_370106_) {
               this.loadedEmptySections.add(i);
            } else if (this.loadedEmptySections.remove(i)) {
               ClientChunkCache.this.level.onSectionBecomingNonEmpty(i);
            }
         }
      }

      private void dropEmptySections(LevelChunk p_364563_) {
         LevelChunkSection[] alevelchunksection = p_364563_.getSections();

         for (int i = 0; i < alevelchunksection.length; i++) {
            ChunkPos chunkpos = p_364563_.getPos();
            this.loadedEmptySections.remove(SectionPos.asLong(chunkpos.x, p_364563_.getSectionYFromSectionIndex(i), chunkpos.z));
         }
      }

      private void addEmptySections(LevelChunk p_362756_) {
         LevelChunkSection[] alevelchunksection = p_362756_.getSections();

         for (int i = 0; i < alevelchunksection.length; i++) {
            LevelChunkSection levelchunksection = alevelchunksection[i];
            if (levelchunksection.hasOnlyAir()) {
               ChunkPos chunkpos = p_362756_.getPos();
               this.loadedEmptySections.add(SectionPos.asLong(chunkpos.x, p_362756_.getSectionYFromSectionIndex(i), chunkpos.z));
            }
         }
      }

      void refreshEmptySections(LevelChunk p_377131_) {
         ChunkPos chunkpos = p_377131_.getPos();
         LevelChunkSection[] alevelchunksection = p_377131_.getSections();

         for (int i = 0; i < alevelchunksection.length; i++) {
            LevelChunkSection levelchunksection = alevelchunksection[i];
            long j = SectionPos.asLong(chunkpos.x, p_377131_.getSectionYFromSectionIndex(i), chunkpos.z);
            if (levelchunksection.hasOnlyAir()) {
               this.loadedEmptySections.add(j);
            } else if (this.loadedEmptySections.remove(j)) {
               ClientChunkCache.this.level.onSectionBecomingNonEmpty(j);
            }
         }
      }

      boolean inRange(int p_104501_, int p_104502_) {
         return Math.abs(p_104501_ - this.viewCenterX) <= this.chunkRadius && Math.abs(p_104502_ - this.viewCenterZ) <= this.chunkRadius;
      }

      protected @Nullable LevelChunk getChunk(int p_104480_) {
         return this.chunks.get(p_104480_);
      }

      private void dumpChunks(String p_171623_) {
         try (FileOutputStream fileoutputstream = new FileOutputStream(p_171623_)) {
            int i = ClientChunkCache.this.storage.chunkRadius;

            for (int j = this.viewCenterZ - i; j <= this.viewCenterZ + i; j++) {
               for (int k = this.viewCenterX - i; k <= this.viewCenterX + i; k++) {
                  LevelChunk levelchunk = ClientChunkCache.this.storage.chunks.get(ClientChunkCache.this.storage.getIndex(k, j));
                  if (levelchunk != null) {
                     ChunkPos chunkpos = levelchunk.getPos();
                     fileoutputstream.write((chunkpos.x + "\t" + chunkpos.z + "\t" + levelchunk.isEmpty() + "\n").getBytes(StandardCharsets.UTF_8));
                  }
               }
            }
         } catch (IOException ioexception) {
            ClientChunkCache.LOGGER.error("Failed to dump chunks to file {}", p_171623_, ioexception);
         }
      }
   }
}
