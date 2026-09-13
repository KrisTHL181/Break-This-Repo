package net.minecraft.data.loot.packs;

import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.SingleRegistryBootstrap;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class TradeRebalanceLootTableProvider {
   public static SingleRegistryBootstrap<LootTable> create() {
      return new LootTableProvider(Set.of(), List.of(new LootTableProvider.SubProviderEntry(TradeRebalanceChestLoot::new, LootContextParamSets.CHEST)));
   }
}
