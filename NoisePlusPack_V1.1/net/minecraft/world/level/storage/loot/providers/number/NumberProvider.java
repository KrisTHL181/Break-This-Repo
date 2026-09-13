package net.minecraft.world.level.storage.loot.providers.number;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;

public interface NumberProvider extends LootContextUser {
   float getFloat(LootContext var1);

   default int getInt(LootContext p_165729_) {
      return Math.round(this.getFloat(p_165729_));
   }

   LootNumberProviderType getType();
}
