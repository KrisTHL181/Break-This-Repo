package net.minecraft.client.multiplayer;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundDebugSubscriptionRequestPacket;
import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.util.debugchart.RemoteDebugSampleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ClientDebugSubscriber {
   private final ClientPacketListener connection;
   private final DebugScreenOverlay debugScreenOverlay;
   private Set<DebugSubscription<?>> remoteSubscriptions = Set.of();
   private final Map<DebugSubscription<?>, ClientDebugSubscriber.ValueMaps<?>> valuesBySubscription = new HashMap<>();

   public ClientDebugSubscriber(ClientPacketListener p_429910_, DebugScreenOverlay p_427111_) {
      this.debugScreenOverlay = p_427111_;
      this.connection = p_429910_;
   }

   private static void addFlag(Set<DebugSubscription<?>> p_427949_, DebugSubscription<?> p_423272_, boolean p_424480_) {
      if (p_424480_) {
         p_427949_.add(p_423272_);
      }
   }

   private Set<DebugSubscription<?>> requestedSubscriptions() {
      Set<DebugSubscription<?>> set = new ReferenceOpenHashSet();
      addFlag(set, RemoteDebugSampleType.TICK_TIME.subscription(), this.debugScreenOverlay.showFpsCharts());
      if (SharedConstants.DEBUG_ENABLED) {
         addFlag(set, DebugSubscriptions.BEES, SharedConstants.DEBUG_BEES);
         addFlag(set, DebugSubscriptions.BEE_HIVES, SharedConstants.DEBUG_BEES);
         addFlag(set, DebugSubscriptions.BRAINS, SharedConstants.DEBUG_BRAIN);
         addFlag(set, DebugSubscriptions.BREEZES, SharedConstants.DEBUG_BREEZE_MOB);
         addFlag(set, DebugSubscriptions.ENTITY_BLOCK_INTERSECTIONS, SharedConstants.DEBUG_ENTITY_BLOCK_INTERSECTION);
         addFlag(set, DebugSubscriptions.ENTITY_PATHS, SharedConstants.DEBUG_PATHFINDING);
         addFlag(set, DebugSubscriptions.GAME_EVENTS, SharedConstants.DEBUG_GAME_EVENT_LISTENERS);
         addFlag(set, DebugSubscriptions.GAME_EVENT_LISTENERS, SharedConstants.DEBUG_GAME_EVENT_LISTENERS);
         addFlag(set, DebugSubscriptions.GOAL_SELECTORS, SharedConstants.DEBUG_GOAL_SELECTOR || SharedConstants.DEBUG_BEES);
         addFlag(set, DebugSubscriptions.NEIGHBOR_UPDATES, SharedConstants.DEBUG_NEIGHBORSUPDATE);
         addFlag(set, DebugSubscriptions.POIS, SharedConstants.DEBUG_POI);
         addFlag(set, DebugSubscriptions.RAIDS, SharedConstants.DEBUG_RAIDS);
         addFlag(set, DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, SharedConstants.DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER);
         addFlag(set, DebugSubscriptions.STRUCTURES, SharedConstants.DEBUG_STRUCTURES);
         addFlag(set, DebugSubscriptions.VILLAGE_SECTIONS, SharedConstants.DEBUG_VILLAGE_SECTIONS);
      }

      return set;
   }

   public void clear() {
      this.remoteSubscriptions = Set.of();
      this.dropLevel();
   }

   public void tick(long p_429002_) {
      Set<DebugSubscription<?>> set = this.requestedSubscriptions();
      if (!set.equals(this.remoteSubscriptions)) {
         this.remoteSubscriptions = set;
         this.onSubscriptionsChanged(set);
      }

      this.valuesBySubscription.forEach((p_425497_, p_430634_) -> {
         if (p_425497_.expireAfterTicks() != 0) {
            p_430634_.purgeExpired(p_429002_);
         }
      });
   }

   private void onSubscriptionsChanged(Set<DebugSubscription<?>> p_427165_) {
      this.valuesBySubscription.keySet().retainAll(p_427165_);
      this.initializeSubscriptions(p_427165_);
      this.connection.send(new ServerboundDebugSubscriptionRequestPacket(p_427165_));
   }

   private void initializeSubscriptions(Set<DebugSubscription<?>> p_453966_) {
      for (DebugSubscription<?> debugsubscription : p_453966_) {
         this.valuesBySubscription.computeIfAbsent(debugsubscription, p_423978_ -> new ClientDebugSubscriber.ValueMaps());
      }
   }

   <V> ClientDebugSubscriber.@Nullable ValueMaps<V> getValueMaps(DebugSubscription<V> p_422955_) {
      return (ClientDebugSubscriber.ValueMaps<V>)this.valuesBySubscription.get(p_422955_);
   }

   private <K, V> ClientDebugSubscriber.@Nullable ValueMap<K, V> getValueMap(DebugSubscription<V> p_424377_, ClientDebugSubscriber.ValueMapType<K, V> p_423384_) {
      ClientDebugSubscriber.ValueMaps<V> valuemaps = this.getValueMaps(p_424377_);
      return valuemaps != null ? p_423384_.get(valuemaps) : null;
   }

   <K, V> @Nullable V getValue(DebugSubscription<V> p_426326_, K p_423774_, ClientDebugSubscriber.ValueMapType<K, V> p_422304_) {
      ClientDebugSubscriber.ValueMap<K, V> valuemap = this.getValueMap(p_426326_, p_422304_);
      return valuemap != null ? valuemap.getValue(p_423774_) : null;
   }

   public DebugValueAccess createDebugValueAccess(final Level p_430673_) {
      return new DebugValueAccess() {
         @Override
         public <T> void forEachChunk(DebugSubscription<T> p_426040_, BiConsumer<ChunkPos, T> p_431635_) {
            ClientDebugSubscriber.this.forEachValue(p_426040_, ClientDebugSubscriber.chunks(), p_431635_);
         }

         @Override
         public <T> @Nullable T getChunkValue(DebugSubscription<T> p_427123_, ChunkPos p_430839_) {
            return ClientDebugSubscriber.this.getValue(p_427123_, p_430839_, ClientDebugSubscriber.chunks());
         }

         @Override
         public <T> void forEachBlock(DebugSubscription<T> p_430515_, BiConsumer<BlockPos, T> p_429609_) {
            ClientDebugSubscriber.this.forEachValue(p_430515_, ClientDebugSubscriber.blocks(), p_429609_);
         }

         @Override
         public <T> @Nullable T getBlockValue(DebugSubscription<T> p_424544_, BlockPos p_430316_) {
            return ClientDebugSubscriber.this.getValue(p_424544_, p_430316_, ClientDebugSubscriber.blocks());
         }

         @Override
         public <T> void forEachEntity(DebugSubscription<T> p_430386_, BiConsumer<Entity, T> p_423769_) {
            ClientDebugSubscriber.this.forEachValue(p_430386_, ClientDebugSubscriber.entities(), (p_424625_, p_430947_) -> {
               Entity entity = p_430673_.getEntity(p_424625_);
               if (entity != null) {
                  p_423769_.accept(entity, p_430947_);
               }
            });
         }

         @Override
         public <T> @Nullable T getEntityValue(DebugSubscription<T> p_428389_, Entity p_427831_) {
            return ClientDebugSubscriber.this.getValue(p_428389_, p_427831_.getUUID(), ClientDebugSubscriber.entities());
         }

         @Override
         public <T> void forEachEvent(DebugSubscription<T> p_429127_, DebugValueAccess.EventVisitor<T> p_425978_) {
            ClientDebugSubscriber.ValueMaps<T> valuemaps = ClientDebugSubscriber.this.getValueMaps(p_429127_);
            if (valuemaps != null) {
               long i = p_430673_.getGameTime();

               for (ClientDebugSubscriber.ValueWrapper<T> valuewrapper : valuemaps.events) {
                  int j = (int)(valuewrapper.expiresAfterTime() - i);
                  int k = p_429127_.expireAfterTicks();
                  p_425978_.accept(valuewrapper.value(), j, k);
               }
            }
         }
      };
   }

   public <T> void updateChunk(long p_431237_, ChunkPos p_427283_, DebugSubscription.Update<T> p_427659_) {
      this.updateMap(p_431237_, p_427283_, p_427659_, chunks());
   }

   public <T> void updateBlock(long p_431330_, BlockPos p_425482_, DebugSubscription.Update<T> p_427405_) {
      this.updateMap(p_431330_, p_425482_, p_427405_, blocks());
   }

   public <T> void updateEntity(long p_426010_, Entity p_428523_, DebugSubscription.Update<T> p_430824_) {
      this.updateMap(p_426010_, p_428523_.getUUID(), p_430824_, entities());
   }

   public <T> void pushEvent(long p_425472_, DebugSubscription.Event<T> p_429061_) {
      ClientDebugSubscriber.ValueMaps<T> valuemaps = this.getValueMaps(p_429061_.subscription());
      if (valuemaps != null) {
         valuemaps.events.add(new ClientDebugSubscriber.ValueWrapper<>(p_429061_.value(), p_425472_ + p_429061_.subscription().expireAfterTicks()));
      }
   }

   private <K, V> void updateMap(long p_429534_, K p_422751_, DebugSubscription.Update<V> p_423126_, ClientDebugSubscriber.ValueMapType<K, V> p_426250_) {
      ClientDebugSubscriber.ValueMap<K, V> valuemap = this.getValueMap(p_423126_.subscription(), p_426250_);
      if (valuemap != null) {
         valuemap.apply(p_429534_, p_422751_, p_423126_);
      }
   }

   <K, V> void forEachValue(DebugSubscription<V> p_425648_, ClientDebugSubscriber.ValueMapType<K, V> p_424349_, BiConsumer<K, V> p_430545_) {
      ClientDebugSubscriber.ValueMap<K, V> valuemap = this.getValueMap(p_425648_, p_424349_);
      if (valuemap != null) {
         valuemap.forEach(p_430545_);
      }
   }

   public void dropLevel() {
      this.valuesBySubscription.clear();
      this.initializeSubscriptions(this.remoteSubscriptions);
   }

   public void dropChunk(ChunkPos p_427632_) {
      if (!this.valuesBySubscription.isEmpty()) {
         for (ClientDebugSubscriber.ValueMaps<?> valuemaps : this.valuesBySubscription.values()) {
            valuemaps.dropChunkAndBlocks(p_427632_);
         }
      }
   }

   public void dropEntity(Entity p_425237_) {
      if (!this.valuesBySubscription.isEmpty()) {
         for (ClientDebugSubscriber.ValueMaps<?> valuemaps : this.valuesBySubscription.values()) {
            valuemaps.entityValues.removeKey(p_425237_.getUUID());
         }
      }
   }

   static <T> ClientDebugSubscriber.ValueMapType<UUID, T> entities() {
      return p_428289_ -> p_428289_.entityValues;
   }

   static <T> ClientDebugSubscriber.ValueMapType<BlockPos, T> blocks() {
      return p_430691_ -> p_430691_.blockValues;
   }

   static <T> ClientDebugSubscriber.ValueMapType<ChunkPos, T> chunks() {
      return p_424690_ -> p_424690_.chunkValues;
   }

   @OnlyIn(Dist.CLIENT)
   static class ValueMap<K, V> {
      private final Map<K, ClientDebugSubscriber.ValueWrapper<V>> values = new HashMap<>();

      public void removeValues(Predicate<ClientDebugSubscriber.ValueWrapper<V>> p_430207_) {
         this.values.values().removeIf(p_430207_);
      }

      public void removeKey(K p_423626_) {
         this.values.remove(p_423626_);
      }

      public void removeKeys(Predicate<K> p_428607_) {
         this.values.keySet().removeIf(p_428607_);
      }

      public @Nullable V getValue(K p_426717_) {
         ClientDebugSubscriber.ValueWrapper<V> valuewrapper = this.values.get(p_426717_);
         return valuewrapper != null ? valuewrapper.value() : null;
      }

      public void apply(long p_431418_, K p_429026_, DebugSubscription.Update<V> p_430377_) {
         if (p_430377_.value().isPresent()) {
            this.values
               .put(p_429026_, new ClientDebugSubscriber.ValueWrapper<>(p_430377_.value().get(), p_431418_ + p_430377_.subscription().expireAfterTicks()));
         } else {
            this.values.remove(p_429026_);
         }
      }

      public void forEach(BiConsumer<K, V> p_426708_) {
         this.values.forEach((p_424485_, p_423257_) -> p_426708_.accept((K)p_424485_, p_423257_.value()));
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface ValueMapType<K, V> {
      ClientDebugSubscriber.ValueMap<K, V> get(ClientDebugSubscriber.ValueMaps<V> var1);
   }

   @OnlyIn(Dist.CLIENT)
   static class ValueMaps<V> {
      final ClientDebugSubscriber.ValueMap<ChunkPos, V> chunkValues = new ClientDebugSubscriber.ValueMap<>();
      final ClientDebugSubscriber.ValueMap<BlockPos, V> blockValues = new ClientDebugSubscriber.ValueMap<>();
      final ClientDebugSubscriber.ValueMap<UUID, V> entityValues = new ClientDebugSubscriber.ValueMap<>();
      final List<ClientDebugSubscriber.ValueWrapper<V>> events = new ArrayList<>();

      public void purgeExpired(long p_427670_) {
         Predicate<ClientDebugSubscriber.ValueWrapper<V>> predicate = p_427128_ -> p_427128_.hasExpired(p_427670_);
         this.chunkValues.removeValues(predicate);
         this.blockValues.removeValues(predicate);
         this.entityValues.removeValues(predicate);
         this.events.removeIf(predicate);
      }

      public void dropChunkAndBlocks(ChunkPos p_425725_) {
         this.chunkValues.removeKey(p_425725_);
         this.blockValues.removeKeys(p_425725_::contains);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record ValueWrapper<T>(T value, long expiresAfterTime) {
      private static final long NO_EXPIRY = -1L;

      public boolean hasExpired(long p_426221_) {
         return this.expiresAfterTime == -1L ? false : p_426221_ >= this.expiresAfterTime;
      }
   }
}
