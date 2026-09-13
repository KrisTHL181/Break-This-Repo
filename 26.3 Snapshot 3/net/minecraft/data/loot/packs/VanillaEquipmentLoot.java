package net.minecraft.data.loot.packs;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
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

public class VanillaEquipmentLoot implements LootTableSubProvider {
   private final LootTableSubProvider.Context output;
   private final HolderGetter<TrimPattern> trimPatterns;
   private final HolderGetter<TrimMaterial> trimMaterials;
   private final HolderGetter<Enchantment> enchantments;

   public VanillaEquipmentLoot(final LootTableSubProvider.Context output) {
      this.output = output;
      this.trimPatterns = output.lookup(Registries.TRIM_PATTERN);
      this.trimMaterials = output.lookup(Registries.TRIM_MATERIAL);
      this.enchantments = output.lookup(Registries.ENCHANTMENT);
   }

   @Override
   public void run() {
      ArmorTrim flowTrim = new ArmorTrim(this.trimMaterials.getOrThrow(TrimMaterials.COPPER), this.trimPatterns.getOrThrow(TrimPatterns.FLOW));
      ArmorTrim boltTrim = new ArmorTrim(this.trimMaterials.getOrThrow(TrimMaterials.COPPER), this.trimPatterns.getOrThrow(TrimPatterns.BOLT));
      this.output
         .accept(
            BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER,
            LootTable.lootTable()
               .withPool(
                  LootPool.lootPool()
                     .setRolls(ConstantValue.exactly(1.0F))
                     .add(
                        NestedLootTable.inlineLootTable(this.trialChamberEquipment(Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, boltTrim).build())
                           .setWeight(4)
                     )
                     .add(NestedLootTable.inlineLootTable(this.trialChamberEquipment(Items.IRON_HELMET, Items.IRON_CHESTPLATE, flowTrim).build()).setWeight(2))
                     .add(
                        NestedLootTable.inlineLootTable(this.trialChamberEquipment(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, flowTrim).build())
                           .setWeight(1)
                     )
               )
         );
      this.output
         .accept(
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
                                 .withEnchantment(this.enchantments.getOrThrow(Enchantments.SHARPNESS), ConstantValue.exactly(1.0F))
                           )
                     )
                     .add(
                        LootItem.lootTableItem(Items.IRON_SWORD)
                           .apply(
                              new SetEnchantmentsFunction.Builder()
                                 .withEnchantment(this.enchantments.getOrThrow(Enchantments.KNOCKBACK), ConstantValue.exactly(1.0F))
                           )
                     )
                     .add(LootItem.lootTableItem(Items.DIAMOND_SWORD))
               )
         );
      this.output
         .accept(
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
                              new SetEnchantmentsFunction.Builder()
                                 .withEnchantment(this.enchantments.getOrThrow(Enchantments.POWER), ConstantValue.exactly(1.0F))
                           )
                     )
                     .add(
                        LootItem.lootTableItem(Items.BOW)
                           .apply(
                              new SetEnchantmentsFunction.Builder()
                                 .withEnchantment(this.enchantments.getOrThrow(Enchantments.PUNCH), ConstantValue.exactly(1.0F))
                           )
                     )
               )
         );
   }

   public LootTable.Builder trialChamberEquipment(final Item helmet, final Item chestplate, final ArmorTrim trim) {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .when(LootItemRandomChanceCondition.randomChance(0.5F))
               .add(
                  LootItem.lootTableItem(helmet)
                     .apply(SetComponentsFunction.setComponent(DataComponents.TRIM, trim))
                     .apply(
                        new SetEnchantmentsFunction.Builder()
                           .withEnchantment(this.enchantments.getOrThrow(Enchantments.PROTECTION), ConstantValue.exactly(4.0F))
                           .withEnchantment(this.enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION), ConstantValue.exactly(4.0F))
                           .withEnchantment(this.enchantments.getOrThrow(Enchantments.FIRE_PROTECTION), ConstantValue.exactly(4.0F))
                     )
               )
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .when(LootItemRandomChanceCondition.randomChance(0.5F))
               .add(
                  LootItem.lootTableItem(chestplate)
                     .apply(SetComponentsFunction.setComponent(DataComponents.TRIM, trim))
                     .apply(
                        new SetEnchantmentsFunction.Builder()
                           .withEnchantment(this.enchantments.getOrThrow(Enchantments.PROTECTION), ConstantValue.exactly(4.0F))
                           .withEnchantment(this.enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION), ConstantValue.exactly(4.0F))
                           .withEnchantment(this.enchantments.getOrThrow(Enchantments.FIRE_PROTECTION), ConstantValue.exactly(4.0F))
                     )
               )
         );
   }
}
