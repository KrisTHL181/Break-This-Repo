package net.minecraft.data.loot.packs;

import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class VanillaEntityInteractLoot implements LootTableSubProvider {
   private final LootTableSubProvider.Context output;

   public VanillaEntityInteractLoot(final LootTableSubProvider.Context output) {
      this.output = output;
   }

   @Override
   public void run() {
      this.output
         .accept(
            BuiltInLootTables.ARMADILLO_BRUSH,
            LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.ARMADILLO_SCUTE)))
         );
   }
}
