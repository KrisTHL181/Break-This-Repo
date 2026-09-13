package net.minecraft.data.loot.packs;

import java.util.function.BiConsumer;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public record VanillaBlockInteractLoot(HolderLookup.Provider registries) implements LootTableSubProvider {
   @Override
   public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> p_424959_) {
      p_424959_.accept(
         BuiltInLootTables.HARVEST_BEEHIVE,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.HONEYCOMB).apply(SetItemCountFunction.setCount(ConstantValue.exactly(3.0F))))
            )
      );
      p_424959_.accept(
         BuiltInLootTables.HARVEST_CAVE_VINE,
         LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.GLOW_BERRIES)))
      );
      p_424959_.accept(
         BuiltInLootTables.HARVEST_SWEET_BERRY_BUSH,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .add(
                     LootItem.lootTableItem(Items.SWEET_BERRIES)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .when(
                           LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.SWEET_BERRY_BUSH)
                              .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SweetBerryBushBlock.AGE, 3))
                        )
                  )
            )
            .withPool(
               LootPool.lootPool().add(LootItem.lootTableItem(Items.SWEET_BERRIES).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
            )
      );
      p_424959_.accept(
         BuiltInLootTables.CARVE_PUMPKIN,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.PUMPKIN_SEEDS).apply(SetItemCountFunction.setCount(ConstantValue.exactly(4.0F))))
            )
      );
   }
}
