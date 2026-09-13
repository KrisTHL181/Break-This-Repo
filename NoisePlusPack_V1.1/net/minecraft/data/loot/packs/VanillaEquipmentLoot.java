package net.minecraft.data.loot.packs;

import java.util.function.BiConsumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public record VanillaEquipmentLoot(HolderLookup.Provider registries) implements LootTableSubProvider {
   @Override
   public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> p_334928_) {
      HolderLookup.RegistryLookup<TrimPattern> registrylookup = this.registries.lookupOrThrow(Registries.TRIM_PATTERN);
      HolderLookup.RegistryLookup<TrimMaterial> registrylookup1 = this.registries.lookupOrThrow(Registries.TRIM_MATERIAL);
      HolderLookup.RegistryLookup<Enchantment> registrylookup2 = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      ArmorTrim armortrim = new ArmorTrim(registrylookup1.getOrThrow(TrimMaterials.COPPER), registrylookup.getOrThrow(TrimPatterns.FLOW));
      ArmorTrim armortrim1 = new ArmorTrim(registrylookup1.getOrThrow(TrimMaterials.COPPER), registrylookup.getOrThrow(TrimPatterns.BOLT));
      p_334928_.accept(
         BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(
                     NestedLootTable.inlineLootTable(
                           trialChamberEquipment(Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, armortrim1, registrylookup2).build()
                        )
                        .setWeight(4)
                  )
                  .add(
                     NestedLootTable.inlineLootTable(trialChamberEquipment(Items.IRON_HELMET, Items.IRON_CHESTPLATE, armortrim, registrylookup2).build())
                        .setWeight(2)
                  )
                  .add(
                     NestedLootTable.inlineLootTable(trialChamberEquipment(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, armortrim, registrylookup2).build())
                        .setWeight(1)
                  )
            )
      );
      p_334928_.accept(
         BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_MELEE,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(NestedLootTable.lootTableReference(BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.IRON_SWORD).setWeight(4))
                  .add(
                     LootItem.lootTableItem(Items.IRON_SWORD)
                        .apply(
                           new SetEnchantmentsFunction.Builder()
                              .withEnchantment(registrylookup2.getOrThrow(Enchantments.SHARPNESS), ConstantValue.exactly(1.0F))
                        )
                  )
                  .add(
                     LootItem.lootTableItem(Items.IRON_SWORD)
                        .apply(
                           new SetEnchantmentsFunction.Builder()
                              .withEnchantment(registrylookup2.getOrThrow(Enchantments.KNOCKBACK), ConstantValue.exactly(1.0F))
                        )
                  )
                  .add(LootItem.lootTableItem(Items.DIAMOND_SWORD))
            )
      );
      p_334928_.accept(
         BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(NestedLootTable.lootTableReference(BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.BOW).setWeight(2))
                  .add(
                     LootItem.lootTableItem(Items.BOW)
                        .apply(
                           new SetEnchantmentsFunction.Builder().withEnchantment(registrylookup2.getOrThrow(Enchantments.POWER), ConstantValue.exactly(1.0F))
                        )
                  )
                  .add(
                     LootItem.lootTableItem(Items.BOW)
                        .apply(
                           new SetEnchantmentsFunction.Builder().withEnchantment(registrylookup2.getOrThrow(Enchantments.PUNCH), ConstantValue.exactly(1.0F))
                        )
                  )
            )
      );
   }

   public static LootTable.Builder trialChamberEquipment(
      Item p_342256_, Item p_345109_, ArmorTrim p_363383_, HolderLookup.RegistryLookup<Enchantment> p_343180_
   ) {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .when(LootItemRandomChanceCondition.randomChance(0.5F))
               .add(
                  LootItem.lootTableItem(p_342256_)
                     .apply(SetComponentsFunction.setComponent(DataComponents.TRIM, p_363383_))
                     .apply(
                        new SetEnchantmentsFunction.Builder()
                           .withEnchantment(p_343180_.getOrThrow(Enchantments.PROTECTION), ConstantValue.exactly(4.0F))
                           .withEnchantment(p_343180_.getOrThrow(Enchantments.PROJECTILE_PROTECTION), ConstantValue.exactly(4.0F))
                           .withEnchantment(p_343180_.getOrThrow(Enchantments.FIRE_PROTECTION), ConstantValue.exactly(4.0F))
                     )
               )
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .when(LootItemRandomChanceCondition.randomChance(0.5F))
               .add(
                  LootItem.lootTableItem(p_345109_)
                     .apply(SetComponentsFunction.setComponent(DataComponents.TRIM, p_363383_))
                     .apply(
                        new SetEnchantmentsFunction.Builder()
                           .withEnchantment(p_343180_.getOrThrow(Enchantments.PROTECTION), ConstantValue.exactly(4.0F))
                           .withEnchantment(p_343180_.getOrThrow(Enchantments.PROJECTILE_PROTECTION), ConstantValue.exactly(4.0F))
                           .withEnchantment(p_343180_.getOrThrow(Enchantments.FIRE_PROTECTION), ConstantValue.exactly(4.0F))
                     )
               )
         );
   }
}
