package net.minecraft.util.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundDebugBlockValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEntityValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEventPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public class LevelDebugSynchronizers {
   private final ServerLevel level;
   private final List<TrackingDebugSynchronizer<?>> allSynchronizers = new ArrayList<>();
   private final Map<DebugSubscription<?>, TrackingDebugSynchronizer.SourceSynchronizer<?>> sourceSynchronizers = new HashMap<>();
   private final TrackingDebugSynchronizer.PoiSynchronizer poiSynchronizer = new TrackingDebugSynchronizer.PoiSynchronizer();
   private final TrackingDebugSynchronizer.VillageSectionSynchronizer villageSectionSynchronizer = new TrackingDebugSynchronizer.VillageSectionSynchronizer();
   private boolean sleeping = true;
   private Set<DebugSubscription<?>> enabledSubscriptions = Set.of();

   public LevelDebugSynchronizers(ServerLevel p_427157_) {
      this.level = p_427157_;

      for (DebugSubscription<?> debugsubscription : BuiltInRegistries.DEBUG_SUBSCRIPTION) {
         if (debugsubscription.valueStreamCodec() != null) {
            this.sourceSynchronizers.put(debugsubscription, new TrackingDebugSynchronizer.SourceSynchronizer<>(debugsubscription));
         }
      }

      this.allSynchronizers.addAll(this.sourceSynchronizers.values());
      this.allSynchronizers.add(this.poiSynchronizer);
      this.allSynchronizers.add(this.villageSectionSynchronizer);
   }

   public void tick(ServerDebugSubscribers p_422875_) {
      this.enabledSubscriptions = p_422875_.enabledSubscriptions();
      boolean flag = this.enabledSubscriptions.isEmpty();
      if (this.sleeping != flag) {
         this.sleeping = flag;
         if (flag) {
            for (TrackingDebugSynchronizer<?> trackingdebugsynchronizer : this.allSynchronizers) {
               trackingdebugsynchronizer.clear();
            }
         } else {
            this.wakeUp();
         }
      }

      if (!this.sleeping) {
         for (TrackingDebugSynchronizer<?> trackingdebugsynchronizer1 : this.allSynchronizers) {
            trackingdebugsynchronizer1.tick(this.level);
         }
      }
   }

   private void wakeUp() {
      ChunkMap chunkmap = this.level.getChunkSource().chunkMap;
      chunkmap.forEachReadyToSendChunk(this::registerChunk);

      for (Entity entity : this.level.getAllEntities()) {
         if (chunkmap.isTrackedByAnyPlayer(entity)) {
            this.registerEntity(entity);
         }
      }
   }

   <T> TrackingDebugSynchronizer.SourceSynchronizer<T> getSourceSynchronizer(DebugSubscription<T> p_431480_) {
      return (TrackingDebugSynchronizer.SourceSynchronizer<T>)this.sourceSynchronizers.get(p_431480_);
   }

   public void registerChunk(final LevelChunk p_429135_) {
      if (!this.sleeping) {
         p_429135_.registerDebugValues(this.level, new DebugValueSource.Registration() {
            @Override
            public <T> void register(DebugSubscription<T> p_425516_, DebugValueSource.ValueGetter<T> p_431120_) {
               LevelDebugSynchronizers.this.getSourceSynchronizer(p_425516_).registerChunk(p_429135_.getPos(), p_431120_);
            }
         });
         p_429135_.getBlockEntities().values().forEach(this::registerBlockEntity);
      }
   }

   public void dropChunk(ChunkPos p_424400_) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer.SourceSynchronizer<?> sourcesynchronizer : this.sourceSynchronizers.values()) {
            sourcesynchronizer.dropChunk(p_424400_);
         }
      }
   }

   public void registerBlockEntity(final BlockEntity p_427726_) {
      if (!this.sleeping) {
         p_427726_.registerDebugValues(this.level, new DebugValueSource.Registration() {
            @Override
            public <T> void register(DebugSubscription<T> p_426999_, DebugValueSource.ValueGetter<T> p_422782_) {
               LevelDebugSynchronizers.this.getSourceSynchronizer(p_426999_).registerBlockEntity(p_427726_.getBlockPos(), p_422782_);
            }
         });
      }
   }

   public void dropBlockEntity(BlockPos p_426214_) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer.SourceSynchronizer<?> sourcesynchronizer : this.sourceSynchronizers.values()) {
            sourcesynchronizer.dropBlockEntity(this.level, p_426214_);
         }
      }
   }

   public void registerEntity(final Entity p_427845_) {
      if (!this.sleeping) {
         p_427845_.registerDebugValues(this.level, new DebugValueSource.Registration() {
            @Override
            public <T> void register(DebugSubscription<T> p_428326_, DebugValueSource.ValueGetter<T> p_426977_) {
               LevelDebugSynchronizers.this.getSourceSynchronizer(p_428326_).registerEntity(p_427845_.getUUID(), p_426977_);
            }
         });
      }
   }

   public void dropEntity(Entity p_429158_) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer.SourceSynchronizer<?> sourcesynchronizer : this.sourceSynchronizers.values()) {
            sourcesynchronizer.dropEntity(p_429158_);
         }
      }
   }

   public void startTrackingChunk(ServerPlayer p_424823_, ChunkPos p_430934_) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer<?> trackingdebugsynchronizer : this.allSynchronizers) {
            trackingdebugsynchronizer.startTrackingChunk(p_424823_, p_430934_);
         }
      }
   }

   public void startTrackingEntity(ServerPlayer p_428156_, Entity p_430378_) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer<?> trackingdebugsynchronizer : this.allSynchronizers) {
            trackingdebugsynchronizer.startTrackingEntity(p_428156_, p_430378_);
         }
      }
   }

   public void registerPoi(PoiRecord p_425813_) {
      if (!this.sleeping) {
         this.poiSynchronizer.onPoiAdded(this.level, p_425813_);
         this.villageSectionSynchronizer.onPoiAdded(this.level, p_425813_);
      }
   }

   public void updatePoi(BlockPos p_423441_) {
      if (!this.sleeping) {
         this.poiSynchronizer.onPoiTicketCountChanged(this.level, p_423441_);
      }
   }

   public void dropPoi(BlockPos p_422957_) {
      if (!this.sleeping) {
         this.poiSynchronizer.onPoiRemoved(this.level, p_422957_);
         this.villageSectionSynchronizer.onPoiRemoved(this.level, p_422957_);
      }
   }

   public boolean hasAnySubscriberFor(DebugSubscription<?> p_430072_) {
      return this.enabledSubscriptions.contains(p_430072_);
   }

   public <T> void sendBlockValue(BlockPos p_423274_, DebugSubscription<T> p_426435_, T p_431487_) {
      if (this.hasAnySubscriberFor(p_426435_)) {
         this.broadcastToTracking(new ChunkPos(p_423274_), p_426435_, new ClientboundDebugBlockValuePacket(p_423274_, p_426435_.packUpdate(p_431487_)));
      }
   }

   public <T> void clearBlockValue(BlockPos p_424825_, DebugSubscription<T> p_429421_) {
      if (this.hasAnySubscriberFor(p_429421_)) {
         this.broadcastToTracking(new ChunkPos(p_424825_), p_429421_, new ClientboundDebugBlockValuePacket(p_424825_, p_429421_.emptyUpdate()));
      }
   }

   public <T> void sendEntityValue(Entity p_426501_, DebugSubscription<T> p_423915_, T p_430715_) {
      if (this.hasAnySubscriberFor(p_423915_)) {
         this.broadcastToTracking(p_426501_, p_423915_, new ClientboundDebugEntityValuePacket(p_426501_.getId(), p_423915_.packUpdate(p_430715_)));
      }
   }

   public <T> void clearEntityValue(Entity p_427122_, DebugSubscription<T> p_427818_) {
      if (this.hasAnySubscriberFor(p_427818_)) {
         this.broadcastToTracking(p_427122_, p_427818_, new ClientboundDebugEntityValuePacket(p_427122_.getId(), p_427818_.emptyUpdate()));
      }
   }

   public <T> void broadcastEventToTracking(BlockPos p_427757_, DebugSubscription<T> p_430643_, T p_427412_) {
      if (this.hasAnySubscriberFor(p_430643_)) {
         this.broadcastToTracking(new ChunkPos(p_427757_), p_430643_, new ClientboundDebugEventPacket(p_430643_.packEvent(p_427412_)));
      }
   }

   private void broadcastToTracking(ChunkPos p_425512_, DebugSubscription<?> p_427501_, Packet<? super ClientGamePacketListener> p_429328_) {
      ChunkMap chunkmap = this.level.getChunkSource().chunkMap;

      for (ServerPlayer serverplayer : chunkmap.getPlayers(p_425512_, false)) {
         if (serverplayer.debugSubscriptions().contains(p_427501_)) {
            serverplayer.connection.send(p_429328_);
         }
      }
   }

   private void broadcastToTracking(Entity p_423477_, DebugSubscription<?> p_427592_, Packet<? super ClientGamePacketListener> p_428714_) {
      ChunkMap chunkmap = this.level.getChunkSource().chunkMap;
      chunkmap.sendToTrackingPlayersFiltered(p_423477_, p_428714_, p_428009_ -> p_428009_.debugSubscriptions().contains(p_427592_));
   }
}
