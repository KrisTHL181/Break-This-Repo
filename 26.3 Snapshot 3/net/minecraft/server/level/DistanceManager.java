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
   private static final int PLAYER_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);
   private final Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk = new Long2ObjectOpenHashMap();
   private final LoadingChunkTracker loadingChunkTracker;
   private final SimulationChunkTracker simulationChunkTracker;
   private final TicketStorage ticketStorage;
   private final DistanceManager.FixedPlayerDistanceChunkTracker naturalSpawnChunkCounter = new DistanceManager.FixedPlayerDistanceChunkTracker(8);
   private final DistanceManager.PlayerTicketTracker playerTicketManager = new DistanceManager.PlayerTicketTracker(32);
   protected final Set<ChunkHolder> chunksToUpdateFutures = new ReferenceOpenHashSet();
   private final ThrottlingChunkTaskDispatcher ticketDispatcher;
   private final LongSet ticketsToRelease = new LongOpenHashSet();
   private final Executor mainThreadExecutor;
   private int simulationDistance = 10;

   protected DistanceManager(final TicketStorage ticketStorage, final Executor executor, final Executor mainThreadExecutor) {
      this.ticketStorage = ticketStorage;
      this.loadingChunkTracker = new LoadingChunkTracker(this, ticketStorage);
      this.simulationChunkTracker = new SimulationChunkTracker(ticketStorage);
      TaskScheduler<Runnable> mainThreadTaskScheduler = TaskScheduler.wrapExecutor("player ticket throttler", mainThreadExecutor);
      this.ticketDispatcher = new ThrottlingChunkTaskDispatcher(mainThreadTaskScheduler, executor, 4);
      this.mainThreadExecutor = mainThreadExecutor;
   }

   protected abstract boolean isChunkToRemove(final long node);

   protected abstract @Nullable ChunkHolder getChunk(final long node);

   protected abstract @Nullable ChunkHolder updateChunkScheduling(final long node, final int level, final @Nullable ChunkHolder chunk, final int oldLevel);

   public boolean runAllUpdates(final ChunkMap scheduler) {
      this.naturalSpawnChunkCounter.runAllUpdates();
      this.simulationChunkTracker.runAllUpdates();
      this.playerTicketManager.runAllUpdates();
      int updates = Integer.MAX_VALUE - this.loadingChunkTracker.runDistanceUpdates(Integer.MAX_VALUE);
      boolean updated = updates != 0;
      if (updated && SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
         LOGGER.debug("DMU {}", updates);
      }

      if (!this.chunksToUpdateFutures.isEmpty()) {
         for (ChunkHolder chunksToUpdateFuture : this.chunksToUpdateFutures) {
            chunksToUpdateFuture.updateHighestAllowedStatus(scheduler);
         }

         for (ChunkHolder chunkHolder : this.chunksToUpdateFutures) {
            chunkHolder.updateFutures(scheduler, this.mainThreadExecutor);
         }

         this.chunksToUpdateFutures.clear();
         return true;
      } else {
         if (!this.ticketsToRelease.isEmpty()) {
            LongIterator iterator = this.ticketsToRelease.iterator();

            while (iterator.hasNext()) {
               long pos = iterator.nextLong();
               if (this.ticketStorage.getTickets(pos).stream().anyMatch(t -> t.getType() == TicketType.PLAYER_LOADING)) {
                  ChunkHolder chunk = scheduler.getUpdatingChunkIfPresent(pos);
                  if (chunk == null) {
                     throw new IllegalStateException();
                  }

                  CompletableFuture<ChunkResult<LevelChunk>> future = chunk.getEntityTickingChunkFuture();
                  future.thenAccept(c -> this.mainThreadExecutor.execute(() -> this.ticketDispatcher.release(pos, () -> {}, false)));
               }
            }

            this.ticketsToRelease.clear();
         }

         return updated;
      }
   }

   public void addPlayer(final SectionPos pos, final ServerPlayer player) {
      ChunkPos chunk = pos.chunk();
      long chunkPos = chunk.pack();
      ((ObjectSet)this.playersPerChunk.computeIfAbsent(chunkPos, k -> new ObjectOpenHashSet())).add(player);
      this.naturalSpawnChunkCounter.update(chunkPos, 0, true);
      this.playerTicketManager.update(chunkPos, 0, true);
      this.ticketStorage.addTicket(new Ticket(TicketType.PLAYER_SIMULATION, this.getPlayerTicketLevel()), chunk);
   }

   public void removePlayer(final SectionPos pos, final ServerPlayer player) {
      ChunkPos chunk = pos.chunk();
      long chunkPos = chunk.pack();
      ObjectSet<ServerPlayer> chunkPlayers = (ObjectSet<ServerPlayer>)this.playersPerChunk.get(chunkPos);
      chunkPlayers.remove(player);
      if (chunkPlayers.isEmpty()) {
         this.playersPerChunk.remove(chunkPos);
         this.naturalSpawnChunkCounter.update(chunkPos, Integer.MAX_VALUE, false);
         this.playerTicketManager.update(chunkPos, Integer.MAX_VALUE, false);
         this.ticketStorage.removeTicket(new Ticket(TicketType.PLAYER_SIMULATION, this.getPlayerTicketLevel()), chunk);
      }
   }

   private int getPlayerTicketLevel() {
      return Math.max(0, ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING) - this.simulationDistance);
   }

   public boolean inEntityTickingRange(final long key) {
      return ChunkLevel.isEntityTicking(this.simulationChunkTracker.getLevel(key));
   }

   public boolean inBlockTickingRange(final long key) {
      return ChunkLevel.isBlockTicking(this.simulationChunkTracker.getLevel(key));
   }

   public int getChunkLevel(final long key, final boolean simulation) {
      return simulation ? this.simulationChunkTracker.getLevel(key) : this.loadingChunkTracker.getLevel(key);
   }

   protected void updatePlayerTickets(final int viewDistance) {
      this.playerTicketManager.updateViewDistance(viewDistance);
   }

   public void updateSimulationDistance(final int newDistance) {
      if (newDistance != this.simulationDistance) {
         this.simulationDistance = newDistance;
         this.ticketStorage.replaceTicketLevelOfType(this.getPlayerTicketLevel(), TicketType.PLAYER_SIMULATION);
      }
   }

   public int getNaturalSpawnChunkCount() {
      this.naturalSpawnChunkCounter.runAllUpdates();
      return this.naturalSpawnChunkCounter.chunks.size();
   }

   public TriState hasPlayersNearby(final long pos) {
      this.naturalSpawnChunkCounter.runAllUpdates();
      int distance = this.naturalSpawnChunkCounter.getLevel(pos);
      if (distance <= NaturalSpawner.INSCRIBED_SQUARE_SPAWN_DISTANCE_CHUNK) {
         return TriState.TRUE;
      } else {
         return distance > 8 ? TriState.FALSE : TriState.DEFAULT;
      }
   }

   public void forEachEntityTickingChunk(final LongConsumer consumer) {
      ObjectIterator var2 = Long2ByteMaps.fastIterable(this.simulationChunkTracker.chunks).iterator();

      while (var2.hasNext()) {
         Entry entry = (Entry)var2.next();
         byte level = entry.getByteValue();
         long key = entry.getLongKey();
         if (ChunkLevel.isEntityTicking(level)) {
            consumer.accept(key);
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

   private class FixedPlayerDistanceChunkTracker extends ChunkTracker {
      protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
      protected final int maxDistance;

      protected FixedPlayerDistanceChunkTracker(final int maxDistance) {
         super(maxDistance + 2, 16, 256);
         this.maxDistance = maxDistance;
         this.chunks.defaultReturnValue((byte)(maxDistance + 2));
      }

      @Override
      protected int getLevel(final long node) {
         return this.chunks.get(node);
      }

      @Override
      protected void setLevel(final long node, final int level) {
         byte oldLevel;
         if (level > this.maxDistance) {
            oldLevel = this.chunks.remove(node);
         } else {
            oldLevel = this.chunks.put(node, (byte)level);
         }

         this.onLevelChange(node, oldLevel, level);
      }

      protected void onLevelChange(final long node, final int oldLevel, final int level) {
      }

      @Override
      protected int getLevelFromSource(final long to) {
         return this.havePlayer(to) ? 0 : Integer.MAX_VALUE;
      }

      private boolean havePlayer(final long chunkPos) {
         ObjectSet<ServerPlayer> players = (ObjectSet<ServerPlayer>)DistanceManager.this.playersPerChunk.get(chunkPos);
         return players != null && !players.isEmpty();
      }

      public void runAllUpdates() {
         this.runUpdates(Integer.MAX_VALUE);
      }
   }

   private class PlayerTicketTracker extends DistanceManager.FixedPlayerDistanceChunkTracker {
      private int viewDistance;
      private final Long2IntMap queueLevels = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
      private final LongSet toUpdate = new LongOpenHashSet();

      protected PlayerTicketTracker(final int maxDistance) {
         super(maxDistance);
         this.viewDistance = 0;
         this.queueLevels.defaultReturnValue(maxDistance + 2);
      }

      @Override
      protected void onLevelChange(final long node, final int oldLevel, final int level) {
         this.toUpdate.add(node);
      }

      public void updateViewDistance(final int viewDistance) {
         ObjectIterator var2 = this.chunks.long2ByteEntrySet().iterator();

         while (var2.hasNext()) {
            Entry entry = (Entry)var2.next();
            byte level = entry.getByteValue();
            long key = entry.getLongKey();
            this.onLevelChange(key, level, this.haveTicketFor(level), level <= viewDistance);
         }

         this.viewDistance = viewDistance;
      }

      private void onLevelChange(final long key, final int level, final boolean saw, final boolean sees) {
         if (saw != sees) {
            Ticket ticket = new Ticket(TicketType.PLAYER_LOADING, DistanceManager.PLAYER_TICKET_LEVEL);
            if (sees) {
               DistanceManager.this.ticketDispatcher.submit(() -> DistanceManager.this.mainThreadExecutor.execute(() -> {
                  if (this.haveTicketFor(this.getLevel(key))) {
                     DistanceManager.this.ticketStorage.addTicket(key, ticket);
                     DistanceManager.this.ticketsToRelease.add(key);
                  } else {
                     DistanceManager.this.ticketDispatcher.release(key, () -> {}, false);
                  }
               }), key, () -> level);
            } else {
               DistanceManager.this.ticketDispatcher
                  .release(key, () -> DistanceManager.this.mainThreadExecutor.execute(() -> DistanceManager.this.ticketStorage.removeTicket(key, ticket)), true);
            }
         }
      }

      @Override
      public void runAllUpdates() {
         super.runAllUpdates();
         if (!this.toUpdate.isEmpty()) {
            LongIterator iterator = this.toUpdate.iterator();

            while (iterator.hasNext()) {
               long node = iterator.nextLong();
               int oldLevel = this.queueLevels.get(node);
               int level = this.getLevel(node);
               if (oldLevel != level) {
                  DistanceManager.this.ticketDispatcher.onLevelChange(ChunkPos.unpack(node), () -> this.queueLevels.get(node), level, l -> {
                     if (l >= this.queueLevels.defaultReturnValue()) {
                        this.queueLevels.remove(node);
                     } else {
                        this.queueLevels.put(node, l);
                     }
                  });
                  this.onLevelChange(node, level, this.haveTicketFor(oldLevel), this.haveTicketFor(level));
               }
            }

            this.toUpdate.clear();
         }
      }

      private boolean haveTicketFor(final int level) {
         return level <= this.viewDistance;
      }
   }
}
