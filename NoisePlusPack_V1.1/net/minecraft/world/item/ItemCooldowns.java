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

   public boolean isOnCooldown(ItemStack p_369547_) {
      return this.getCooldownPercent(p_369547_, 0.0F) > 0.0F;
   }

   public float getCooldownPercent(ItemStack p_366950_, float p_41523_) {
      Identifier identifier = this.getCooldownGroup(p_366950_);
      ItemCooldowns.CooldownInstance itemcooldowns$cooldowninstance = this.cooldowns.get(identifier);
      if (itemcooldowns$cooldowninstance != null) {
         float f = itemcooldowns$cooldowninstance.endTime - itemcooldowns$cooldowninstance.startTime;
         float f1 = itemcooldowns$cooldowninstance.endTime - (this.tickCount + p_41523_);
         return Mth.clamp(f1 / f, 0.0F, 1.0F);
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

   public Identifier getCooldownGroup(ItemStack p_361933_) {
      UseCooldown usecooldown = p_361933_.get(DataComponents.USE_COOLDOWN);
      Identifier identifier = BuiltInRegistries.ITEM.getKey(p_361933_.getItem());
      return usecooldown == null ? identifier : usecooldown.cooldownGroup().orElse(identifier);
   }

   public void addCooldown(ItemStack p_366379_, int p_367584_) {
      this.addCooldown(this.getCooldownGroup(p_366379_), p_367584_);
   }

   public void addCooldown(Identifier p_455409_, int p_41526_) {
      this.cooldowns.put(p_455409_, new ItemCooldowns.CooldownInstance(this.tickCount, this.tickCount + p_41526_));
      this.onCooldownStarted(p_455409_, p_41526_);
   }

   public void removeCooldown(Identifier p_450820_) {
      this.cooldowns.remove(p_450820_);
      this.onCooldownEnded(p_450820_);
   }

   protected void onCooldownStarted(Identifier p_452458_, int p_41530_) {
   }

   protected void onCooldownEnded(Identifier p_454919_) {
   }

   record CooldownInstance(int startTime, int endTime) {
   }
}
