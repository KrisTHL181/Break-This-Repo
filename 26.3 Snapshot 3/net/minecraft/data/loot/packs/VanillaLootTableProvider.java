package net.minecraft.data.loot.packs;

import java.util.List;
import net.minecraft.core.registries.SingleRegistryBootstrap;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class VanillaLootTableProvider {
   public static SingleRegistryBootstrap<LootTable> create() {
      return new LootTableProvider(
         BuiltInLootTables.all(),
         List.of(
            new LootTableProvider.SubProviderEntry(VanillaFishingLoot::new, LootContextParamSets.FISHING),
            new LootTableProvider.SubProviderEntry(VanillaChestLoot::new, LootContextParamSets.CHEST),
            new LootTableProvider.SubProviderEntry(VanillaEntityLoot::new, LootContextParamSets.ENTITY),
            new LootTableProvider.SubProviderEntry(VanillaEquipmentLoot::new, LootContextParamSets.EQUIPMENT),
            new LootTableProvider.SubProviderEntry(VanillaBlockLoot::new, LootContextParamSets.BLOCK),
            new LootTableProvider.SubProviderEntry(VanillaPiglinBarterLoot::new, LootContextParamSets.PIGLIN_BARTER),
            new LootTableProvider.SubProviderEntry(VanillaGiftLoot::new, LootContextParamSets.GIFT),
            new LootTableProvider.SubProviderEntry(VanillaArchaeologyLoot::new, LootContextParamSets.ARCHAEOLOGY),
            new LootTableProvider.SubProviderEntry(VanillaShearingLoot::new, LootContextParamSets.SHEARING),
            new LootTableProvider.SubProviderEntry(VanillaEntityInteractLoot::new, LootContextParamSets.ENTITY_INTERACT),
            new LootTableProvider.SubProviderEntry(VanillaBlockInteractLoot::new, LootContextParamSets.BLOCK_INTERACT),
            new LootTableProvider.SubProviderEntry(VanillaChargedCreeperExplosionLoot::new, LootContextParamSets.ENTITY)
         )
      );
   }
}
