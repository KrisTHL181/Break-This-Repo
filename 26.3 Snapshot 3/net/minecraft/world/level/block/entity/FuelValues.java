package net.minecraft.world.level.block.entity;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import java.util.Collections;
import java.util.SequencedSet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public class FuelValues {
   public static final int BREWING_TIME_SECONDS = 20;
   private final Object2IntSortedMap<Item> values;

   private FuelValues(final Object2IntSortedMap<Item> values) {
      this.values = values;
   }

   public boolean isFuel(final ItemStack itemStack) {
      return this.values.containsKey(itemStack.getItem());
   }

   public SequencedSet<Item> fuelItems() {
      return Collections.unmodifiableSequencedSet(this.values.keySet());
   }

   public int burnDuration(final ItemStack itemStack) {
      return itemStack.isEmpty() ? 0 : this.values.getInt(itemStack.getItem());
   }

   public static FuelValues vanillaBurnTimes(final HolderLookup.Provider registries, final FeatureFlagSet enabledFeatures) {
      return vanillaBurnTimes(registries, enabledFeatures, 200);
   }

   public static FuelValues vanillaBurnTimes(final HolderLookup.Provider registries, final FeatureFlagSet enabledFeatures, final int baseUnit) {
      return new FuelValues.Builder(registries, enabledFeatures)
         .add(Items.LAVA_BUCKET, baseUnit * 100)
         .add(Blocks.COAL_BLOCK, baseUnit * 8 * 10)
         .add(Items.BLAZE_ROD, baseUnit * 12)
         .add(Items.COAL, baseUnit * 8)
         .add(Items.CHARCOAL, baseUnit * 8)
         .add(ItemTags.LOGS, baseUnit * 3 / 2)
         .add(ItemTags.BAMBOO_BLOCKS, baseUnit * 3 / 2)
         .add(ItemTags.PLANKS, baseUnit * 3 / 2)
         .add(Blocks.BAMBOO_MOSAIC, baseUnit * 3 / 2)
         .add(ItemTags.WOODEN_STAIRS, baseUnit * 3 / 2)
         .add(Blocks.BAMBOO_MOSAIC_STAIRS, baseUnit * 3 / 2)
         .add(ItemTags.WOODEN_SLABS, baseUnit * 3 / 4)
         .add(Blocks.BAMBOO_MOSAIC_SLAB, baseUnit * 3 / 4)
         .add(ItemTags.WOODEN_TRAPDOORS, baseUnit * 3 / 2)
         .add(ItemTags.WOODEN_PRESSURE_PLATES, baseUnit * 3 / 2)
         .add(ItemTags.WOODEN_SHELVES, baseUnit * 3 / 2)
         .add(ItemTags.WOODEN_FENCES, baseUnit * 3 / 2)
         .add(ItemTags.FENCE_GATES, baseUnit * 3 / 2)
         .add(Blocks.NOTE_BLOCK, baseUnit * 3 / 2)
         .add(Blocks.BOOKSHELF, baseUnit * 3 / 2)
         .add(Blocks.CHISELED_BOOKSHELF, baseUnit * 3 / 2)
         .add(Blocks.LECTERN, baseUnit * 3 / 2)
         .add(Blocks.JUKEBOX, baseUnit * 3 / 2)
         .add(Blocks.CHEST, baseUnit * 3 / 2)
         .add(Blocks.TRAPPED_CHEST, baseUnit * 3 / 2)
         .add(Blocks.CRAFTING_TABLE, baseUnit * 3 / 2)
         .add(Blocks.DAYLIGHT_DETECTOR, baseUnit * 3 / 2)
         .add(ItemTags.BANNERS, baseUnit * 3 / 2)
         .add(Items.BOW, baseUnit * 3 / 2)
         .add(Items.FISHING_ROD, baseUnit * 3 / 2)
         .add(Blocks.LADDER, baseUnit * 3 / 2)
         .add(ItemTags.SIGNS, baseUnit)
         .add(ItemTags.HANGING_SIGNS, baseUnit * 4)
         .add(Items.WOODEN_SHOVEL, baseUnit)
         .add(Items.WOODEN_SWORD, baseUnit)
         .add(Items.WOODEN_SPEAR, baseUnit)
         .add(Items.WOODEN_HOE, baseUnit)
         .add(Items.WOODEN_AXE, baseUnit)
         .add(Items.WOODEN_PICKAXE, baseUnit)
         .add(ItemTags.WOODEN_DOORS, baseUnit)
         .add(ItemTags.BOATS, baseUnit * 6)
         .add(ItemTags.WOOL, baseUnit / 2)
         .add(ItemTags.WOOL_STAIRS, baseUnit / 2)
         .add(ItemTags.WOOL_SLABS, baseUnit / 4)
         .add(ItemTags.WOODEN_BUTTONS, baseUnit / 2)
         .add(Items.STICK, baseUnit / 2)
         .add(ItemTags.SAPLINGS, baseUnit / 2)
         .add(Items.BOWL, baseUnit / 2)
         .add(ItemTags.WOOL_CARPETS, 1 + baseUnit / 3)
         .add(Blocks.DRIED_KELP_BLOCK, 1 + baseUnit * 20)
         .add(Items.CROSSBOW, baseUnit * 3 / 2)
         .add(Blocks.BAMBOO, baseUnit / 4)
         .add(Blocks.DEAD_BUSH, baseUnit / 2)
         .add(Blocks.SHORT_DRY_GRASS, baseUnit / 2)
         .add(Blocks.TALL_DRY_GRASS, baseUnit / 2)
         .add(Blocks.SCAFFOLDING, baseUnit / 4)
         .add(Blocks.LOOM, baseUnit * 3 / 2)
         .add(Blocks.BARREL, baseUnit * 3 / 2)
         .add(Blocks.CARTOGRAPHY_TABLE, baseUnit * 3 / 2)
         .add(Blocks.FLETCHING_TABLE, baseUnit * 3 / 2)
         .add(Blocks.SMITHING_TABLE, baseUnit * 3 / 2)
         .add(Blocks.COMPOSTER, baseUnit * 3 / 2)
         .add(Blocks.AZALEA, baseUnit / 2)
         .add(Blocks.FLOWERING_AZALEA, baseUnit / 2)
         .add(Blocks.MANGROVE_ROOTS, baseUnit * 3 / 2)
         .add(Blocks.LEAF_LITTER, baseUnit / 2)
         .add(ItemTags.CUSHIONS, baseUnit)
         .remove(ItemTags.NON_FLAMMABLE_WOOD)
         .build();
   }

   public static class Builder {
      private final HolderLookup<Item> items;
      private final FeatureFlagSet enabledFeatures;
      private final Object2IntSortedMap<Item> values = new Object2IntLinkedOpenHashMap();

      public Builder(final HolderLookup.Provider registries, final FeatureFlagSet enabledFeatures) {
         this.items = registries.lookupOrThrow(Registries.ITEM);
         this.enabledFeatures = enabledFeatures;
      }

      public FuelValues build() {
         return new FuelValues(this.values);
      }

      public FuelValues.Builder remove(final TagKey<Item> tag) {
         this.values.keySet().removeIf(item -> item.builtInRegistryHolder().is(tag));
         return this;
      }

      public FuelValues.Builder add(final TagKey<Item> tag, final int time) {
         this.items.get(tag).ifPresent(items -> {
            for (Holder<Item> item : items) {
               this.putInternal(time, item.value());
            }
         });
         return this;
      }

      public FuelValues.Builder add(final ItemLike itemLike, final int time) {
         Item item = itemLike.asItem();
         this.putInternal(time, item);
         return this;
      }

      private void putInternal(final int time, final Item item) {
         if (item.isEnabled(this.enabledFeatures)) {
            this.values.put(item, time);
         }
      }
   }
}
