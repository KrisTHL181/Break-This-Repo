package net.minecraft.data.loot.packs;

import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class VanillaShearingLoot implements LootTableSubProvider {
   private final LootTableSubProvider.Context output;

   public VanillaShearingLoot(final LootTableSubProvider.Context output) {
      this.output = output;
   }

   @Override
   public void run() {
      this.output
         .accept(
            BuiltInLootTables.BOGGED_SHEAR,
            LootTable.lootTable()
               .withPool(
                  LootPool.lootPool()
                     .setRolls(ConstantValue.exactly(2.0F))
                     .add(LootItem.lootTableItem(Items.BROWN_MUSHROOM).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                     .add(LootItem.lootTableItem(Items.RED_MUSHROOM).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               )
         );
      ColorCollection.zipApply(
         BuiltInLootTables.SHEAR_DYED_SHEEP,
         Blocks.WOOL,
         (dyedSheep, wool) -> this.output
            .accept(
               dyedSheep, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1.0F, 3.0F)).add(LootItem.lootTableItem(wool)))
            )
      );
      this.output
         .accept(
            BuiltInLootTables.SHEAR_SHEEP, LootTable.lootTable().withPool(EntityLootSubProvider.createSheepDispatchPool(BuiltInLootTables.SHEAR_DYED_SHEEP))
         );
      this.output
         .accept(
            BuiltInLootTables.SHEAR_MOOSHROOM,
            LootTable.lootTable()
               .withPool(
                  LootPool.lootPool()
                     .add(
                        AlternativesEntry.alternatives(
                           NestedLootTable.lootTableReference(BuiltInLootTables.SHEAR_RED_MOOSHROOM)
                              .when(
                                 LootItemEntityPropertyCondition.hasProperties(
                                    LootContext.EntityTarget.THIS,
                                    EntityPredicate.Builder.entity()
                                       .components(DataComponentExactPredicate.expect(DataComponents.MOOSHROOM_VARIANT, MushroomCow.Variant.RED))
                                 )
                              ),
                           NestedLootTable.lootTableReference(BuiltInLootTables.SHEAR_BROWN_MOOSHROOM)
                              .when(
                                 LootItemEntityPropertyCondition.hasProperties(
                                    LootContext.EntityTarget.THIS,
                                    EntityPredicate.Builder.entity()
                                       .components(DataComponentExactPredicate.expect(DataComponents.MOOSHROOM_VARIANT, MushroomCow.Variant.BROWN))
                                 )
                              )
                        )
                     )
               )
         );
      this.output
         .accept(
            BuiltInLootTables.SHEAR_RED_MOOSHROOM,
            LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(5.0F)).add(LootItem.lootTableItem(Items.RED_MUSHROOM)))
         );
      this.output
         .accept(
            BuiltInLootTables.SHEAR_BROWN_MOOSHROOM,
            LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(5.0F)).add(LootItem.lootTableItem(Items.BROWN_MUSHROOM)))
         );
      this.output
         .accept(
            BuiltInLootTables.SHEAR_SNOW_GOLEM,
            LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.CARVED_PUMPKIN)))
         );
   }
}
