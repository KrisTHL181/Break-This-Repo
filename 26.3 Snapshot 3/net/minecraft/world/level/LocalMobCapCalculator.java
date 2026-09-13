package net.minecraft.world.level;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;

public class LocalMobCapCalculator {
   private final Long2ObjectMap<List<ServerPlayer>> playersNearChunk = new Long2ObjectOpenHashMap();
   private final Map<ServerPlayer, LocalMobCapCalculator.MobCounts> playerMobCounts = Maps.newHashMap();
   private final ChunkMap chunkMap;

   public LocalMobCapCalculator(final ChunkMap chunkMap) {
      this.chunkMap = chunkMap;
   }

   private List<ServerPlayer> getPlayersNear(final ChunkPos pos) {
      return (List<ServerPlayer>)this.playersNearChunk.computeIfAbsent(pos.pack(), key -> this.chunkMap.getPlayersCloseForSpawning(pos));
   }

   public void addMob(final ChunkPos pos, final MobCategory category) {
      for (ServerPlayer player : this.getPlayersNear(pos)) {
         this.playerMobCounts.computeIfAbsent(player, key -> new LocalMobCapCalculator.MobCounts()).add(category);
      }
   }

   public boolean canSpawn(final MobCategory mobCategory, final ChunkPos pos) {
      for (ServerPlayer serverPlayer : this.getPlayersNear(pos)) {
         LocalMobCapCalculator.MobCounts mobCounts = this.playerMobCounts.get(serverPlayer);
         if (mobCounts == null || mobCounts.canSpawn(mobCategory)) {
            return true;
         }
      }

      return false;
   }

   private static class MobCounts {
      private final Object2IntMap<MobCategory> counts = new Object2IntOpenHashMap(MobCategory.values().length);

      public void add(final MobCategory category) {
         this.counts.computeInt(category, (k, count) -> count == null ? 1 : count + 1);
      }

      public boolean canSpawn(final MobCategory category) {
         return this.counts.getOrDefault(category, 0) < category.getMaxInstancesPerChunk();
      }
   }
}
