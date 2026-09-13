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
    public void generate(final BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        HolderLookup.RegistryLookup<TrimPattern> trimPatterns = this.registries.lookupOrThrow(Registries.TRIM_PATTERN);
        HolderLookup.RegistryLookup<TrimMaterial> trimMaterials = this.registries.lookupOrThrow(Registries.TRIM_MATERIAL);
        HolderLookup.RegistryLookup<Enchantment> enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        ArmorTrim flowTrim = new ArmorTrim(trimMaterials.getOrThrow(TrimMaterials.COPPER), trimPatterns.getOrThrow(TrimPatterns.FLOW));
        ArmorTrim boltTrim = new ArmorTrim(trimMaterials.getOrThrow(TrimMaterials.COPPER), trimPatterns.getOrThrow(TrimPatterns.BOLT));
        output.accept(
            BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(
                            NestedLootTable.inlineLootTable(
                                    trialChamberEquipment(Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, boltTrim, enchantments).build()
                                )
                                .setWeight(4)
                        )
                        .add(
                            NestedLootTable.inlineLootTable(trialChamberEquipment(Items.IRON_HELMET, Items.IRON_CHESTPLATE, flowTrim, enchantments).build())
                                .setWeight(2)
                        )
                        .add(
                            NestedLootTable.inlineLootTable(
                                    trialChamberEquipment(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, flowTrim, enchantments).build()
                                )
                                .setWeight(1)
                        )
                )
        );
        output.accept(
            BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_MELEE,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(NestedLootTable.lootTableReference(BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER))
                )
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.IRON_SWORD).setWeight(4))
                        .add(
                            LootItem.lootTableItem(Items.IRON_SWORD)
                                .apply(
                                    new SetEnchantmentsFunction.Builder()
                                        .withEnchantment(enchantments.getOrThrow(Enchantments.SHARPNESS), ConstantValue.exactly(1.0F))
                                )
                        )
                        .add(
                            LootItem.lootTableItem(Items.IRON_SWORD)
                                .apply(
                                    new SetEnchantmentsFunction.Builder()
                                        .withEnchantment(enchantments.getOrThrow(Enchantments.KNOCKBACK), ConstantValue.exactly(1.0F))
                                )
                        )
                        .add(LootItem.lootTableItem(Items.DIAMOND_SWORD))
                )
        );
        output.accept(
            BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(NestedLootTable.lootTableReference(BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER))
                )
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.BOW).setWeight(2))
                        .add(
                            LootItem.lootTableItem(Items.BOW)
                                .apply(
                                    new SetEnchantmentsFunction.Builder()
                                        .withEnchantment(enchantments.getOrThrow(Enchantments.POWER), ConstantValue.exactly(1.0F))
                                )
                        )
                        .add(
                            LootItem.lootTableItem(Items.BOW)
                                .apply(
                                    new SetEnchantmentsFunction.Builder()
                                        .withEnchantment(enchantments.getOrThrow(Enchantments.PUNCH), ConstantValue.exactly(1.0F))
                                )
                        )
                )
        );
    }

    public static LootTable.Builder trialChamberEquipment(
        final Item helmet, final Item chestplate, final ArmorTrim trim, final HolderLookup.RegistryLookup<Enchantment> enchantments
    ) {
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
                                    .withEnchantment(enchantments.getOrThrow(Enchantments.PROTECTION), ConstantValue.exactly(4.0F))
                                    .withEnchantment(enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION), ConstantValue.exactly(4.0F))
                                    .withEnchantment(enchantments.getOrThrow(Enchantments.FIRE_PROTECTION), ConstantValue.exactly(4.0F))
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
                                    .withEnchantment(enchantments.getOrThrow(Enchantments.PROTECTION), ConstantValue.exactly(4.0F))
                                    .withEnchantment(enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION), ConstantValue.exactly(4.0F))
                                    .withEnchantment(enchantments.getOrThrow(Enchantments.FIRE_PROTECTION), ConstantValue.exactly(4.0F))
                            )
                    )
            );
    }
}