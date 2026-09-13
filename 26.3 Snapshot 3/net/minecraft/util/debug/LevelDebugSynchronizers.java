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

   public LevelDebugSynchronizers(final ServerLevel level) {
      this.level = level;

      for (DebugSubscription<?> subscription : BuiltInRegistries.DEBUG_SUBSCRIPTION) {
         if (subscription.valueStreamCodec() != null) {
            this.sourceSynchronizers.put(subscription, new TrackingDebugSynchronizer.SourceSynchronizer<>(subscription));
         }
      }

      this.allSynchronizers.addAll(this.sourceSynchronizers.values());
      this.allSynchronizers.add(this.poiSynchronizer);
      this.allSynchronizers.add(this.villageSectionSynchronizer);
   }

   public void tick(final ServerDebugSubscribers serverSubscribers) {
      this.enabledSubscriptions = serverSubscribers.enabledSubscriptions();
      boolean shouldSleep = this.enabledSubscriptions.isEmpty();
      if (this.sleeping != shouldSleep) {
         this.sleeping = shouldSleep;
         if (shouldSleep) {
            for (TrackingDebugSynchronizer<?> synchronizer : this.allSynchronizers) {
               synchronizer.clear();
            }
         } else {
            this.wakeUp();
         }
      }

      if (!this.sleeping) {
         for (TrackingDebugSynchronizer<?> synchronizer : this.allSynchronizers) {
            synchronizer.tick(this.level);
         }
      }
   }

   private void wakeUp() {
      ChunkMap chunkMap = this.level.getChunkSource().chunkMap;
      chunkMap.forEachReadyToSendChunk(this::registerChunk);

      for (Entity entity : this.level.getAllEntities()) {
         if (chunkMap.isTrackedByAnyPlayer(entity)) {
            this.registerEntity(entity);
         }
      }
   }

   private <T> TrackingDebugSynchronizer.SourceSynchronizer<T> getSourceSynchronizer(final DebugSubscription<T> subscription) {
      return (TrackingDebugSynchronizer.SourceSynchronizer<T>)this.sourceSynchronizers.get(subscription);
   }

   public void registerChunk(final LevelChunk chunk) {
      if (!this.sleeping) {
         chunk.registerDebugValues(this.level, new DebugValueSource.Registration() {
            @Override
            public <T> void register(final DebugSubscription<T> subscription, final DebugValueSource.ValueGetter<T> getter) {
               LevelDebugSynchronizers.this.getSourceSynchronizer(subscription).registerChunk(chunk.getPos(), getter);
            }
         });
         chunk.getBlockEntities().values().forEach(this::registerBlockEntity);
      }
   }

   public void dropChunk(final ChunkPos chunkPos) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer.SourceSynchronizer<?> synchronizer : this.sourceSynchronizers.values()) {
            synchronizer.dropChunk(chunkPos);
         }
      }
   }

   public void registerBlockEntity(final BlockEntity blockEntity) {
      if (!this.sleeping) {
         blockEntity.registerDebugValues(this.level, new DebugValueSource.Registration() {
            @Override
            public <T> void register(final DebugSubscription<T> subscription, final DebugValueSource.ValueGetter<T> getter) {
               LevelDebugSynchronizers.this.getSourceSynchronizer(subscription).registerBlockEntity(blockEntity.getBlockPos(), getter);
            }
         });
      }
   }

   public void dropBlockEntity(final BlockPos blockPos) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer.SourceSynchronizer<?> synchronizer : this.sourceSynchronizers.values()) {
            synchronizer.dropBlockEntity(this.level, blockPos);
         }
      }
   }

   public void registerEntity(final Entity entity) {
      if (!this.sleeping) {
         entity.registerDebugValues(this.level, new DebugValueSource.Registration() {
            @Override
            public <T> void register(final DebugSubscription<T> subscription, final DebugValueSource.ValueGetter<T> getter) {
               LevelDebugSynchronizers.this.getSourceSynchronizer(subscription).registerEntity(entity.getUUID(), getter);
            }
         });
      }
   }

   public void dropEntity(final Entity entity) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer.SourceSynchronizer<?> synchronizer : this.sourceSynchronizers.values()) {
            synchronizer.dropEntity(entity);
         }
      }
   }

   public void startTrackingChunk(final ServerPlayer player, final ChunkPos chunkPos) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer<?> synchronizer : this.allSynchronizers) {
            synchronizer.startTrackingChunk(player, chunkPos);
         }
      }
   }

   public void startTrackingEntity(final ServerPlayer player, final Entity entity) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer<?> synchronizer : this.allSynchronizers) {
            synchronizer.startTrackingEntity(player, entity);
         }
      }
   }

   public void registerPoi(final PoiRecord poi) {
      if (!this.sleeping) {
         this.poiSynchronizer.onPoiAdded(this.level, poi);
         this.villageSectionSynchronizer.onPoiAdded(this.level, poi);
      }
   }

   public void updatePoi(final BlockPos pos) {
      if (!this.sleeping) {
         this.poiSynchronizer.onPoiTicketCountChanged(this.level, pos);
      }
   }

   public void dropPoi(final BlockPos pos) {
      if (!this.sleeping) {
         this.poiSynchronizer.onPoiRemoved(this.level, pos);
         this.villageSectionSynchronizer.onPoiRemoved(this.level, pos);
      }
   }

   public boolean hasAnySubscriberFor(final DebugSubscription<?> subscription) {
      return this.enabledSubscriptions.contains(subscription);
   }

   public <T> void sendBlockValue(final BlockPos blockPos, final DebugSubscription<T> subscription, final T value) {
      if (this.hasAnySubscriberFor(subscription)) {
         this.broadcastToTracking(ChunkPos.containing(blockPos), subscription, new ClientboundDebugBlockValuePacket(blockPos, subscription.packUpdate(value)));
      }
   }

   public <T> void clearBlockValue(final BlockPos blockPos, final DebugSubscription<T> subscription) {
      if (this.hasAnySubscriberFor(subscription)) {
         this.broadcastToTracking(ChunkPos.containing(blockPos), subscription, new ClientboundDebugBlockValuePacket(blockPos, subscription.emptyUpdate()));
      }
   }

   public <T> void sendEntityValue(final Entity entity, final DebugSubscription<T> subscription, final T value) {
      if (this.hasAnySubscriberFor(subscription)) {
         this.broadcastToTracking(entity, subscription, new ClientboundDebugEntityValuePacket(entity.getId(), subscription.packUpdate(value)));
      }
   }

   public <T> void clearEntityValue(final Entity entity, final DebugSubscription<T> subscription) {
      if (this.hasAnySubscriberFor(subscription)) {
         this.broadcastToTracking(entity, subscription, new ClientboundDebugEntityValuePacket(entity.getId(), subscription.emptyUpdate()));
      }
   }

   public <T> void broadcastEventToTracking(final BlockPos blockPos, final DebugSubscription<T> subscription, final T value) {
      if (this.hasAnySubscriberFor(subscription)) {
         this.broadcastToTracking(ChunkPos.containing(blockPos), subscription, new ClientboundDebugEventPacket(subscription.packEvent(value)));
      }
   }

   private void broadcastToTracking(final ChunkPos trackedChunk, final DebugSubscription<?> subscription, final Packet<? super ClientGamePacketListener> packet) {
      ChunkMap chunkMap = this.level.getChunkSource().chunkMap;

      for (ServerPlayer player : chunkMap.getPlayers(trackedChunk, false)) {
         if (player.debugSubscriptions().contains(subscription)) {
            player.connection.send(packet);
         }
      }
   }

   private void broadcastToTracking(final Entity trackedEntity, final DebugSubscription<?> subscription, final Packet<? super ClientGamePacketListener> packet) {
      ChunkMap chunkMap = this.level.getChunkSource().chunkMap;
      chunkMap.sendToTrackingPlayersFiltered(trackedEntity, packet, player -> player.debugSubscriptions().contains(subscription));
   }
}
