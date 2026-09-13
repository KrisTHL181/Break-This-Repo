package net.minecraft.util.debug;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundDebugBlockValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugChunkValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEntityValuePacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public abstract class TrackingDebugSynchronizer<T> {
   protected final DebugSubscription<T> subscription;
   private final Set<UUID> subscribedPlayers = new ObjectOpenHashSet();

   public TrackingDebugSynchronizer(DebugSubscription<T> p_429280_) {
      this.subscription = p_429280_;
   }

   public final void tick(ServerLevel p_426854_) {
      for (ServerPlayer serverplayer : p_426854_.players()) {
         boolean flag = this.subscribedPlayers.contains(serverplayer.getUUID());
         boolean flag1 = serverplayer.debugSubscriptions().contains(this.subscription);
         if (flag1 != flag) {
            if (flag1) {
               this.addSubscriber(serverplayer);
            } else {
               this.subscribedPlayers.remove(serverplayer.getUUID());
            }
         }
      }

      this.subscribedPlayers.removeIf(p_430677_ -> p_426854_.getPlayerByUUID(p_430677_) == null);
      if (!this.subscribedPlayers.isEmpty()) {
         this.pollAndSendUpdates(p_426854_);
      }
   }

   private void addSubscriber(ServerPlayer p_431465_) {
      this.subscribedPlayers.add(p_431465_.getUUID());
      p_431465_.getChunkTrackingView().forEach(p_426197_ -> {
         if (!p_431465_.connection.chunkSender.isPending(p_426197_.toLong())) {
            this.startTrackingChunk(p_431465_, p_426197_);
         }
      });
      p_431465_.level().getChunkSource().chunkMap.forEachEntityTrackedBy(p_431465_, p_424938_ -> this.startTrackingEntity(p_431465_, p_424938_));
   }

   protected final void sendToPlayersTrackingChunk(ServerLevel p_424461_, ChunkPos p_425780_, Packet<? super ClientGamePacketListener> p_428807_) {
      ChunkMap chunkmap = p_424461_.getChunkSource().chunkMap;

      for (UUID uuid : this.subscribedPlayers) {
         if (p_424461_.getPlayerByUUID(uuid) instanceof ServerPlayer serverplayer && chunkmap.isChunkTracked(serverplayer, p_425780_.x, p_425780_.z)) {
            serverplayer.connection.send(p_428807_);
         }
      }
   }

   protected final void sendToPlayersTrackingEntity(ServerLevel p_431135_, Entity p_430774_, Packet<? super ClientGamePacketListener> p_430295_) {
      ChunkMap chunkmap = p_431135_.getChunkSource().chunkMap;
      chunkmap.sendToTrackingPlayersFiltered(p_430774_, p_430295_, p_449343_ -> this.subscribedPlayers.contains(p_449343_.getUUID()));
   }

   public final void startTrackingChunk(ServerPlayer p_428817_, ChunkPos p_427859_) {
      if (this.subscribedPlayers.contains(p_428817_.getUUID())) {
         this.sendInitialChunk(p_428817_, p_427859_);
      }
   }

   public final void startTrackingEntity(ServerPlayer p_426837_, Entity p_431735_) {
      if (this.subscribedPlayers.contains(p_426837_.getUUID())) {
         this.sendInitialEntity(p_426837_, p_431735_);
      }
   }

   protected void clear() {
   }

   protected void pollAndSendUpdates(ServerLevel p_430456_) {
   }

   protected void sendInitialChunk(ServerPlayer p_425468_, ChunkPos p_429823_) {
   }

   protected void sendInitialEntity(ServerPlayer p_429729_, Entity p_424158_) {
   }

   public static class PoiSynchronizer extends TrackingDebugSynchronizer<DebugPoiInfo> {
      public PoiSynchronizer() {
         super(DebugSubscriptions.POIS);
      }

      @Override
      protected void sendInitialChunk(ServerPlayer p_429056_, ChunkPos p_426730_) {
         ServerLevel serverlevel = p_429056_.level();
         PoiManager poimanager = serverlevel.getPoiManager();
         poimanager.getInChunk(p_428459_ -> true, p_426730_, PoiManager.Occupancy.ANY)
            .forEach(
               p_430044_ -> p_429056_.connection
                  .send(new ClientboundDebugBlockValuePacket(p_430044_.getPos(), this.subscription.packUpdate(new DebugPoiInfo(p_430044_))))
            );
      }

      public void onPoiAdded(ServerLevel p_431549_, PoiRecord p_430114_) {
         this.sendToPlayersTrackingChunk(
            p_431549_,
            new ChunkPos(p_430114_.getPos()),
            new ClientboundDebugBlockValuePacket(p_430114_.getPos(), this.subscription.packUpdate(new DebugPoiInfo(p_430114_)))
         );
      }

      public void onPoiRemoved(ServerLevel p_425928_, BlockPos p_425255_) {
         this.sendToPlayersTrackingChunk(p_425928_, new ChunkPos(p_425255_), new ClientboundDebugBlockValuePacket(p_425255_, this.subscription.emptyUpdate()));
      }

      public void onPoiTicketCountChanged(ServerLevel p_423894_, BlockPos p_429450_) {
         this.sendToPlayersTrackingChunk(
            p_423894_,
            new ChunkPos(p_429450_),
            new ClientboundDebugBlockValuePacket(p_429450_, this.subscription.packUpdate(p_423894_.getPoiManager().getDebugPoiInfo(p_429450_)))
         );
      }
   }

   public static class SourceSynchronizer<T> extends TrackingDebugSynchronizer<T> {
      private final Map<ChunkPos, TrackingDebugSynchronizer.ValueSource<T>> chunkSources = new HashMap<>();
      private final Map<BlockPos, TrackingDebugSynchronizer.ValueSource<T>> blockEntitySources = new HashMap<>();
      private final Map<UUID, TrackingDebugSynchronizer.ValueSource<T>> entitySources = new HashMap<>();

      public SourceSynchronizer(DebugSubscription<T> p_424262_) {
         super(p_424262_);
      }

      @Override
      protected void clear() {
         this.chunkSources.clear();
         this.blockEntitySources.clear();
         this.entitySources.clear();
      }

      @Override
      protected void pollAndSendUpdates(ServerLevel p_425636_) {
         for (Entry<ChunkPos, TrackingDebugSynchronizer.ValueSource<T>> entry : this.chunkSources.entrySet()) {
            DebugSubscription.Update<T> update = entry.getValue().pollUpdate(this.subscription);
            if (update != null) {
               ChunkPos chunkpos = entry.getKey();
               this.sendToPlayersTrackingChunk(p_425636_, chunkpos, new ClientboundDebugChunkValuePacket(chunkpos, update));
            }
         }

         for (Entry<BlockPos, TrackingDebugSynchronizer.ValueSource<T>> entry1 : this.blockEntitySources.entrySet()) {
            DebugSubscription.Update<T> update1 = entry1.getValue().pollUpdate(this.subscription);
            if (update1 != null) {
               BlockPos blockpos = entry1.getKey();
               ChunkPos chunkpos1 = new ChunkPos(blockpos);
               this.sendToPlayersTrackingChunk(p_425636_, chunkpos1, new ClientboundDebugBlockValuePacket(blockpos, update1));
            }
         }

         for (Entry<UUID, TrackingDebugSynchronizer.ValueSource<T>> entry2 : this.entitySources.entrySet()) {
            DebugSubscription.Update<T> update2 = entry2.getValue().pollUpdate(this.subscription);
            if (update2 != null) {
               Entity entity = Objects.requireNonNull(p_425636_.getEntity(entry2.getKey()));
               this.sendToPlayersTrackingEntity(p_425636_, entity, new ClientboundDebugEntityValuePacket(entity.getId(), update2));
            }
         }
      }

      public void registerChunk(ChunkPos p_426364_, DebugValueSource.ValueGetter<T> p_428217_) {
         this.chunkSources.put(p_426364_, new TrackingDebugSynchronizer.ValueSource<>(p_428217_));
      }

      public void registerBlockEntity(BlockPos p_422457_, DebugValueSource.ValueGetter<T> p_423872_) {
         this.blockEntitySources.put(p_422457_, new TrackingDebugSynchronizer.ValueSource<>(p_423872_));
      }

      public void registerEntity(UUID p_430345_, DebugValueSource.ValueGetter<T> p_429935_) {
         this.entitySources.put(p_430345_, new TrackingDebugSynchronizer.ValueSource<>(p_429935_));
      }

      public void dropChunk(ChunkPos p_427149_) {
         this.chunkSources.remove(p_427149_);
         this.blockEntitySources.keySet().removeIf(p_427149_::contains);
      }

      public void dropBlockEntity(ServerLevel p_424451_, BlockPos p_430802_) {
         TrackingDebugSynchronizer.ValueSource<T> valuesource = this.blockEntitySources.remove(p_430802_);
         if (valuesource != null) {
            ChunkPos chunkpos = new ChunkPos(p_430802_);
            this.sendToPlayersTrackingChunk(p_424451_, chunkpos, new ClientboundDebugBlockValuePacket(p_430802_, this.subscription.emptyUpdate()));
         }
      }

      public void dropEntity(Entity p_425062_) {
         this.entitySources.remove(p_425062_.getUUID());
      }

      @Override
      protected void sendInitialChunk(ServerPlayer p_430243_, ChunkPos p_430779_) {
         TrackingDebugSynchronizer.ValueSource<T> valuesource = this.chunkSources.get(p_430779_);
         if (valuesource != null && valuesource.lastSyncedValue != null) {
            p_430243_.connection.send(new ClientboundDebugChunkValuePacket(p_430779_, this.subscription.packUpdate(valuesource.lastSyncedValue)));
         }

         for (Entry<BlockPos, TrackingDebugSynchronizer.ValueSource<T>> entry : this.blockEntitySources.entrySet()) {
            T t = entry.getValue().lastSyncedValue;
            if (t != null) {
               BlockPos blockpos = entry.getKey();
               if (p_430779_.contains(blockpos)) {
                  p_430243_.connection.send(new ClientboundDebugBlockValuePacket(blockpos, this.subscription.packUpdate(t)));
               }
            }
         }
      }

      @Override
      protected void sendInitialEntity(ServerPlayer p_429064_, Entity p_424750_) {
         TrackingDebugSynchronizer.ValueSource<T> valuesource = this.entitySources.get(p_424750_.getUUID());
         if (valuesource != null && valuesource.lastSyncedValue != null) {
            p_429064_.connection.send(new ClientboundDebugEntityValuePacket(p_424750_.getId(), this.subscription.packUpdate(valuesource.lastSyncedValue)));
         }
      }
   }

   static class ValueSource<T> {
      private final DebugValueSource.ValueGetter<T> getter;
      @Nullable T lastSyncedValue;

      ValueSource(DebugValueSource.ValueGetter<T> p_422624_) {
         this.getter = p_422624_;
      }

      public DebugSubscription.@Nullable Update<T> pollUpdate(DebugSubscription<T> p_422787_) {
         T t = this.getter.get();
         if (!Objects.equals(t, this.lastSyncedValue)) {
            this.lastSyncedValue = t;
            return p_422787_.packUpdate(t);
         } else {
            return null;
         }
      }
   }

   public static class VillageSectionSynchronizer extends TrackingDebugSynchronizer<Unit> {
      public VillageSectionSynchronizer() {
         super(DebugSubscriptions.VILLAGE_SECTIONS);
      }

      @Override
      protected void sendInitialChunk(ServerPlayer p_428047_, ChunkPos p_426053_) {
         ServerLevel serverlevel = p_428047_.level();
         PoiManager poimanager = serverlevel.getPoiManager();
         poimanager.getInChunk(p_427505_ -> true, p_426053_, PoiManager.Occupancy.ANY).forEach(p_425719_ -> {
            SectionPos sectionpos = SectionPos.of(p_425719_.getPos());
            forEachVillageSectionUpdate(serverlevel, sectionpos, (p_430328_, p_428803_) -> {
               BlockPos blockpos = p_430328_.center();
               p_428047_.connection.send(new ClientboundDebugBlockValuePacket(blockpos, this.subscription.packUpdate(p_428803_ ? Unit.INSTANCE : null)));
            });
         });
      }

      public void onPoiAdded(ServerLevel p_426131_, PoiRecord p_427603_) {
         this.sendVillageSectionsPacket(p_426131_, p_427603_.getPos());
      }

      public void onPoiRemoved(ServerLevel p_426839_, BlockPos p_431563_) {
         this.sendVillageSectionsPacket(p_426839_, p_431563_);
      }

      private void sendVillageSectionsPacket(ServerLevel p_429694_, BlockPos p_423747_) {
         forEachVillageSectionUpdate(
            p_429694_,
            SectionPos.of(p_423747_),
            (p_430550_, p_431379_) -> {
               BlockPos blockpos = p_430550_.center();
               if (p_431379_) {
                  this.sendToPlayersTrackingChunk(
                     p_429694_, new ChunkPos(blockpos), new ClientboundDebugBlockValuePacket(blockpos, this.subscription.packUpdate(Unit.INSTANCE))
                  );
               } else {
                  this.sendToPlayersTrackingChunk(
                     p_429694_, new ChunkPos(blockpos), new ClientboundDebugBlockValuePacket(blockpos, this.subscription.emptyUpdate())
                  );
               }
            }
         );
      }

      private static void forEachVillageSectionUpdate(ServerLevel p_429439_, SectionPos p_426277_, BiConsumer<SectionPos, Boolean> p_429599_) {
         for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
               for (int k = -1; k <= 1; k++) {
                  SectionPos sectionpos = p_426277_.offset(j, k, i);
                  if (p_429439_.isVillage(sectionpos.center())) {
                     p_429599_.accept(sectionpos, true);
                  } else {
                     p_429599_.accept(sectionpos, false);
                  }
               }
            }
         }
      }
   }
}
