package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;

public interface NumberProvider extends LootContextUser {
   float getFloat(LootContext context);

   default int getInt(final LootContext context) {
      return Math.round(this.getFloat(context));
   }

   MapCodec<? extends NumberProvider> codec();
}
