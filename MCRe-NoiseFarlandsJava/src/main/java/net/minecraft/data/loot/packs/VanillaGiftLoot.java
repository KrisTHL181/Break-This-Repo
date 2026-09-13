package net.minecraft.data.loot.packs;

import java.util.function.BiConsumer;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.chicken.ChickenVariants;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public record VanillaGiftLoot(HolderLookup.Provider registries) implements LootTableSubProvider {
    @Override
    public void generate(final BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        HolderGetter<ChickenVariant> chickenVariants = this.registries.lookupOrThrow(Registries.CHICKEN_VARIANT);
        output.accept(
            BuiltInLootTables.CAT_MORNING_GIFT,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.RABBIT_HIDE).setWeight(10))
                        .add(LootItem.lootTableItem(Items.RABBIT_FOOT).setWeight(10))
                        .add(LootItem.lootTableItem(Items.CHICKEN).setWeight(10))
                        .add(LootItem.lootTableItem(Items.FEATHER).setWeight(10))
                        .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(10))
                        .add(LootItem.lootTableItem(Items.STRING).setWeight(10))
                        .add(LootItem.lootTableItem(Items.PHANTOM_MEMBRANE).setWeight(2))
                )
        );
        output.accept(
            BuiltInLootTables.ARMORER_GIFT,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.CHAINMAIL_HELMET))
                        .add(LootItem.lootTableItem(Items.CHAINMAIL_CHESTPLATE))
                        .add(LootItem.lootTableItem(Items.CHAINMAIL_LEGGINGS))
                        .add(LootItem.lootTableItem(Items.CHAINMAIL_BOOTS))
                )
        );
        output.accept(
            BuiltInLootTables.BUTCHER_GIFT,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.COOKED_RABBIT))
                        .add(LootItem.lootTableItem(Items.COOKED_CHICKEN))
                        .add(LootItem.lootTableItem(Items.COOKED_PORKCHOP))
                        .add(LootItem.lootTableItem(Items.COOKED_BEEF))
                        .add(LootItem.lootTableItem(Items.COOKED_MUTTON))
                )
        );
        output.accept(
            BuiltInLootTables.CARTOGRAPHER_GIFT,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.MAP)).add(LootItem.lootTableItem(Items.PAPER))
                )
        );
        output.accept(
            BuiltInLootTables.CLERIC_GIFT,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.REDSTONE))
                        .add(LootItem.lootTableItem(Items.LAPIS_LAZULI))
                )
        );
        output.accept(
            BuiltInLootTables.FARMER_GIFT,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.BREAD))
                        .add(LootItem.lootTableItem(Items.PUMPKIN_PIE))
                        .add(LootItem.lootTableItem(Items.COOKIE))
                )
        );
        output.accept(
            BuiltInLootTables.FISHERMAN_GIFT,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.COD)).add(LootItem.lootTableItem(Items.SALMON))
                )
        );
        output.accept(
            BuiltInLootTables.FLETCHER_GIFT,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.ARROW).setWeight(26))
                        .add(
                            LootItem.lootTableItem(Items.TIPPED_ARROW)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(SetPotionFunction.setPotion(Potions.SWIFTNESS))
                        )
                        .add(
                            LootItem.lootTableItem(Items.TIPPED_ARROW)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(SetPotionFunction.setPotion(Potions.SLOWNESS))
                        )
                        .add(
                            LootItem.lootTableItem(Items.TIPPED_ARROW)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(SetPotionFunction.setPotion(Potions.STRENGTH))
                        )
                        .add(
                            LootItem.lootTableItem(Items.TIPPED_ARROW)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(SetPotionFunction.setPotion(Potions.HEALING))
                        )
                        .add(
                            LootItem.lootTableItem(Items.TIPPED_ARROW)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(SetPotionFunction.setPotion(Potions.HARMING))
                        )
                        .add(
                            LootItem.lootTableItem(Items.TIPPED_ARROW)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(SetPotionFunction.setPotion(Potions.LEAPING))
                        )
                        .add(
                            LootItem.lootTableItem(Items.TIPPED_ARROW)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(SetPotionFunction.setPotion(Potions.REGENERATION))
                        )
                        .add(
                            LootItem.lootTableItem(Items.TIPPED_ARROW)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(SetPotionFunction.setPotion(Potions.FIRE_RESISTANCE))
                        )
                        .add(
                            LootItem.lootTableItem(Items.TIPPED_ARROW)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(SetPotionFunction.setPotion(Potions.WATER_BREATHING))
                        )
                        .add(
                            LootItem.lootTableItem(Items.TIPPED_ARROW)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(SetPotionFunction.setPotion(Potions.INVISIBILITY))
                        )
                        .add(
                            LootItem.lootTableItem(Items.TIPPED_ARROW)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(SetPotionFunction.setPotion(Potions.NIGHT_VISION))
                        )
                        .add(
                            LootItem.lootTableItem(Items.TIPPED_ARROW)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(SetPotionFunction.setPotion(Potions.WEAKNESS))
                        )
                        .add(
                            LootItem.lootTableItem(Items.TIPPED_ARROW)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F)))
                                .apply(SetPotionFunction.setPotion(Potions.POISON))
                        )
                )
        );
        output.accept(
            BuiltInLootTables.LEATHERWORKER_GIFT,
            LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.LEATHER)))
        );
        output.accept(
            BuiltInLootTables.LIBRARIAN_GIFT,
            LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.BOOK)))
        );
        output.accept(
            BuiltInLootTables.MASON_GIFT,
            LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.CLAY)))
        );
        output.accept(
            BuiltInLootTables.SHEPHERD_GIFT,
            LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).addAll(Items.WOOL.map(LootItem::lootTableItem).asList()))
        );
        output.accept(
            BuiltInLootTables.TOOLSMITH_GIFT,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.STONE_PICKAXE))
                        .add(LootItem.lootTableItem(Items.STONE_AXE))
                        .add(LootItem.lootTableItem(Items.STONE_HOE))
                        .add(LootItem.lootTableItem(Items.STONE_SHOVEL))
                )
        );
        output.accept(
            BuiltInLootTables.WEAPONSMITH_GIFT,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.STONE_AXE))
                        .add(LootItem.lootTableItem(Items.GOLDEN_AXE))
                        .add(LootItem.lootTableItem(Items.IRON_AXE))
                )
        );
        output.accept(
            BuiltInLootTables.UNEMPLOYED_GIFT,
            LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.WHEAT_SEEDS)))
        );
        output.accept(
            BuiltInLootTables.BABY_VILLAGER_GIFT,
            LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.POPPY)))
        );
        output.accept(
            BuiltInLootTables.SNIFFER_DIGGING,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.TORCHFLOWER_SEEDS))
                        .add(LootItem.lootTableItem(Items.PITCHER_POD))
                )
        );
        output.accept(
            BuiltInLootTables.PANDA_SNEEZE,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.SLIME_BALL).setWeight(1))
                        .add(EmptyLootItem.emptyItem().setWeight(699))
                )
        );
        output.accept(
            BuiltInLootTables.CHICKEN_LAY,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(
                            AlternativesEntry.alternatives(
                                LootItem.lootTableItem(Items.EGG)
                                    .when(
                                        LootItemEntityPropertyCondition.hasProperties(
                                            LootContext.EntityTarget.THIS,
                                            EntityPredicate.Builder.entity()
                                                .components(
                                                    DataComponentExactPredicate.expect(
                                                        DataComponents.CHICKEN_VARIANT, chickenVariants.getOrThrow(ChickenVariants.TEMPERATE)
                                                    )
                                                )
                                        )
                                    ),
                                LootItem.lootTableItem(Items.BROWN_EGG)
                                    .when(
                                        LootItemEntityPropertyCondition.hasProperties(
                                            LootContext.EntityTarget.THIS,
                                            EntityPredicate.Builder.entity()
                                                .components(
                                                    DataComponentExactPredicate.expect(
                                                        DataComponents.CHICKEN_VARIANT, chickenVariants.getOrThrow(ChickenVariants.WARM)
                                                    )
                                                )
                                        )
                                    ),
                                LootItem.lootTableItem(Items.BLUE_EGG)
                                    .when(
                                        LootItemEntityPropertyCondition.hasProperties(
                                            LootContext.EntityTarget.THIS,
                                            EntityPredicate.Builder.entity()
                                                .components(
                                                    DataComponentExactPredicate.expect(
                                                        DataComponents.CHICKEN_VARIANT, chickenVariants.getOrThrow(ChickenVariants.COLD)
                                                    )
                                                )
                                        )
                                    )
                            )
                        )
                )
        );
        output.accept(
            BuiltInLootTables.ARMADILLO_SHED,
            LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.ARMADILLO_SCUTE)))
        );
        output.accept(
            BuiltInLootTables.TURTLE_GROW,
            LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.TURTLE_SCUTE)))
        );
    }
}