package net.minecraft.stats;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.entity.player.Player;

public class StatsCounter {
   protected final Object2IntMap<Stat<?>> stats = Object2IntMaps.synchronize(new Object2IntOpenHashMap());

   public StatsCounter() {
      this.stats.defaultReturnValue(0);
   }

   public void increment(final Player player, final Stat<?> stat, final int count) {
      int result = (int)Math.min((long)this.getValue(stat) + count, 2147483647L);
      this.setValue(player, stat, result);
   }

   public void setValue(final Player player, final Stat<?> stat, final int count) {
      this.stats.put(stat, count);
   }

   public <T> int getValue(final StatType<T> type, final T key) {
      return type.contains(key) ? this.getValue(type.get(key)) : 0;
   }

   public int getValue(final Stat<?> stat) {
      return this.stats.getInt(stat);
   }
}
