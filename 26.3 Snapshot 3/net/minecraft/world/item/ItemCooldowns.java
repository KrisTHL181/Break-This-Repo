package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.component.UseCooldown;

public class ItemCooldowns {
   private final Map<Identifier, ItemCooldowns.CooldownInstance> cooldowns = Maps.newHashMap();
   private int tickCount;

   public boolean isOnCooldown(final ItemStack item) {
      return this.getCooldownPercent(item, 0.0F) > 0.0F;
   }

   public float getCooldownPercent(final ItemStack item, final float a) {
      Identifier group = this.getCooldownGroup(item);
      ItemCooldowns.CooldownInstance cooldown = this.cooldowns.get(group);
      if (cooldown != null) {
         float duration = cooldown.endTime - cooldown.startTime;
         float remaining = cooldown.endTime - (this.tickCount + a);
         return Mth.clamp(remaining / duration, 0.0F, 1.0F);
      } else {
         return 0.0F;
      }
   }

   public void tick() {
      this.tickCount++;
      if (!this.cooldowns.isEmpty()) {
         Iterator<Entry<Identifier, ItemCooldowns.CooldownInstance>> iterator = this.cooldowns.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<Identifier, ItemCooldowns.CooldownInstance> entry = iterator.next();
            if (entry.getValue().endTime <= this.tickCount) {
               iterator.remove();
               this.onCooldownEnded(entry.getKey());
            }
         }
      }
   }

   public Identifier getCooldownGroup(final ItemStack item) {
      UseCooldown useCooldown = item.get(DataComponents.USE_COOLDOWN);
      Identifier defaultItemGroup = BuiltInRegistries.ITEM.getKey(item.getItem());
      return useCooldown == null ? defaultItemGroup : useCooldown.cooldownGroup().orElse(defaultItemGroup);
   }

   public void addCooldown(final ItemStack item, final int time) {
      this.addCooldown(this.getCooldownGroup(item), time);
   }

   public void addCooldown(final Identifier cooldownGroup, final int time) {
      this.cooldowns.put(cooldownGroup, new ItemCooldowns.CooldownInstance(this.tickCount, this.tickCount + time));
      this.onCooldownStarted(cooldownGroup, time);
   }

   public void removeCooldown(final Identifier cooldownGroup) {
      this.cooldowns.remove(cooldownGroup);
      this.onCooldownEnded(cooldownGroup);
   }

   protected void onCooldownStarted(final Identifier cooldownGroup, final int duration) {
   }

   protected void onCooldownEnded(final Identifier cooldownGroup) {
   }

   private record CooldownInstance(int startTime, int endTime) {
   }
}
