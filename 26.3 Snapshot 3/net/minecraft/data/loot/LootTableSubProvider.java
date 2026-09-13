package net.minecraft.data.loot;

import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.BootstrapContextAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

@FunctionalInterface
public interface LootTableSubProvider {
   void run();

   interface Context extends BootstrapContextAccess {
      Holder.Reference<LootTable> accept(ResourceKey<LootTable> key, LootTable.Builder value);
   }

   interface Factory {
      LootTableSubProvider create(LootTableSubProvider.Context context);
   }
}
