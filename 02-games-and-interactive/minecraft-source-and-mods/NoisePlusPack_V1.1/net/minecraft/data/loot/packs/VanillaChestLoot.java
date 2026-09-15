package net.minecraft.data.loot.packs;

import java.util.function.BiConsumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.InstrumentTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetInstrumentFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.functions.SetNameFunction;
import net.minecraft.world.level.storage.loot.functions.SetOminousBottleAmplifierFunction;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.functions.SetStewEffectFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public record VanillaChestLoot(HolderLookup.Provider registries) implements LootTableSubProvider {
   @Override
   public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> p_250931_) {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      p_250931_.accept(
         BuiltInLootTables.ABANDONED_MINESHAFT,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(20))
                  .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE))
                  .add(LootItem.lootTableItem(Items.NAME_TAG).setWeight(30))
                  .add(LootItem.lootTableItem(Items.BOOK).setWeight(10).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
                  .add(LootItem.lootTableItem(Items.IRON_PICKAXE).setWeight(5))
                  .add(EmptyLootItem.emptyItem().setWeight(5))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(2.0F, 4.0F))
                  .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                  .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.REDSTONE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 9.0F))))
                  .add(LootItem.lootTableItem(Items.LAPIS_LAZULI).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 9.0F))))
                  .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                  .add(LootItem.lootTableItem(Items.COAL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.GLOW_BERRIES).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 6.0F))))
                  .add(LootItem.lootTableItem(Items.MELON_SEEDS).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.PUMPKIN_SEEDS).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.BEETROOT_SEEDS).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(3.0F))
                  .add(LootItem.lootTableItem(Blocks.RAIL).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F))))
                  .add(LootItem.lootTableItem(Blocks.POWERED_RAIL).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Blocks.DETECTOR_RAIL).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Blocks.ACTIVATOR_RAIL).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Blocks.TORCH).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 16.0F))))
            )
      );
      p_250931_.accept(BuiltInLootTables.BASTION_BRIDGE, this.bastionBridgeLootTable());
      p_250931_.accept(BuiltInLootTables.BASTION_HOGLIN_STABLE, this.bastionHoglinStableLootTable());
      p_250931_.accept(BuiltInLootTables.BASTION_OTHER, this.bastionOtherLootTable());
      p_250931_.accept(BuiltInLootTables.BASTION_TREASURE, this.bastionTreasureLootTable());
      p_250931_.accept(
         BuiltInLootTables.BURIED_TREASURE,
         LootTable.lootTable()
            .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.HEART_OF_THE_SEA)))
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(5.0F, 8.0F))
                  .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Blocks.TNT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 3.0F))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F))))
                  .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                  .add(
                     LootItem.lootTableItem(Items.PRISMARINE_CRYSTALS).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F)))
                  )
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(0.0F, 1.0F))
                  .add(LootItem.lootTableItem(Items.LEATHER_CHESTPLATE))
                  .add(LootItem.lootTableItem(Items.IRON_SWORD))
                  .add(LootItem.lootTableItem(Items.IRON_SPEAR))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(2.0F))
                  .add(LootItem.lootTableItem(Items.COOKED_COD).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.COOKED_SALMON).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(0.0F, 2.0F))
                  .add(LootItem.lootTableItem(Items.POTION))
                  .apply(SetPotionFunction.setPotion(Potions.WATER_BREATHING))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(EmptyLootItem.emptyItem().setWeight(148))
                  .add(LootItem.lootTableItem(Items.COPPER_NAUTILUS_ARMOR).setWeight(20).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(LootItem.lootTableItem(Items.IRON_NAUTILUS_ARMOR).setWeight(10).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(LootItem.lootTableItem(Items.GOLDEN_NAUTILUS_ARMOR).setWeight(5).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(LootItem.lootTableItem(Items.DIAMOND_NAUTILUS_ARMOR).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
            )
      );
      p_250931_.accept(BuiltInLootTables.ANCIENT_CITY, this.ancientCityLootTable());
      p_250931_.accept(
         BuiltInLootTables.ANCIENT_CITY_ICE_BOX,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(4.0F, 10.0F))
                  .add(
                     LootItem.lootTableItem(Items.SUSPICIOUS_STEW)
                        .setWeight(1)
                        .apply(
                           SetStewEffectFunction.stewEffect()
                              .withEffect(MobEffects.NIGHT_VISION, UniformGenerator.between(7.0F, 10.0F))
                              .withEffect(MobEffects.BLINDNESS, UniformGenerator.between(5.0F, 7.0F))
                        )
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F)))
                  )
                  .add(LootItem.lootTableItem(Items.GOLDEN_CARROT).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 10.0F))))
                  .add(LootItem.lootTableItem(Items.BAKED_POTATO).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 10.0F))))
                  .add(LootItem.lootTableItem(Items.PACKED_ICE).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                  .add(LootItem.lootTableItem(Items.SNOWBALL).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
            )
      );
      p_250931_.accept(BuiltInLootTables.DESERT_PYRAMID, this.desertPyramidLootTable());
      p_250931_.accept(BuiltInLootTables.END_CITY_TREASURE, this.endCityTreasureLootTable());
      p_250931_.accept(
         BuiltInLootTables.IGLOO_CHEST,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(2.0F, 8.0F))
                  .add(LootItem.lootTableItem(Items.APPLE).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.COAL).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.STONE_AXE).setWeight(2))
                  .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(10))
                  .add(LootItem.lootTableItem(Items.EMERALD))
                  .add(LootItem.lootTableItem(Items.WHEAT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 3.0F))))
            )
            .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.GOLDEN_APPLE)))
      );
      p_250931_.accept(BuiltInLootTables.JUNGLE_TEMPLE, this.jungleTempleLootTable());
      p_250931_.accept(
         BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 2.0F))
                  .add(LootItem.lootTableItem(Items.ARROW).setWeight(30).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))
            )
      );
      p_250931_.accept(BuiltInLootTables.NETHER_BRIDGE, this.netherBridgeLootTable());
      p_250931_.accept(BuiltInLootTables.PILLAGER_OUTPOST, this.pillagerOutpostLootTable());
      p_250931_.accept(BuiltInLootTables.SHIPWRECK_MAP, this.shipwreckMapLootTable());
      p_250931_.accept(BuiltInLootTables.SHIPWRECK_SUPPLY, this.shipwreckSupplyLootTable());
      p_250931_.accept(BuiltInLootTables.SHIPWRECK_TREASURE, this.shipwreckTreasureLootTable());
      p_250931_.accept(
         BuiltInLootTables.SIMPLE_DUNGEON,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 3.0F))
                  .add(LootItem.lootTableItem(Items.LEATHER).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                  .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(15))
                  .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).setWeight(2))
                  .add(LootItem.lootTableItem(Items.MUSIC_DISC_OTHERSIDE).setWeight(2))
                  .add(LootItem.lootTableItem(Items.MUSIC_DISC_13).setWeight(15))
                  .add(LootItem.lootTableItem(Items.MUSIC_DISC_CAT).setWeight(15))
                  .add(LootItem.lootTableItem(Items.NAME_TAG).setWeight(20))
                  .add(LootItem.lootTableItem(Items.GOLDEN_HORSE_ARMOR).setWeight(10))
                  .add(LootItem.lootTableItem(Items.COPPER_HORSE_ARMOR).setWeight(15))
                  .add(LootItem.lootTableItem(Items.IRON_HORSE_ARMOR).setWeight(15))
                  .add(LootItem.lootTableItem(Items.DIAMOND_HORSE_ARMOR).setWeight(5))
                  .add(LootItem.lootTableItem(Items.BOOK).setWeight(10).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 4.0F))
                  .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(20))
                  .add(LootItem.lootTableItem(Items.WHEAT).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.BUCKET).setWeight(10))
                  .add(LootItem.lootTableItem(Items.REDSTONE).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.COAL).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.MELON_SEEDS).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.PUMPKIN_SEEDS).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.BEETROOT_SEEDS).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(3.0F))
                  .add(LootItem.lootTableItem(Items.BONE).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                  .add(LootItem.lootTableItem(Items.GUNPOWDER).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                  .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                  .add(LootItem.lootTableItem(Items.STRING).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.SPAWN_BONUS_CHEST,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.STONE_AXE))
                  .add(LootItem.lootTableItem(Items.WOODEN_AXE).setWeight(3))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.STONE_PICKAXE))
                  .add(LootItem.lootTableItem(Items.WOODEN_PICKAXE).setWeight(3))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(3.0F))
                  .add(LootItem.lootTableItem(Items.APPLE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                  .add(LootItem.lootTableItem(Items.SALMON).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(4.0F))
                  .add(LootItem.lootTableItem(Items.STICK).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 12.0F))))
                  .add(LootItem.lootTableItem(Blocks.OAK_PLANKS).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 12.0F))))
                  .add(LootItem.lootTableItem(Blocks.OAK_LOG).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Blocks.SPRUCE_LOG).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Blocks.BIRCH_LOG).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Blocks.JUNGLE_LOG).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Blocks.ACACIA_LOG).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Blocks.DARK_OAK_LOG).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Blocks.MANGROVE_LOG).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
            )
      );
      p_250931_.accept(BuiltInLootTables.STRONGHOLD_CORRIDOR, this.strongholdCorridorLootTable());
      p_250931_.accept(
         BuiltInLootTables.STRONGHOLD_CROSSING,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 4.0F))
                  .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                  .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.REDSTONE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 9.0F))))
                  .add(LootItem.lootTableItem(Items.COAL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.APPLE).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.IRON_PICKAXE))
                  .add(LootItem.lootTableItem(Items.BOOK).apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, ConstantValue.exactly(30.0F))))
            )
      );
      p_250931_.accept(BuiltInLootTables.STRONGHOLD_LIBRARY, this.strongholdLibraryLootTable());
      p_250931_.accept(
         BuiltInLootTables.UNDERWATER_RUIN_BIG,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(2.0F, 8.0F))
                  .add(LootItem.lootTableItem(Items.COAL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.EMERALD))
                  .add(LootItem.lootTableItem(Items.STONE_SPEAR).setWeight(2))
                  .add(LootItem.lootTableItem(Items.WHEAT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 3.0F))))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.GOLDEN_APPLE))
                  .add(LootItem.lootTableItem(Items.BOOK).setWeight(5).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
                  .add(LootItem.lootTableItem(Items.LEATHER_CHESTPLATE))
                  .add(LootItem.lootTableItem(Items.GOLDEN_HELMET))
                  .add(LootItem.lootTableItem(Items.FISHING_ROD).setWeight(5).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
                  .add(
                     LootItem.lootTableItem(Items.MAP)
                        .setWeight(10)
                        .apply(
                           ExplorationMapFunction.makeExplorationMap()
                              .setDestination(StructureTags.ON_TREASURE_MAPS)
                              .setMapDecoration(MapDecorationTypes.RED_X)
                              .setZoom((byte)1)
                              .setSkipKnownStructures(false)
                        )
                        .apply(SetNameFunction.setName(Component.translatable("filled_map.buried_treasure"), SetNameFunction.Target.ITEM_NAME))
                  )
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(EmptyLootItem.emptyItem().setWeight(148))
                  .add(LootItem.lootTableItem(Items.COPPER_NAUTILUS_ARMOR).setWeight(20).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(LootItem.lootTableItem(Items.IRON_NAUTILUS_ARMOR).setWeight(10).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(LootItem.lootTableItem(Items.GOLDEN_NAUTILUS_ARMOR).setWeight(5).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(LootItem.lootTableItem(Items.DIAMOND_NAUTILUS_ARMOR).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.UNDERWATER_RUIN_SMALL,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(2.0F, 8.0F))
                  .add(LootItem.lootTableItem(Items.COAL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.STONE_AXE).setWeight(2))
                  .add(LootItem.lootTableItem(Items.STONE_SPEAR).setWeight(2))
                  .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(5))
                  .add(LootItem.lootTableItem(Items.EMERALD))
                  .add(LootItem.lootTableItem(Items.WHEAT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 3.0F))))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.LEATHER_CHESTPLATE))
                  .add(LootItem.lootTableItem(Items.GOLDEN_HELMET))
                  .add(LootItem.lootTableItem(Items.FISHING_ROD).setWeight(5).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
                  .add(
                     LootItem.lootTableItem(Items.MAP)
                        .setWeight(5)
                        .apply(
                           ExplorationMapFunction.makeExplorationMap()
                              .setDestination(StructureTags.ON_TREASURE_MAPS)
                              .setMapDecoration(MapDecorationTypes.RED_X)
                              .setZoom((byte)1)
                              .setSkipKnownStructures(false)
                        )
                        .apply(SetNameFunction.setName(Component.translatable("filled_map.buried_treasure"), SetNameFunction.Target.ITEM_NAME))
                  )
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(EmptyLootItem.emptyItem().setWeight(148))
                  .add(LootItem.lootTableItem(Items.COPPER_NAUTILUS_ARMOR).setWeight(20).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(LootItem.lootTableItem(Items.IRON_NAUTILUS_ARMOR).setWeight(10).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(LootItem.lootTableItem(Items.GOLDEN_NAUTILUS_ARMOR).setWeight(5).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(LootItem.lootTableItem(Items.DIAMOND_NAUTILUS_ARMOR).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_WEAPONSMITH,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(3.0F, 8.0F))
                  .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                  .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.APPLE).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.IRON_PICKAXE).setWeight(5))
                  .add(LootItem.lootTableItem(Items.IRON_SWORD).setWeight(5))
                  .add(LootItem.lootTableItem(Items.IRON_SPEAR).setWeight(5))
                  .add(LootItem.lootTableItem(Items.COPPER_SPEAR).setWeight(7))
                  .add(LootItem.lootTableItem(Items.IRON_CHESTPLATE).setWeight(5))
                  .add(LootItem.lootTableItem(Items.IRON_HELMET).setWeight(5))
                  .add(LootItem.lootTableItem(Items.IRON_LEGGINGS).setWeight(5))
                  .add(LootItem.lootTableItem(Items.IRON_BOOTS).setWeight(5))
                  .add(LootItem.lootTableItem(Blocks.OBSIDIAN).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 7.0F))))
                  .add(LootItem.lootTableItem(Blocks.OAK_SAPLING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 7.0F))))
                  .add(LootItem.lootTableItem(Items.SADDLE).setWeight(3))
                  .add(LootItem.lootTableItem(Items.COPPER_HORSE_ARMOR))
                  .add(LootItem.lootTableItem(Items.IRON_HORSE_ARMOR))
                  .add(LootItem.lootTableItem(Items.GOLDEN_HORSE_ARMOR))
                  .add(LootItem.lootTableItem(Items.DIAMOND_HORSE_ARMOR))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.BUNDLE).setWeight(1).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(EmptyLootItem.emptyItem().setWeight(2))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_TOOLSMITH,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(3.0F, 8.0F))
                  .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                  .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.IRON_PICKAXE).setWeight(5))
                  .add(LootItem.lootTableItem(Items.COAL).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.STICK).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.IRON_SHOVEL).setWeight(5))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_CARTOGRAPHER,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 5.0F))
                  .add(LootItem.lootTableItem(Items.MAP).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.PAPER).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                  .add(LootItem.lootTableItem(Items.COMPASS).setWeight(5))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.STICK).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.BUNDLE).setWeight(1).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(EmptyLootItem.emptyItem().setWeight(2))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_MASON,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 5.0F))
                  .add(LootItem.lootTableItem(Items.CLAY_BALL).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.FLOWER_POT).setWeight(1))
                  .add(LootItem.lootTableItem(Blocks.STONE).setWeight(2))
                  .add(LootItem.lootTableItem(Blocks.STONE_BRICKS).setWeight(2))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.YELLOW_DYE).setWeight(1))
                  .add(LootItem.lootTableItem(Blocks.SMOOTH_STONE).setWeight(1))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(1))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_ARMORER,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 5.0F))
                  .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.IRON_HELMET).setWeight(1))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(1))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_SHEPHERD,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 5.0F))
                  .add(LootItem.lootTableItem(Blocks.WHITE_WOOL).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                  .add(LootItem.lootTableItem(Blocks.BLACK_WOOL).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Blocks.GRAY_WOOL).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Blocks.BROWN_WOOL).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Blocks.LIGHT_GRAY_WOOL).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(1))
                  .add(LootItem.lootTableItem(Items.SHEARS).setWeight(1))
                  .add(LootItem.lootTableItem(Items.WHEAT).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_BUTCHER,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 5.0F))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(1))
                  .add(LootItem.lootTableItem(Items.PORKCHOP).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.WHEAT).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.BEEF).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.MUTTON).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.COAL).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_FLETCHER,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 5.0F))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(1))
                  .add(LootItem.lootTableItem(Items.ARROW).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.FEATHER).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.EGG).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.FLINT).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.STICK).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_FISHER,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 5.0F))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(1))
                  .add(LootItem.lootTableItem(Items.COD).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.SALMON).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.WATER_BUCKET).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.BARREL).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.WHEAT_SEEDS).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.COAL).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_TANNERY,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 5.0F))
                  .add(LootItem.lootTableItem(Items.LEATHER).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.LEATHER_CHESTPLATE).setWeight(2))
                  .add(LootItem.lootTableItem(Items.LEATHER_BOOTS).setWeight(2))
                  .add(LootItem.lootTableItem(Items.LEATHER_HELMET).setWeight(2))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.LEATHER_LEGGINGS).setWeight(2))
                  .add(LootItem.lootTableItem(Items.SADDLE).setWeight(1))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.BUNDLE).setWeight(1).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(EmptyLootItem.emptyItem().setWeight(2))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_TEMPLE,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(3.0F, 8.0F))
                  .add(LootItem.lootTableItem(Items.REDSTONE).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(7).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(7).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.LAPIS_LAZULI).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_PLAINS_HOUSE,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(3.0F, 8.0F))
                  .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.DANDELION).setWeight(2))
                  .add(LootItem.lootTableItem(Items.POPPY).setWeight(1))
                  .add(LootItem.lootTableItem(Items.POTATO).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 7.0F))))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.APPLE).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                  .add(LootItem.lootTableItem(Items.BOOK).setWeight(1))
                  .add(LootItem.lootTableItem(Items.FEATHER).setWeight(1))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Blocks.OAK_SAPLING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.BUNDLE).setWeight(1).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(EmptyLootItem.emptyItem().setWeight(2))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_TAIGA_HOUSE,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(3.0F, 8.0F))
                  .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                  .add(LootItem.lootTableItem(Items.FERN).setWeight(2))
                  .add(LootItem.lootTableItem(Items.LARGE_FERN).setWeight(2))
                  .add(LootItem.lootTableItem(Items.POTATO).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 7.0F))))
                  .add(LootItem.lootTableItem(Items.SWEET_BERRIES).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 7.0F))))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.PUMPKIN_SEEDS).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                  .add(LootItem.lootTableItem(Items.PUMPKIN_PIE).setWeight(1))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Blocks.SPRUCE_SAPLING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                  .add(LootItem.lootTableItem(Items.SPRUCE_SIGN).setWeight(1))
                  .add(LootItem.lootTableItem(Items.SPRUCE_LOG).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.BUNDLE).setWeight(1).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(EmptyLootItem.emptyItem().setWeight(2))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_SAVANNA_HOUSE,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(3.0F, 8.0F))
                  .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.SHORT_GRASS).setWeight(5))
                  .add(LootItem.lootTableItem(Items.TALL_GRASS).setWeight(5))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.WHEAT_SEEDS).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Blocks.ACACIA_SAPLING).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                  .add(LootItem.lootTableItem(Items.SADDLE).setWeight(1))
                  .add(LootItem.lootTableItem(Blocks.TORCH).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                  .add(LootItem.lootTableItem(Items.BUCKET).setWeight(1))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.BUNDLE).setWeight(1).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(EmptyLootItem.emptyItem().setWeight(2))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_SNOWY_HOUSE,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(3.0F, 8.0F))
                  .add(LootItem.lootTableItem(Blocks.BLUE_ICE).setWeight(1))
                  .add(LootItem.lootTableItem(Blocks.SNOW_BLOCK).setWeight(4))
                  .add(LootItem.lootTableItem(Items.POTATO).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 7.0F))))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.BEETROOT_SEEDS).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                  .add(LootItem.lootTableItem(Items.BEETROOT_SOUP).setWeight(1))
                  .add(LootItem.lootTableItem(Items.FURNACE).setWeight(1))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.SNOWBALL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 7.0F))))
                  .add(LootItem.lootTableItem(Items.COAL).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.BUNDLE).setWeight(1).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(EmptyLootItem.emptyItem().setWeight(2))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.VILLAGE_DESERT_HOUSE,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(3.0F, 8.0F))
                  .add(LootItem.lootTableItem(Items.CLAY_BALL).setWeight(1))
                  .add(LootItem.lootTableItem(Items.GREEN_DYE).setWeight(1))
                  .add(LootItem.lootTableItem(Blocks.CACTUS).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.WHEAT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 7.0F))))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.BOOK).setWeight(1))
                  .add(LootItem.lootTableItem(Blocks.DEAD_BUSH).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.BUNDLE).setWeight(1).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(EmptyLootItem.emptyItem().setWeight(2))
            )
      );
      p_250931_.accept(BuiltInLootTables.WOODLAND_MANSION, this.woodlandMansionLootTable());
      p_250931_.accept(
         BuiltInLootTables.RUINED_PORTAL,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(4.0F, 8.0F))
                  .add(LootItem.lootTableItem(Items.OBSIDIAN).setWeight(40).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                  .add(LootItem.lootTableItem(Items.FLINT).setWeight(40).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(40).apply(SetItemCountFunction.setCount(UniformGenerator.between(9.0F, 18.0F))))
                  .add(LootItem.lootTableItem(Items.FLINT_AND_STEEL).setWeight(40))
                  .add(LootItem.lootTableItem(Items.FIRE_CHARGE).setWeight(40))
                  .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(15))
                  .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 24.0F))))
                  .add(LootItem.lootTableItem(Items.GOLDEN_SWORD).setWeight(15).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
                  .add(LootItem.lootTableItem(Items.GOLDEN_AXE).setWeight(15).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
                  .add(LootItem.lootTableItem(Items.GOLDEN_HOE).setWeight(15).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
                  .add(LootItem.lootTableItem(Items.GOLDEN_SHOVEL).setWeight(15).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
                  .add(LootItem.lootTableItem(Items.GOLDEN_PICKAXE).setWeight(15).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
                  .add(LootItem.lootTableItem(Items.GOLDEN_BOOTS).setWeight(15).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
                  .add(
                     LootItem.lootTableItem(Items.GOLDEN_CHESTPLATE).setWeight(15).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
                  )
                  .add(LootItem.lootTableItem(Items.GOLDEN_HELMET).setWeight(15).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
                  .add(LootItem.lootTableItem(Items.GOLDEN_LEGGINGS).setWeight(15).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
                  .add(
                     LootItem.lootTableItem(Items.GLISTERING_MELON_SLICE)
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 12.0F)))
                  )
                  .add(LootItem.lootTableItem(Items.GOLDEN_HORSE_ARMOR).setWeight(5))
                  .add(LootItem.lootTableItem(Items.LIGHT_WEIGHTED_PRESSURE_PLATE).setWeight(5))
                  .add(LootItem.lootTableItem(Items.GOLDEN_CARROT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 12.0F))))
                  .add(LootItem.lootTableItem(Items.CLOCK).setWeight(5))
                  .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                  .add(LootItem.lootTableItem(Items.BELL).setWeight(1))
                  .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).setWeight(1))
                  .add(LootItem.lootTableItem(Items.GOLD_BLOCK).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(EmptyLootItem.emptyItem().setWeight(1))
                  .add(LootItem.lootTableItem(Items.LODESTONE).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_CORRIDOR_DISPENSER,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.ARROW).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F))))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_WATER_DISPENSER,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.WATER_BUCKET).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_CHAMBER_DISPENSER,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.WATER_BUCKET).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))).setWeight(4))
                  .add(LootItem.lootTableItem(Items.ARROW).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F))).setWeight(4))
                  .add(LootItem.lootTableItem(Items.SNOWBALL).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F))).setWeight(6))
                  .add(LootItem.lootTableItem(Items.EGG).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F))).setWeight(2))
                  .add(LootItem.lootTableItem(Items.FIRE_CHARGE).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F))).setWeight(6))
                  .add(
                     LootItem.lootTableItem(Items.SPLASH_POTION)
                        .apply(SetPotionFunction.setPotion(Potions.SLOWNESS))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
                        .setWeight(1)
                  )
                  .add(
                     LootItem.lootTableItem(Items.SPLASH_POTION)
                        .apply(SetPotionFunction.setPotion(Potions.POISON))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
                        .setWeight(1)
                  )
                  .add(
                     LootItem.lootTableItem(Items.SPLASH_POTION)
                        .apply(SetPotionFunction.setPotion(Potions.WEAKNESS))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
                        .setWeight(1)
                  )
                  .add(
                     LootItem.lootTableItem(Items.LINGERING_POTION)
                        .apply(SetPotionFunction.setPotion(Potions.SLOWNESS))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
                        .setWeight(1)
                  )
                  .add(
                     LootItem.lootTableItem(Items.LINGERING_POTION)
                        .apply(SetPotionFunction.setPotion(Potions.POISON))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
                        .setWeight(1)
                  )
                  .add(
                     LootItem.lootTableItem(Items.LINGERING_POTION)
                        .apply(SetPotionFunction.setPotion(Potions.WEAKNESS))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
                        .setWeight(1)
                  )
                  .add(
                     LootItem.lootTableItem(Items.LINGERING_POTION)
                        .apply(SetPotionFunction.setPotion(Potions.HEALING))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
                        .setWeight(1)
                  )
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_CORRIDOR_POT,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.EMERALD).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))).setWeight(125))
                  .add(LootItem.lootTableItem(Items.ARROW).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))).setWeight(100))
                  .add(LootItem.lootTableItem(Items.IRON_INGOT).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))).setWeight(100))
                  .add(LootItem.lootTableItem(Items.TRIAL_KEY).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))).setWeight(10))
                  .add(LootItem.lootTableItem(Items.MUSIC_DISC_CREATOR_MUSIC_BOX).setWeight(5))
                  .add(LootItem.lootTableItem(Items.DIAMOND).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))).setWeight(5))
                  .add(LootItem.lootTableItem(Items.EMERALD_BLOCK).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))).setWeight(5))
                  .add(LootItem.lootTableItem(Items.DIAMOND_BLOCK).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))).setWeight(1))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_SUPPLY,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(3.0F, 5.0F))
                  .add(LootItem.lootTableItem(Items.ARROW).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 14.0F))).setWeight(2))
                  .add(
                     LootItem.lootTableItem(Items.TIPPED_ARROW)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.POISON))
                        .setWeight(1)
                  )
                  .add(
                     LootItem.lootTableItem(Items.TIPPED_ARROW)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.SLOWNESS))
                        .setWeight(1)
                  )
                  .add(LootItem.lootTableItem(Items.BAKED_POTATO).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))).setWeight(2))
                  .add(LootItem.lootTableItem(Items.GLOW_BERRIES).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 10.0F))).setWeight(2))
                  .add(LootItem.lootTableItem(Items.ACACIA_PLANKS).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 6.0F))).setWeight(1))
                  .add(LootItem.lootTableItem(Items.MOSS_BLOCK).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))).setWeight(1))
                  .add(LootItem.lootTableItem(Items.BONE_MEAL).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))).setWeight(1))
                  .add(LootItem.lootTableItem(Items.TUFF).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 10.0F))).setWeight(1))
                  .add(LootItem.lootTableItem(Items.TORCH).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 6.0F))).setWeight(1))
                  .add(
                     LootItem.lootTableItem(Items.POTION)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.REGENERATION))
                  )
                  .add(
                     LootItem.lootTableItem(Items.POTION)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.STRENGTH))
                  )
                  .add(
                     LootItem.lootTableItem(Items.STONE_PICKAXE)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.15F, 0.8F)))
                        .setWeight(2)
                  )
                  .add(LootItem.lootTableItem(Items.MILK_BUCKET).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_ENTRANCE,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(2.0F, 3.0F))
                  .add(LootItem.lootTableItem(Items.TRIAL_KEY).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))).setWeight(1))
                  .add(LootItem.lootTableItem(Items.STICK).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))).setWeight(5))
                  .add(LootItem.lootTableItem(Items.WOODEN_AXE).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))).setWeight(10))
                  .add(LootItem.lootTableItem(Items.HONEYCOMB).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))).setWeight(10))
                  .add(LootItem.lootTableItem(Items.ARROW).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 10.0F))).setWeight(10))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_INTERSECTION,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 3.0F))
                  .add(LootItem.lootTableItem(Items.DIAMOND_BLOCK).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))).setWeight(1))
                  .add(LootItem.lootTableItem(Items.EMERALD_BLOCK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))).setWeight(5))
                  .add(
                     LootItem.lootTableItem(Items.DIAMOND_AXE)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.1F, 0.5F)))
                        .setWeight(5)
                  )
                  .add(
                     LootItem.lootTableItem(Items.DIAMOND_PICKAXE)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.1F, 0.5F)))
                        .setWeight(5)
                  )
                  .add(LootItem.lootTableItem(Items.DIAMOND).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))).setWeight(10))
                  .add(LootItem.lootTableItem(Items.CAKE).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))).setWeight(20))
                  .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).apply(SetItemCountFunction.setCount(UniformGenerator.between(8.0F, 20.0F))).setWeight(20))
                  .add(LootItem.lootTableItem(Items.IRON_BLOCK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))).setWeight(20))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_INTERSECTION_BARREL,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 3.0F))
                  .add(
                     LootItem.lootTableItem(Items.DIAMOND_AXE)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.4F, 0.9F)))
                        .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
                        .setWeight(1)
                  )
                  .add(
                     LootItem.lootTableItem(Items.DIAMOND_PICKAXE)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.15F, 0.8F)))
                        .setWeight(1)
                  )
                  .add(LootItem.lootTableItem(Items.DIAMOND).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))).setWeight(1))
                  .add(
                     LootItem.lootTableItem(Items.COMPASS)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.15F, 0.8F)))
                        .setWeight(1)
                  )
                  .add(LootItem.lootTableItem(Items.BUCKET).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))).setWeight(1))
                  .add(
                     LootItem.lootTableItem(Items.GOLDEN_AXE)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.15F, 0.8F)))
                        .setWeight(4)
                  )
                  .add(
                     LootItem.lootTableItem(Items.GOLDEN_PICKAXE)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.15F, 0.8F)))
                        .setWeight(4)
                  )
                  .add(LootItem.lootTableItem(Items.BAMBOO_PLANKS).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 15.0F))).setWeight(10))
                  .add(LootItem.lootTableItem(Items.BAKED_POTATO).apply(SetItemCountFunction.setCount(UniformGenerator.between(6.0F, 10.0F))).setWeight(10))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_CORRIDOR,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 3.0F))
                  .add(
                     LootItem.lootTableItem(Items.IRON_AXE)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.4F, 0.9F)))
                        .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
                        .setWeight(1)
                  )
                  .add(LootItem.lootTableItem(Items.HONEYCOMB).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))).setWeight(1))
                  .add(
                     LootItem.lootTableItem(Items.STONE_AXE)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.15F, 0.8F)))
                        .setWeight(2)
                  )
                  .add(
                     LootItem.lootTableItem(Items.STONE_PICKAXE)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.15F, 0.8F)))
                        .setWeight(2)
                  )
                  .add(LootItem.lootTableItem(Items.ENDER_PEARL).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))).setWeight(2))
                  .add(
                     LootItem.lootTableItem(Items.BAMBOO_HANGING_SIGN).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))).setWeight(2)
                  )
                  .add(LootItem.lootTableItem(Items.BAMBOO_PLANKS).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 6.0F))).setWeight(2))
                  .add(LootItem.lootTableItem(Items.SCAFFOLDING).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 10.0F))).setWeight(2))
                  .add(LootItem.lootTableItem(Items.TORCH).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 6.0F))).setWeight(2))
                  .add(LootItem.lootTableItem(Items.TUFF).apply(SetItemCountFunction.setCount(UniformGenerator.between(8.0F, 20.0F))).setWeight(3))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.SHIELD).setWeight(3).apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.5F, 1.0F))))
                  .add(
                     LootItem.lootTableItem(Items.BOW)
                        .setWeight(3)
                        .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(5.0F, 15.0F)))
                  )
                  .add(
                     LootItem.lootTableItem(Items.CROSSBOW)
                        .setWeight(2)
                        .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(5.0F, 20.0F)))
                  )
                  .add(
                     LootItem.lootTableItem(Items.IRON_AXE)
                        .setWeight(2)
                        .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(0.0F, 10.0F)))
                  )
                  .add(
                     LootItem.lootTableItem(Items.IRON_CHESTPLATE)
                        .setWeight(2)
                        .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(0.0F, 10.0F)))
                  )
                  .add(LootItem.lootTableItem(Items.GOLDEN_CARROT).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                  .add(
                     LootItem.lootTableItem(Items.BOOK)
                        .setWeight(2)
                        .apply(
                           new EnchantRandomlyFunction.Builder()
                              .withOneOf(
                                 HolderSet.direct(
                                    registrylookup.getOrThrow(Enchantments.SHARPNESS),
                                    registrylookup.getOrThrow(Enchantments.BANE_OF_ARTHROPODS),
                                    registrylookup.getOrThrow(Enchantments.EFFICIENCY),
                                    registrylookup.getOrThrow(Enchantments.FORTUNE),
                                    registrylookup.getOrThrow(Enchantments.SILK_TOUCH),
                                    registrylookup.getOrThrow(Enchantments.FEATHER_FALLING)
                                 )
                              )
                        )
                  )
                  .add(
                     LootItem.lootTableItem(Items.BOOK)
                        .setWeight(2)
                        .apply(
                           new EnchantRandomlyFunction.Builder()
                              .withOneOf(
                                 HolderSet.direct(
                                    registrylookup.getOrThrow(Enchantments.RIPTIDE),
                                    registrylookup.getOrThrow(Enchantments.LOYALTY),
                                    registrylookup.getOrThrow(Enchantments.CHANNELING),
                                    registrylookup.getOrThrow(Enchantments.IMPALING),
                                    registrylookup.getOrThrow(Enchantments.MENDING)
                                 )
                              )
                        )
                  )
                  .add(
                     LootItem.lootTableItem(Items.DIAMOND_CHESTPLATE)
                        .setWeight(1)
                        .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(5.0F, 15.0F)))
                  )
                  .add(
                     LootItem.lootTableItem(Items.DIAMOND_AXE)
                        .setWeight(1)
                        .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(5.0F, 15.0F)))
                  )
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.ARROW).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                  .add(
                     LootItem.lootTableItem(Items.TIPPED_ARROW)
                        .setWeight(4)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.POISON))
                  )
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.WIND_CHARGE).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.HONEY_BOTTLE).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                  .add(
                     LootItem.lootTableItem(Items.OMINOUS_BOTTLE)
                        .setWeight(2)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetOminousBottleAmplifierFunction.setAmplifier(UniformGenerator.between(0.0F, 1.0F)))
                  )
                  .add(LootItem.lootTableItem(Items.WIND_CHARGE).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 12.0F))))
                  .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_REWARD_UNIQUE,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(4))
                  .add(LootItem.lootTableItem(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE).setWeight(3))
                  .add(LootItem.lootTableItem(Items.GUSTER_BANNER_PATTERN).setWeight(2))
                  .add(LootItem.lootTableItem(Items.MUSIC_DISC_PRECIPICE).setWeight(2))
                  .add(LootItem.lootTableItem(Items.TRIDENT).setWeight(1))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_REWARD,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(NestedLootTable.lootTableReference(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE).setWeight(8))
                  .add(NestedLootTable.lootTableReference(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON).setWeight(2))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 3.0F))
                  .add(NestedLootTable.lootTableReference(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .when(LootItemRandomChanceCondition.randomChance(0.25F))
                  .add(NestedLootTable.lootTableReference(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_UNIQUE))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.EMERALD_BLOCK).setWeight(5))
                  .add(LootItem.lootTableItem(Items.IRON_BLOCK).setWeight(4))
                  .add(
                     LootItem.lootTableItem(Items.CROSSBOW)
                        .setWeight(4)
                        .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(5.0F, 20.0F)))
                  )
                  .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(3))
                  .add(
                     LootItem.lootTableItem(Items.DIAMOND_AXE)
                        .setWeight(3)
                        .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(10.0F, 20.0F)))
                  )
                  .add(
                     LootItem.lootTableItem(Items.DIAMOND_CHESTPLATE)
                        .setWeight(3)
                        .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(10.0F, 20.0F)))
                  )
                  .add(
                     LootItem.lootTableItem(Items.BOOK)
                        .setWeight(2)
                        .apply(
                           new EnchantRandomlyFunction.Builder()
                              .withOneOf(
                                 HolderSet.direct(
                                    registrylookup.getOrThrow(Enchantments.KNOCKBACK),
                                    registrylookup.getOrThrow(Enchantments.PUNCH),
                                    registrylookup.getOrThrow(Enchantments.SMITE),
                                    registrylookup.getOrThrow(Enchantments.LOOTING),
                                    registrylookup.getOrThrow(Enchantments.MULTISHOT)
                                 )
                              )
                        )
                  )
                  .add(
                     LootItem.lootTableItem(Items.BOOK)
                        .setWeight(2)
                        .apply(
                           new EnchantRandomlyFunction.Builder()
                              .withOneOf(HolderSet.direct(registrylookup.getOrThrow(Enchantments.BREACH), registrylookup.getOrThrow(Enchantments.DENSITY)))
                        )
                  )
                  .add(
                     LootItem.lootTableItem(Items.BOOK)
                        .setWeight(2)
                        .apply(
                           new SetEnchantmentsFunction.Builder()
                              .withEnchantment(registrylookup.getOrThrow(Enchantments.WIND_BURST), ConstantValue.exactly(1.0F))
                        )
                  )
                  .add(LootItem.lootTableItem(Items.DIAMOND_BLOCK).setWeight(1))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_COMMON,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.EMERALD).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 10.0F))))
                  .add(LootItem.lootTableItem(Items.WIND_CHARGE).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(8.0F, 12.0F))))
                  .add(
                     LootItem.lootTableItem(Items.TIPPED_ARROW)
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 12.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.STRONG_SLOWNESS))
                  )
                  .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 3.0F))))
                  .add(
                     LootItem.lootTableItem(Items.OMINOUS_BOTTLE)
                        .setWeight(1)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetOminousBottleAmplifierFunction.setAmplifier(UniformGenerator.between(2.0F, 4.0F)))
                  )
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_UNIQUE,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).setWeight(3))
                  .add(LootItem.lootTableItem(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE).setWeight(3))
                  .add(LootItem.lootTableItem(Items.FLOW_BANNER_PATTERN).setWeight(2))
                  .add(LootItem.lootTableItem(Items.MUSIC_DISC_CREATOR).setWeight(1))
                  .add(LootItem.lootTableItem(Items.HEAVY_CORE).setWeight(1))
            )
      );
      p_250931_.accept(
         BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(NestedLootTable.lootTableReference(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE).setWeight(8))
                  .add(NestedLootTable.lootTableReference(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_COMMON).setWeight(2))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(UniformGenerator.between(1.0F, 3.0F))
                  .add(NestedLootTable.lootTableReference(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_COMMON))
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .when(LootItemRandomChanceCondition.randomChance(0.75F))
                  .add(NestedLootTable.lootTableReference(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_UNIQUE))
            )
      );
      this.spawnerLootTables(p_250931_);
   }

   public void spawnerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> p_312949_) {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      p_312949_.accept(
         BuiltInLootTables.SPAWNER_TRIAL_CHAMBER_KEY,
         LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.TRIAL_KEY)))
      );
      p_312949_.accept(
         BuiltInLootTables.SPAWNER_TRIAL_CHAMBER_CONSUMABLES,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.COOKED_CHICKEN).setWeight(3).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(LootItem.lootTableItem(Items.BREAD).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.BAKED_POTATO).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(
                     LootItem.lootTableItem(Items.POTION)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.REGENERATION))
                  )
                  .add(
                     LootItem.lootTableItem(Items.POTION)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.SWIFTNESS))
                  )
            )
      );
      p_312949_.accept(
         BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY,
         LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.OMINOUS_TRIAL_KEY)))
      );
      p_312949_.accept(
         BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.COOKED_BEEF).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                  .add(LootItem.lootTableItem(Items.BAKED_POTATO).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
                  .add(LootItem.lootTableItem(Items.GOLDEN_CARROT).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                  .add(
                     LootItem.lootTableItem(Items.POTION)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.REGENERATION))
                  )
                  .add(
                     LootItem.lootTableItem(Items.POTION)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.STRENGTH))
                  )
            )
      );
      p_312949_.accept(
         BuiltInLootTables.SPAWNER_TRIAL_ITEMS_TO_DROP_WHEN_OMINOUS,
         LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(
                     LootItem.lootTableItem(Items.LINGERING_POTION)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.WIND_CHARGED))
                  )
                  .add(
                     LootItem.lootTableItem(Items.LINGERING_POTION)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.OOZING))
                  )
                  .add(
                     LootItem.lootTableItem(Items.LINGERING_POTION)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.WEAVING))
                  )
                  .add(
                     LootItem.lootTableItem(Items.LINGERING_POTION)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.INFESTED))
                  )
                  .add(
                     LootItem.lootTableItem(Items.LINGERING_POTION)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.STRENGTH))
                  )
                  .add(
                     LootItem.lootTableItem(Items.LINGERING_POTION)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.SWIFTNESS))
                  )
                  .add(
                     LootItem.lootTableItem(Items.LINGERING_POTION)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.SLOW_FALLING))
                  )
            )
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(Items.ARROW).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                  .add(
                     LootItem.lootTableItem(Items.ARROW)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.POISON))
                  )
                  .add(
                     LootItem.lootTableItem(Items.ARROW)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                        .apply(SetPotionFunction.setPotion(Potions.STRONG_SLOWNESS))
                  )
                  .add(LootItem.lootTableItem(Items.FIRE_CHARGE).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                  .add(LootItem.lootTableItem(Items.WIND_CHARGE).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
            )
      );
   }

   public LootTable.Builder shipwreckSupplyLootTable() {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(3.0F, 10.0F))
               .add(LootItem.lootTableItem(Items.PAPER).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 12.0F))))
               .add(LootItem.lootTableItem(Items.POTATO).setWeight(7).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
               .add(LootItem.lootTableItem(Items.MOSS_BLOCK).setWeight(7).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
               .add(LootItem.lootTableItem(Items.POISONOUS_POTATO).setWeight(7).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
               .add(LootItem.lootTableItem(Items.CARROT).setWeight(7).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F))))
               .add(LootItem.lootTableItem(Items.WHEAT).setWeight(7).apply(SetItemCountFunction.setCount(UniformGenerator.between(8.0F, 21.0F))))
               .add(
                  LootItem.lootTableItem(Items.SUSPICIOUS_STEW)
                     .setWeight(10)
                     .apply(
                        SetStewEffectFunction.stewEffect()
                           .withEffect(MobEffects.NIGHT_VISION, UniformGenerator.between(7.0F, 10.0F))
                           .withEffect(MobEffects.JUMP_BOOST, UniformGenerator.between(7.0F, 10.0F))
                           .withEffect(MobEffects.WEAKNESS, UniformGenerator.between(6.0F, 8.0F))
                           .withEffect(MobEffects.BLINDNESS, UniformGenerator.between(5.0F, 7.0F))
                           .withEffect(MobEffects.POISON, UniformGenerator.between(10.0F, 20.0F))
                           .withEffect(MobEffects.SATURATION, UniformGenerator.between(7.0F, 10.0F))
                     )
               )
               .add(LootItem.lootTableItem(Items.COAL).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
               .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 24.0F))))
               .add(LootItem.lootTableItem(Blocks.PUMPKIN).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Blocks.BAMBOO).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.GUNPOWDER).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Blocks.TNT).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
               .add(LootItem.lootTableItem(Items.LEATHER_HELMET).setWeight(3).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
               .add(LootItem.lootTableItem(Items.LEATHER_CHESTPLATE).setWeight(3).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
               .add(LootItem.lootTableItem(Items.LEATHER_LEGGINGS).setWeight(3).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
               .add(LootItem.lootTableItem(Items.LEATHER_BOOTS).setWeight(3).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(5))
               .add(
                  LootItem.lootTableItem(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE)
                     .setWeight(1)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)))
               )
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(148))
               .add(LootItem.lootTableItem(Items.COPPER_NAUTILUS_ARMOR).setWeight(20).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.IRON_NAUTILUS_ARMOR).setWeight(10).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.GOLDEN_NAUTILUS_ARMOR).setWeight(5).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.DIAMOND_NAUTILUS_ARMOR).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
         );
   }

   public LootTable.Builder shipwreckMapLootTable() {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(
                  LootItem.lootTableItem(Items.MAP)
                     .apply(
                        ExplorationMapFunction.makeExplorationMap()
                           .setDestination(StructureTags.ON_TREASURE_MAPS)
                           .setMapDecoration(MapDecorationTypes.RED_X)
                           .setZoom((byte)1)
                           .setSkipKnownStructures(false)
                     )
                     .apply(SetNameFunction.setName(Component.translatable("filled_map.buried_treasure"), SetNameFunction.Target.ITEM_NAME))
               )
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(3.0F))
               .add(LootItem.lootTableItem(Items.COMPASS))
               .add(LootItem.lootTableItem(Items.MAP))
               .add(LootItem.lootTableItem(Items.CLOCK))
               .add(LootItem.lootTableItem(Items.PAPER).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 10.0F))))
               .add(LootItem.lootTableItem(Items.FEATHER).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.BOOK).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(5))
               .add(
                  LootItem.lootTableItem(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE)
                     .setWeight(1)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)))
               )
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(148))
               .add(LootItem.lootTableItem(Items.COPPER_NAUTILUS_ARMOR).setWeight(20).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.IRON_NAUTILUS_ARMOR).setWeight(10).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.GOLDEN_NAUTILUS_ARMOR).setWeight(5).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.DIAMOND_NAUTILUS_ARMOR).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
         );
   }

   public LootTable.Builder bastionHoglinStableLootTable() {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_SHOVEL)
                     .setWeight(15)
                     .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.15F, 0.8F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_PICKAXE)
                     .setWeight(12)
                     .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.15F, 0.95F)))
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(LootItem.lootTableItem(Items.NETHERITE_SCRAP).setWeight(8).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.ANCIENT_DEBRIS).setWeight(12).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.ANCIENT_DEBRIS).setWeight(5).apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F))))
               .add(LootItem.lootTableItem(Items.SADDLE).setWeight(12).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Blocks.GOLD_BLOCK).setWeight(16).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
               .add(LootItem.lootTableItem(Items.GOLDEN_CARROT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(8.0F, 17.0F))))
               .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(10).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(3.0F, 4.0F))
               .add(
                  LootItem.lootTableItem(Items.GOLDEN_AXE)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(LootItem.lootTableItem(Blocks.CRYING_OBSIDIAN).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Blocks.GLOWSTONE).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 6.0F))))
               .add(LootItem.lootTableItem(Blocks.GILDED_BLACKSTONE).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
               .add(LootItem.lootTableItem(Blocks.SOUL_SAND).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))
               .add(LootItem.lootTableItem(Blocks.CRIMSON_NYLIUM).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))
               .add(LootItem.lootTableItem(Items.GOLD_NUGGET).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
               .add(LootItem.lootTableItem(Items.LEATHER).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.ARROW).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 17.0F))))
               .add(LootItem.lootTableItem(Items.STRING).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
               .add(LootItem.lootTableItem(Items.PORKCHOP).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.COOKED_PORKCHOP).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
               .add(LootItem.lootTableItem(Blocks.CRIMSON_FUNGUS).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))
               .add(LootItem.lootTableItem(Blocks.CRIMSON_ROOTS).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(11))
               .add(LootItem.lootTableItem(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE).setWeight(1))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(9))
               .add(LootItem.lootTableItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).setWeight(1))
         );
   }

   public LootTable.Builder bastionBridgeLootTable() {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(LootItem.lootTableItem(Blocks.LODESTONE).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(1.0F, 2.0F))
               .add(
                  LootItem.lootTableItem(Items.CROSSBOW)
                     .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.1F, 0.5F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(LootItem.lootTableItem(Items.SPECTRAL_ARROW).apply(SetItemCountFunction.setCount(UniformGenerator.between(10.0F, 28.0F))))
               .add(LootItem.lootTableItem(Blocks.GILDED_BLACKSTONE).apply(SetItemCountFunction.setCount(UniformGenerator.between(8.0F, 12.0F))))
               .add(LootItem.lootTableItem(Blocks.CRYING_OBSIDIAN).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
               .add(LootItem.lootTableItem(Blocks.GOLD_BLOCK).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.GOLD_INGOT).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 9.0F))))
               .add(LootItem.lootTableItem(Items.IRON_INGOT).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 9.0F))))
               .add(LootItem.lootTableItem(Items.GOLDEN_SWORD).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(
                  LootItem.lootTableItem(Items.GOLDEN_CHESTPLATE)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(
                  LootItem.lootTableItem(Items.GOLDEN_HELMET)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(
                  LootItem.lootTableItem(Items.GOLDEN_LEGGINGS)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(
                  LootItem.lootTableItem(Items.GOLDEN_BOOTS)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(
                  LootItem.lootTableItem(Items.GOLDEN_AXE)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(2.0F, 4.0F))
               .add(LootItem.lootTableItem(Items.STRING).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
               .add(LootItem.lootTableItem(Items.LEATHER).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.ARROW).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 17.0F))))
               .add(LootItem.lootTableItem(Items.IRON_NUGGET).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
               .add(LootItem.lootTableItem(Items.GOLD_NUGGET).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(11))
               .add(LootItem.lootTableItem(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE).setWeight(1))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(9))
               .add(LootItem.lootTableItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).setWeight(1))
         );
   }

   public LootTable.Builder endCityTreasureLootTable() {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(2.0F, 6.0F))
               .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))
               .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F))))
               .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))
               .add(LootItem.lootTableItem(Items.EMERALD).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
               .add(LootItem.lootTableItem(Items.BEETROOT_SEEDS).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 10.0F))))
               .add(LootItem.lootTableItem(Items.SADDLE).setWeight(3))
               .add(LootItem.lootTableItem(Items.COPPER_HORSE_ARMOR))
               .add(LootItem.lootTableItem(Items.IRON_HORSE_ARMOR))
               .add(LootItem.lootTableItem(Items.GOLDEN_HORSE_ARMOR))
               .add(LootItem.lootTableItem(Items.DIAMOND_HORSE_ARMOR))
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_SWORD)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_SPEAR)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_BOOTS)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_CHESTPLATE)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_LEGGINGS)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_HELMET)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_PICKAXE)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_SHOVEL)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.IRON_SWORD)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.IRON_BOOTS)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.IRON_CHESTPLATE)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.IRON_LEGGINGS)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.IRON_HELMET)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.IRON_PICKAXE)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.IRON_SHOVEL)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(14))
               .add(LootItem.lootTableItem(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE).setWeight(1))
         );
   }

   public LootTable.Builder netherBridgeLootTable() {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(2.0F, 4.0F))
               .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.GOLDEN_SWORD).setWeight(5))
               .add(LootItem.lootTableItem(Items.GOLDEN_CHESTPLATE).setWeight(5))
               .add(LootItem.lootTableItem(Items.FLINT_AND_STEEL).setWeight(5))
               .add(LootItem.lootTableItem(Items.NETHER_WART).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 7.0F))))
               .add(LootItem.lootTableItem(Items.SADDLE).setWeight(10))
               .add(LootItem.lootTableItem(Items.GOLDEN_HORSE_ARMOR).setWeight(8))
               .add(LootItem.lootTableItem(Items.COPPER_HORSE_ARMOR).setWeight(5))
               .add(LootItem.lootTableItem(Items.IRON_HORSE_ARMOR).setWeight(5))
               .add(LootItem.lootTableItem(Items.DIAMOND_HORSE_ARMOR).setWeight(3))
               .add(LootItem.lootTableItem(Blocks.OBSIDIAN).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(14))
               .add(LootItem.lootTableItem(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE).setWeight(1))
         );
   }

   public LootTable.Builder bastionTreasureLootTable() {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(3.0F))
               .add(LootItem.lootTableItem(Items.NETHERITE_INGOT).setWeight(15).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Blocks.ANCIENT_DEBRIS).setWeight(10).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.NETHERITE_SCRAP).setWeight(8).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Blocks.ANCIENT_DEBRIS).setWeight(4).apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F))))
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_SWORD)
                     .setWeight(6)
                     .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.8F, 1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_SPEAR)
                     .setWeight(6)
                     .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.8F, 1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_CHESTPLATE)
                     .setWeight(6)
                     .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.8F, 1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_HELMET)
                     .setWeight(6)
                     .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.8F, 1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_LEGGINGS)
                     .setWeight(6)
                     .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.8F, 1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_BOOTS)
                     .setWeight(6)
                     .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.8F, 1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(LootItem.lootTableItem(Items.DIAMOND_SWORD).setWeight(6))
               .add(LootItem.lootTableItem(Items.DIAMOND_SPEAR).setWeight(6))
               .add(LootItem.lootTableItem(Items.DIAMOND_CHESTPLATE).setWeight(5))
               .add(LootItem.lootTableItem(Items.DIAMOND_HELMET).setWeight(5))
               .add(LootItem.lootTableItem(Items.DIAMOND_BOOTS).setWeight(5))
               .add(LootItem.lootTableItem(Items.DIAMOND_LEGGINGS).setWeight(5))
               .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
               .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(3.0F, 4.0F))
               .add(LootItem.lootTableItem(Items.SPECTRAL_ARROW).apply(SetItemCountFunction.setCount(UniformGenerator.between(12.0F, 25.0F))))
               .add(LootItem.lootTableItem(Blocks.GOLD_BLOCK).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
               .add(LootItem.lootTableItem(Blocks.IRON_BLOCK).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.GOLD_INGOT).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 9.0F))))
               .add(LootItem.lootTableItem(Items.IRON_INGOT).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 9.0F))))
               .add(LootItem.lootTableItem(Blocks.CRYING_OBSIDIAN).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.QUARTZ).apply(SetItemCountFunction.setCount(UniformGenerator.between(8.0F, 23.0F))))
               .add(LootItem.lootTableItem(Blocks.GILDED_BLACKSTONE).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 15.0F))))
               .add(LootItem.lootTableItem(Items.MAGMA_CREAM).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(11))
               .add(LootItem.lootTableItem(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE).setWeight(1))
         )
         .withPool(
            LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).setWeight(1))
         );
   }

   public LootTable.Builder bastionOtherLootTable() {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_PICKAXE)
                     .setWeight(6)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(LootItem.lootTableItem(Items.DIAMOND_SHOVEL).setWeight(6).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(
                  LootItem.lootTableItem(Items.CROSSBOW)
                     .setWeight(6)
                     .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.1F, 0.9F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(LootItem.lootTableItem(Items.ANCIENT_DEBRIS).setWeight(12).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.NETHERITE_SCRAP).setWeight(4).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.SPECTRAL_ARROW).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(10.0F, 22.0F))))
               .add(LootItem.lootTableItem(Items.PIGLIN_BANNER_PATTERN).setWeight(9).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.MUSIC_DISC_PIGSTEP).setWeight(5).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.GOLDEN_CARROT).setWeight(12).apply(SetItemCountFunction.setCount(UniformGenerator.between(6.0F, 17.0F))))
               .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(9).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(
                  LootItem.lootTableItem(Items.BOOK)
                     .setWeight(10)
                     .apply(new EnchantRandomlyFunction.Builder().withEnchantment(registrylookup.getOrThrow(Enchantments.SOUL_SPEED)))
               )
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(2.0F))
               .add(
                  LootItem.lootTableItem(Items.IRON_SWORD)
                     .setWeight(2)
                     .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.1F, 0.9F)))
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(LootItem.lootTableItem(Blocks.IRON_BLOCK).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(
                  LootItem.lootTableItem(Items.GOLDEN_BOOTS)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                     .apply(new EnchantRandomlyFunction.Builder().withEnchantment(registrylookup.getOrThrow(Enchantments.SOUL_SPEED)))
               )
               .add(
                  LootItem.lootTableItem(Items.GOLDEN_AXE)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                     .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries))
               )
               .add(LootItem.lootTableItem(Blocks.GOLD_BLOCK).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.CROSSBOW).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
               .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
               .add(LootItem.lootTableItem(Items.GOLDEN_SWORD).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.GOLDEN_CHESTPLATE).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.GOLDEN_HELMET).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.GOLDEN_LEGGINGS).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.GOLDEN_BOOTS).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Blocks.CRYING_OBSIDIAN).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(3.0F, 4.0F))
               .add(LootItem.lootTableItem(Blocks.GILDED_BLACKSTONE).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Blocks.IRON_CHAIN).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 10.0F))))
               .add(LootItem.lootTableItem(Items.MAGMA_CREAM).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
               .add(LootItem.lootTableItem(Blocks.BONE_BLOCK).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 6.0F))))
               .add(LootItem.lootTableItem(Items.IRON_NUGGET).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
               .add(LootItem.lootTableItem(Blocks.OBSIDIAN).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 6.0F))))
               .add(LootItem.lootTableItem(Items.GOLD_NUGGET).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
               .add(LootItem.lootTableItem(Items.STRING).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 6.0F))))
               .add(LootItem.lootTableItem(Items.ARROW).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 17.0F))))
               .add(LootItem.lootTableItem(Items.COOKED_PORKCHOP).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(11))
               .add(LootItem.lootTableItem(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE).setWeight(1))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(9))
               .add(LootItem.lootTableItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).setWeight(1))
         );
   }

   public LootTable.Builder woodlandMansionLootTable() {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(1.0F, 3.0F))
               .add(LootItem.lootTableItem(Items.LEAD).setWeight(20))
               .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(15))
               .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).setWeight(2))
               .add(LootItem.lootTableItem(Items.MUSIC_DISC_13).setWeight(15))
               .add(LootItem.lootTableItem(Items.MUSIC_DISC_CAT).setWeight(15))
               .add(LootItem.lootTableItem(Items.NAME_TAG).setWeight(20))
               .add(LootItem.lootTableItem(Items.CHAINMAIL_CHESTPLATE).setWeight(10))
               .add(LootItem.lootTableItem(Items.DIAMOND_HOE).setWeight(15))
               .add(LootItem.lootTableItem(Items.DIAMOND_CHESTPLATE).setWeight(5))
               .add(LootItem.lootTableItem(Items.BOOK).setWeight(10).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(1.0F, 4.0F))
               .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
               .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
               .add(LootItem.lootTableItem(Items.BREAD).setWeight(20))
               .add(LootItem.lootTableItem(Items.WHEAT).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
               .add(LootItem.lootTableItem(Items.BUCKET).setWeight(10))
               .add(LootItem.lootTableItem(Items.REDSTONE).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
               .add(LootItem.lootTableItem(Items.COAL).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
               .add(LootItem.lootTableItem(Items.MELON_SEEDS).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
               .add(LootItem.lootTableItem(Items.PUMPKIN_SEEDS).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
               .add(LootItem.lootTableItem(Items.BEETROOT_SEEDS).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
               .add(LootItem.lootTableItem(Items.RESIN_CLUMP).setWeight(50).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(3.0F))
               .add(LootItem.lootTableItem(Items.BONE).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
               .add(LootItem.lootTableItem(Items.GUNPOWDER).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
               .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
               .add(LootItem.lootTableItem(Items.STRING).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(1))
               .add(LootItem.lootTableItem(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE).setWeight(1))
         );
   }

   public LootTable.Builder strongholdLibraryLootTable() {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(2.0F, 10.0F))
               .add(LootItem.lootTableItem(Items.BOOK).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.PAPER).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))
               .add(LootItem.lootTableItem(Items.MAP))
               .add(LootItem.lootTableItem(Items.COMPASS))
               .add(
                  LootItem.lootTableItem(Items.BOOK)
                     .setWeight(10)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, ConstantValue.exactly(30.0F)))
               )
         )
         .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE).setWeight(1)));
   }

   public LootTable.Builder strongholdCorridorLootTable() {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(2.0F, 3.0F))
               .add(LootItem.lootTableItem(Items.ENDER_PEARL).setWeight(10))
               .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.REDSTONE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 9.0F))))
               .add(LootItem.lootTableItem(Items.BREAD).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.APPLE).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.IRON_PICKAXE).setWeight(5))
               .add(LootItem.lootTableItem(Items.IRON_SWORD).setWeight(5))
               .add(LootItem.lootTableItem(Items.IRON_CHESTPLATE).setWeight(5))
               .add(LootItem.lootTableItem(Items.IRON_HELMET).setWeight(5))
               .add(LootItem.lootTableItem(Items.IRON_LEGGINGS).setWeight(5))
               .add(LootItem.lootTableItem(Items.IRON_BOOTS).setWeight(5))
               .add(LootItem.lootTableItem(Items.GOLDEN_APPLE))
               .add(LootItem.lootTableItem(Items.LEATHER).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.COPPER_HORSE_ARMOR))
               .add(LootItem.lootTableItem(Items.IRON_HORSE_ARMOR))
               .add(LootItem.lootTableItem(Items.GOLDEN_HORSE_ARMOR))
               .add(LootItem.lootTableItem(Items.DIAMOND_HORSE_ARMOR))
               .add(LootItem.lootTableItem(Items.MUSIC_DISC_OTHERSIDE))
               .add(LootItem.lootTableItem(Items.BOOK).apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, ConstantValue.exactly(30.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(9))
               .add(LootItem.lootTableItem(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE).setWeight(1))
         );
   }

   public LootTable.Builder ancientCityLootTable() {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(5.0F, 10.0F))
               .add(
                  LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
               )
               .add(LootItem.lootTableItem(Items.MUSIC_DISC_OTHERSIDE).setWeight(1))
               .add(LootItem.lootTableItem(Items.COMPASS).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.SCULK_CATALYST).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
               .add(LootItem.lootTableItem(Items.NAME_TAG).setWeight(2))
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_HOE)
                     .setWeight(2)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                     .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.8F, 1.0F)))
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(30.0F, 50.0F)))
               )
               .add(LootItem.lootTableItem(Items.LEAD).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.DIAMOND_HORSE_ARMOR).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.LEATHER).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.MUSIC_DISC_13).setWeight(2))
               .add(LootItem.lootTableItem(Items.MUSIC_DISC_CAT).setWeight(2))
               .add(
                  LootItem.lootTableItem(Items.DIAMOND_LEGGINGS)
                     .setWeight(2)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(30.0F, 50.0F)))
               )
               .add(
                  LootItem.lootTableItem(Items.BOOK)
                     .setWeight(3)
                     .apply(new EnchantRandomlyFunction.Builder().withEnchantment(registrylookup.getOrThrow(Enchantments.SWIFT_SNEAK)))
               )
               .add(LootItem.lootTableItem(Items.SCULK).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 10.0F))))
               .add(LootItem.lootTableItem(Items.SCULK_SENSOR).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.CANDLE).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
               .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 15.0F))))
               .add(LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.GLOW_BERRIES).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 15.0F))))
               .add(
                  LootItem.lootTableItem(Items.IRON_LEGGINGS)
                     .setWeight(3)
                     .apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, UniformGenerator.between(20.0F, 39.0F)))
               )
               .add(LootItem.lootTableItem(Items.ECHO_SHARD).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.DISC_FRAGMENT_5).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(
                  LootItem.lootTableItem(Items.POTION)
                     .setWeight(5)
                     .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                     .apply(SetPotionFunction.setPotion(Potions.STRONG_REGENERATION))
               )
               .add(LootItem.lootTableItem(Items.BOOK).setWeight(5).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
               .add(LootItem.lootTableItem(Items.BOOK).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 10.0F))))
               .add(LootItem.lootTableItem(Items.BONE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 15.0F))))
               .add(LootItem.lootTableItem(Items.SOUL_TORCH).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 15.0F))))
               .add(LootItem.lootTableItem(Items.COAL).setWeight(7).apply(SetItemCountFunction.setCount(UniformGenerator.between(6.0F, 15.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(75))
               .add(LootItem.lootTableItem(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE).setWeight(4))
               .add(LootItem.lootTableItem(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE).setWeight(1))
         );
   }

   public LootTable.Builder jungleTempleLootTable() {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(2.0F, 6.0F))
               .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))
               .add(LootItem.lootTableItem(Blocks.BAMBOO).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.EMERALD).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.BONE).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 6.0F))))
               .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(16).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 7.0F))))
               .add(LootItem.lootTableItem(Items.LEATHER).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.COPPER_HORSE_ARMOR))
               .add(LootItem.lootTableItem(Items.IRON_HORSE_ARMOR))
               .add(LootItem.lootTableItem(Items.GOLDEN_HORSE_ARMOR))
               .add(LootItem.lootTableItem(Items.DIAMOND_HORSE_ARMOR))
               .add(LootItem.lootTableItem(Items.BOOK).apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, ConstantValue.exactly(30.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(2))
               .add(
                  LootItem.lootTableItem(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE)
                     .setWeight(1)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)))
               )
         );
   }

   public LootTable.Builder shipwreckTreasureLootTable() {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(3.0F, 6.0F))
               .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(90).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.EMERALD).setWeight(40).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(5))
               .add(LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE).setWeight(5))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(2.0F, 5.0F))
               .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(50).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 10.0F))))
               .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 10.0F))))
               .add(LootItem.lootTableItem(Items.LAPIS_LAZULI).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 10.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(5))
               .add(
                  LootItem.lootTableItem(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE)
                     .setWeight(1)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)))
               )
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(148))
               .add(LootItem.lootTableItem(Items.COPPER_NAUTILUS_ARMOR).setWeight(20).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.IRON_NAUTILUS_ARMOR).setWeight(10).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.GOLDEN_NAUTILUS_ARMOR).setWeight(5).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
               .add(LootItem.lootTableItem(Items.DIAMOND_NAUTILUS_ARMOR).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
         );
   }

   public LootTable.Builder pillagerOutpostLootTable() {
      return LootTable.lootTable()
         .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(0.0F, 1.0F)).add(LootItem.lootTableItem(Items.CROSSBOW)))
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(2.0F, 3.0F))
               .add(LootItem.lootTableItem(Items.WHEAT).setWeight(7).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.POTATO).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.CARROT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 5.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(1.0F, 3.0F))
               .add(LootItem.lootTableItem(Blocks.DARK_OAK_LOG).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 3.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(2.0F, 3.0F))
               .add(LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE).setWeight(7))
               .add(LootItem.lootTableItem(Items.STRING).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
               .add(LootItem.lootTableItem(Items.ARROW).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))
               .add(LootItem.lootTableItem(Items.TRIPWIRE_HOOK).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.BOOK).setWeight(1).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(0.0F, 1.0F))
               .add(LootItem.lootTableItem(Items.GOAT_HORN))
               .apply(SetInstrumentFunction.setInstrumentOptions(InstrumentTags.REGULAR_GOAT_HORNS))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(3))
               .add(
                  LootItem.lootTableItem(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE)
                     .setWeight(1)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)))
               )
         );
   }

   public LootTable.Builder desertPyramidLootTable() {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(UniformGenerator.between(2.0F, 4.0F))
               .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))
               .add(LootItem.lootTableItem(Items.EMERALD).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.BONE).setWeight(25).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 6.0F))))
               .add(LootItem.lootTableItem(Items.SPIDER_EYE).setWeight(25).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
               .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(25).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 7.0F))))
               .add(LootItem.lootTableItem(Items.LEATHER).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
               .add(LootItem.lootTableItem(Items.COPPER_HORSE_ARMOR).setWeight(15))
               .add(LootItem.lootTableItem(Items.IRON_HORSE_ARMOR).setWeight(15))
               .add(LootItem.lootTableItem(Items.GOLDEN_HORSE_ARMOR).setWeight(10))
               .add(LootItem.lootTableItem(Items.DIAMOND_HORSE_ARMOR).setWeight(5))
               .add(LootItem.lootTableItem(Items.BOOK).setWeight(20).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
               .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(20))
               .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).setWeight(2))
               .add(EmptyLootItem.emptyItem().setWeight(15))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(4.0F))
               .add(LootItem.lootTableItem(Items.BONE).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
               .add(LootItem.lootTableItem(Items.GUNPOWDER).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
               .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
               .add(LootItem.lootTableItem(Items.STRING).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
               .add(LootItem.lootTableItem(Blocks.SAND).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(EmptyLootItem.emptyItem().setWeight(6))
               .add(
                  LootItem.lootTableItem(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE)
                     .setWeight(1)
                     .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)))
               )
         );
   }
}
