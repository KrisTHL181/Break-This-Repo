package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongConsumer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.SharedConstants;
import net.minecraft.core.SectionPos;
import net.minecraft.util.TriState;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class DistanceManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final int PLAYER_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);
   final Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk = new Long2ObjectOpenHashMap();
   private final LoadingChunkTracker loadingChunkTracker;
   private final SimulationChunkTracker simulationChunkTracker;
   final TicketStorage ticketStorage;
   private final DistanceManager.FixedPlayerDistanceChunkTracker naturalSpawnChunkCounter = new DistanceManager.FixedPlayerDistanceChunkTracker(8);
   private final DistanceManager.PlayerTicketTracker playerTicketManager = new DistanceManager.PlayerTicketTracker(32);
   protected final Set<ChunkHolder> chunksToUpdateFutures = new ReferenceOpenHashSet();
   final ThrottlingChunkTaskDispatcher ticketDispatcher;
   final LongSet ticketsToRelease = new LongOpenHashSet();
   final Executor mainThreadExecutor;
   private int simulationDistance = 10;

   protected DistanceManager(TicketStorage p_395018_, Executor p_140774_, Executor p_140775_) {
      this.ticketStorage = p_395018_;
      this.loadingChunkTracker = new LoadingChunkTracker(this, p_395018_);
      this.simulationChunkTracker = new SimulationChunkTracker(p_395018_);
      TaskScheduler<Runnable> taskscheduler = TaskScheduler.wrapExecutor("player ticket throttler", p_140775_);
      this.ticketDispatcher = new ThrottlingChunkTaskDispatcher(taskscheduler, p_140774_, 4);
      this.mainThreadExecutor = p_140775_;
   }

   protected abstract boolean isChunkToRemove(long var1);

   protected abstract @Nullable ChunkHolder getChunk(long var1);

   protected abstract @Nullable ChunkHolder updateChunkScheduling(long var1, int var3, @Nullable ChunkHolder var4, int var5);

   public boolean runAllUpdates(ChunkMap p_140806_) {
      this.naturalSpawnChunkCounter.runAllUpdates();
      this.simulationChunkTracker.runAllUpdates();
      this.playerTicketManager.runAllUpdates();
      int i = Integer.MAX_VALUE - this.loadingChunkTracker.runDistanceUpdates(Integer.MAX_VALUE);
      boolean flag = i != 0;
      if (flag && SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
         LOGGER.debug("DMU {}", i);
      }

      if (!this.chunksToUpdateFutures.isEmpty()) {
         for (ChunkHolder chunkholder1 : this.chunksToUpdateFutures) {
            chunkholder1.updateHighestAllowedStatus(p_140806_);
         }

         for (ChunkHolder chunkholder2 : this.chunksToUpdateFutures) {
            chunkholder2.updateFutures(p_140806_, this.mainThreadExecutor);
         }

         this.chunksToUpdateFutures.clear();
         return true;
      } else {
         if (!this.ticketsToRelease.isEmpty()) {
            LongIterator longiterator = this.ticketsToRelease.iterator();

            while (longiterator.hasNext()) {
               long j = longiterator.nextLong();
               if (this.ticketStorage.getTickets(j).stream().anyMatch(p_390137_ -> p_390137_.getType() == TicketType.PLAYER_LOADING)) {
                  ChunkHolder chunkholder = p_140806_.getUpdatingChunkIfPresent(j);
                  if (chunkholder == null) {
                     throw new IllegalStateException();
                  }

                  CompletableFuture<ChunkResult<LevelChunk>> completablefuture = chunkholder.getEntityTickingChunkFuture();
                  completablefuture.thenAccept(p_336030_ -> this.mainThreadExecutor.execute(() -> this.ticketDispatcher.release(j, () -> {}, false)));
               }
            }

            this.ticketsToRelease.clear();
         }

         return flag;
      }
   }

   public void addPlayer(SectionPos p_140803_, ServerPlayer p_140804_) {
      ChunkPos chunkpos = p_140803_.chunk();
      long i = chunkpos.toLong();
      ((ObjectSet)this.playersPerChunk.computeIfAbsent(i, p_183921_ -> new ObjectOpenHashSet())).add(p_140804_);
      this.naturalSpawnChunkCounter.update(i, 0, true);
      this.playerTicketManager.update(i, 0, true);
      this.ticketStorage.addTicket(new Ticket(TicketType.PLAYER_SIMULATION, this.getPlayerTicketLevel()), chunkpos);
   }

   public void removePlayer(SectionPos p_140829_, ServerPlayer p_140830_) {
      ChunkPos chunkpos = p_140829_.chunk();
      long i = chunkpos.toLong();
      ObjectSet<ServerPlayer> objectset = (ObjectSet<ServerPlayer>)this.playersPerChunk.get(i);
      objectset.remove(p_140830_);
      if (objectset.isEmpty()) {
         this.playersPerChunk.remove(i);
         this.naturalSpawnChunkCounter.update(i, Integer.MAX_VALUE, false);
         this.playerTicketManager.update(i, Integer.MAX_VALUE, false);
         this.ticketStorage.removeTicket(new Ticket(TicketType.PLAYER_SIMULATION, this.getPlayerTicketLevel()), chunkpos);
      }
   }

   private int getPlayerTicketLevel() {
      return Math.max(0, ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING) - this.simulationDistance);
   }

   public boolean inEntityTickingRange(long p_183914_) {
      return ChunkLevel.isEntityTicking(this.simulationChunkTracker.getLevel(p_183914_));
   }

   public boolean inBlockTickingRange(long p_183917_) {
      return ChunkLevel.isBlockTicking(this.simulationChunkTracker.getLevel(p_183917_));
   }

   public int getChunkLevel(long p_392779_, boolean p_394642_) {
      return p_394642_ ? this.simulationChunkTracker.getLevel(p_392779_) : this.loadingChunkTracker.getLevel(p_392779_);
   }

   protected void updatePlayerTickets(int p_140778_) {
      this.playerTicketManager.updateViewDistance(p_140778_);
   }

   public void updateSimulationDistance(int p_183912_) {
      if (p_183912_ != this.simulationDistance) {
         this.simulationDistance = p_183912_;
         this.ticketStorage.replaceTicketLevelOfType(this.getPlayerTicketLevel(), TicketType.PLAYER_SIMULATION);
      }
   }

   public int getNaturalSpawnChunkCount() {
      this.naturalSpawnChunkCounter.runAllUpdates();
      return this.naturalSpawnChunkCounter.chunks.size();
   }

   public TriState hasPlayersNearby(long p_140848_) {
      this.naturalSpawnChunkCounter.runAllUpdates();
      int i = this.naturalSpawnChunkCounter.getLevel(p_140848_);
      if (i <= NaturalSpawner.INSCRIBED_SQUARE_SPAWN_DISTANCE_CHUNK) {
         return TriState.TRUE;
      } else {
         return i > 8 ? TriState.FALSE : TriState.DEFAULT;
      }
   }

   public void forEachEntityTickingChunk(LongConsumer p_397433_) {
      ObjectIterator var2 = Long2ByteMaps.fastIterable(this.simulationChunkTracker.chunks).iterator();

      while (var2.hasNext()) {
         Entry entry = (Entry)var2.next();
         byte b0 = entry.getByteValue();
         long i = entry.getLongKey();
         if (ChunkLevel.isEntityTicking(b0)) {
            p_397433_.accept(i);
         }
      }
   }

   public LongIterator getSpawnCandidateChunks() {
      this.naturalSpawnChunkCounter.runAllUpdates();
      return this.naturalSpawnChunkCounter.chunks.keySet().iterator();
   }

   public String getDebugStatus() {
      return this.ticketDispatcher.getDebugStatus();
   }

   public boolean hasTickets() {
      return this.ticketStorage.hasTickets();
   }

   class FixedPlayerDistanceChunkTracker extends ChunkTracker {
      protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
      protected final int maxDistance;

      protected FixedPlayerDistanceChunkTracker(final int p_140891_) {
         super(p_140891_ + 2, 16, 256);
         this.maxDistance = p_140891_;
         this.chunks.defaultReturnValue((byte)(p_140891_ + 2));
      }

      @Override
      protected int getLevel(long p_140901_) {
         return this.chunks.get(p_140901_);
      }

      @Override
      protected void setLevel(long p_140893_, int p_140894_) {
         byte b0;
         if (p_140894_ > this.maxDistance) {
            b0 = this.chunks.remove(p_140893_);
         } else {
            b0 = this.chunks.put(p_140893_, (byte)p_140894_);
         }

         this.onLevelChange(p_140893_, b0, p_140894_);
      }

      protected void onLevelChange(long p_140895_, int p_140896_, int p_140897_) {
      }

      @Override
      protected int getLevelFromSource(long p_140899_) {
         return this.havePlayer(p_140899_) ? 0 : Integer.MAX_VALUE;
      }

      private boolean havePlayer(long p_140903_) {
         ObjectSet<ServerPlayer> objectset = (ObjectSet<ServerPlayer>)DistanceManager.this.playersPerChunk.get(p_140903_);
         return objectset != null && !objectset.isEmpty();
      }

      public void runAllUpdates() {
         this.runUpdates(Integer.MAX_VALUE);
      }
   }

   class PlayerTicketTracker extends DistanceManager.FixedPlayerDistanceChunkTracker {
      private int viewDistance;
      private final Long2IntMap queueLevels = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
      private final LongSet toUpdate = new LongOpenHashSet();

      protected PlayerTicketTracker(final int p_140910_) {
         super(p_140910_);
         this.viewDistance = 0;
         this.queueLevels.defaultReturnValue(p_140910_ + 2);
      }

      @Override
      protected void onLevelChange(long p_140915_, int p_140916_, int p_140917_) {
         this.toUpdate.add(p_140915_);
      }

      public void updateViewDistance(int p_140913_) {
         ObjectIterator var2 = this.chunks.long2ByteEntrySet().iterator();

         while (var2.hasNext()) {
            Entry entry = (Entry)var2.next();
            byte b0 = entry.getByteValue();
            long i = entry.getLongKey();
            this.onLevelChange(i, b0, this.haveTicketFor(b0), b0 <= p_140913_);
         }

         this.viewDistance = p_140913_;
      }

      private void onLevelChange(long p_140919_, int p_140920_, boolean p_140921_, boolean p_140922_) {
         if (p_140921_ != p_140922_) {
            Ticket ticket = new Ticket(TicketType.PLAYER_LOADING, DistanceManager.PLAYER_TICKET_LEVEL);
            if (p_140922_) {
               DistanceManager.this.ticketDispatcher.submit(() -> DistanceManager.this.mainThreadExecutor.execute(() -> {
                  if (this.haveTicketFor(this.getLevel(p_140919_))) {
                     DistanceManager.this.ticketStorage.addTicket(p_140919_, ticket);
                     DistanceManager.this.ticketsToRelease.add(p_140919_);
                  } else {
                     DistanceManager.this.ticketDispatcher.release(p_140919_, () -> {}, false);
                  }
               }), p_140919_, () -> p_140920_);
            } else {
               DistanceManager.this.ticketDispatcher
                  .release(
                     p_140919_,
                     () -> DistanceManager.this.mainThreadExecutor.execute(() -> DistanceManager.this.ticketStorage.removeTicket(p_140919_, ticket)),
                     true
                  );
            }
         }
      }

      @Override
      public void runAllUpdates() {
         super.runAllUpdates();
         if (!this.toUpdate.isEmpty()) {
            LongIterator longiterator = this.toUpdate.iterator();

            while (longiterator.hasNext()) {
               long i = longiterator.nextLong();
               int j = this.queueLevels.get(i);
               int k = this.getLevel(i);
               if (j != k) {
                  DistanceManager.this.ticketDispatcher.onLevelChange(new ChunkPos(i), () -> this.queueLevels.get(i), k, p_140928_ -> {
                     if (p_140928_ >= this.queueLevels.defaultReturnValue()) {
                        this.queueLevels.remove(i);
                     } else {
                        this.queueLevels.put(i, p_140928_);
                     }
                  });
                  this.onLevelChange(i, k, this.haveTicketFor(j), this.haveTicketFor(k));
               }
            }

            this.toUpdate.clear();
         }
      }

      private boolean haveTicketFor(int p_140933_) {
         return p_140933_ <= this.viewDistance;
      }
   }
}
