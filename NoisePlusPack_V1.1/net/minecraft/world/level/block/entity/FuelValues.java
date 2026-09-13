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
   private final Object2IntSortedMap<Item> values;

   FuelValues(Object2IntSortedMap<Item> p_367283_) {
      this.values = p_367283_;
   }

   public boolean isFuel(ItemStack p_369912_) {
      return this.values.containsKey(p_369912_.getItem());
   }

   public SequencedSet<Item> fuelItems() {
      return Collections.unmodifiableSequencedSet(this.values.keySet());
   }

   public int burnDuration(ItemStack p_368393_) {
      return p_368393_.isEmpty() ? 0 : this.values.getInt(p_368393_.getItem());
   }

   public static FuelValues vanillaBurnTimes(HolderLookup.Provider p_363816_, FeatureFlagSet p_367705_) {
      return vanillaBurnTimes(p_363816_, p_367705_, 200);
   }

   public static FuelValues vanillaBurnTimes(HolderLookup.Provider p_370014_, FeatureFlagSet p_364680_, int p_363278_) {
      return new FuelValues.Builder(p_370014_, p_364680_)
         .add(Items.LAVA_BUCKET, p_363278_ * 100)
         .add(Blocks.COAL_BLOCK, p_363278_ * 8 * 10)
         .add(Items.BLAZE_ROD, p_363278_ * 12)
         .add(Items.COAL, p_363278_ * 8)
         .add(Items.CHARCOAL, p_363278_ * 8)
         .add(ItemTags.LOGS, p_363278_ * 3 / 2)
         .add(ItemTags.BAMBOO_BLOCKS, p_363278_ * 3 / 2)
         .add(ItemTags.PLANKS, p_363278_ * 3 / 2)
         .add(Blocks.BAMBOO_MOSAIC, p_363278_ * 3 / 2)
         .add(ItemTags.WOODEN_STAIRS, p_363278_ * 3 / 2)
         .add(Blocks.BAMBOO_MOSAIC_STAIRS, p_363278_ * 3 / 2)
         .add(ItemTags.WOODEN_SLABS, p_363278_ * 3 / 4)
         .add(Blocks.BAMBOO_MOSAIC_SLAB, p_363278_ * 3 / 4)
         .add(ItemTags.WOODEN_TRAPDOORS, p_363278_ * 3 / 2)
         .add(ItemTags.WOODEN_PRESSURE_PLATES, p_363278_ * 3 / 2)
         .add(ItemTags.WOODEN_SHELVES, p_363278_ * 3 / 2)
         .add(ItemTags.WOODEN_FENCES, p_363278_ * 3 / 2)
         .add(ItemTags.FENCE_GATES, p_363278_ * 3 / 2)
         .add(Blocks.NOTE_BLOCK, p_363278_ * 3 / 2)
         .add(Blocks.BOOKSHELF, p_363278_ * 3 / 2)
         .add(Blocks.CHISELED_BOOKSHELF, p_363278_ * 3 / 2)
         .add(Blocks.LECTERN, p_363278_ * 3 / 2)
         .add(Blocks.JUKEBOX, p_363278_ * 3 / 2)
         .add(Blocks.CHEST, p_363278_ * 3 / 2)
         .add(Blocks.TRAPPED_CHEST, p_363278_ * 3 / 2)
         .add(Blocks.CRAFTING_TABLE, p_363278_ * 3 / 2)
         .add(Blocks.DAYLIGHT_DETECTOR, p_363278_ * 3 / 2)
         .add(ItemTags.BANNERS, p_363278_ * 3 / 2)
         .add(Items.BOW, p_363278_ * 3 / 2)
         .add(Items.FISHING_ROD, p_363278_ * 3 / 2)
         .add(Blocks.LADDER, p_363278_ * 3 / 2)
         .add(ItemTags.SIGNS, p_363278_)
         .add(ItemTags.HANGING_SIGNS, p_363278_ * 4)
         .add(Items.WOODEN_SHOVEL, p_363278_)
         .add(Items.WOODEN_SWORD, p_363278_)
         .add(Items.WOODEN_SPEAR, p_363278_)
         .add(Items.WOODEN_HOE, p_363278_)
         .add(Items.WOODEN_AXE, p_363278_)
         .add(Items.WOODEN_PICKAXE, p_363278_)
         .add(ItemTags.WOODEN_DOORS, p_363278_)
         .add(ItemTags.BOATS, p_363278_ * 6)
         .add(ItemTags.WOOL, p_363278_ / 2)
         .add(ItemTags.WOODEN_BUTTONS, p_363278_ / 2)
         .add(Items.STICK, p_363278_ / 2)
         .add(ItemTags.SAPLINGS, p_363278_ / 2)
         .add(Items.BOWL, p_363278_ / 2)
         .add(ItemTags.WOOL_CARPETS, 1 + p_363278_ / 3)
         .add(Blocks.DRIED_KELP_BLOCK, 1 + p_363278_ * 20)
         .add(Items.CROSSBOW, p_363278_ * 3 / 2)
         .add(Blocks.BAMBOO, p_363278_ / 4)
         .add(Blocks.DEAD_BUSH, p_363278_ / 2)
         .add(Blocks.SHORT_DRY_GRASS, p_363278_ / 2)
         .add(Blocks.TALL_DRY_GRASS, p_363278_ / 2)
         .add(Blocks.SCAFFOLDING, p_363278_ / 4)
         .add(Blocks.LOOM, p_363278_ * 3 / 2)
         .add(Blocks.BARREL, p_363278_ * 3 / 2)
         .add(Blocks.CARTOGRAPHY_TABLE, p_363278_ * 3 / 2)
         .add(Blocks.FLETCHING_TABLE, p_363278_ * 3 / 2)
         .add(Blocks.SMITHING_TABLE, p_363278_ * 3 / 2)
         .add(Blocks.COMPOSTER, p_363278_ * 3 / 2)
         .add(Blocks.AZALEA, p_363278_ / 2)
         .add(Blocks.FLOWERING_AZALEA, p_363278_ / 2)
         .add(Blocks.MANGROVE_ROOTS, p_363278_ * 3 / 2)
         .add(Blocks.LEAF_LITTER, p_363278_ / 2)
         .remove(ItemTags.NON_FLAMMABLE_WOOD)
         .build();
   }

   public static class Builder {
      private final HolderLookup<Item> items;
      private final FeatureFlagSet enabledFeatures;
      private final Object2IntSortedMap<Item> values = new Object2IntLinkedOpenHashMap();

      public Builder(HolderLookup.Provider p_369440_, FeatureFlagSet p_369101_) {
         this.items = p_369440_.lookupOrThrow(Registries.ITEM);
         this.enabledFeatures = p_369101_;
      }

      public FuelValues build() {
         return new FuelValues(this.values);
      }

      public FuelValues.Builder remove(TagKey<Item> p_369702_) {
         this.values.keySet().removeIf(p_361506_ -> p_361506_.builtInRegistryHolder().is(p_369702_));
         return this;
      }

      public FuelValues.Builder add(TagKey<Item> p_367371_, int p_368360_) {
         this.items.get(p_367371_).ifPresent(p_361860_ -> {
            for (Holder<Item> holder : p_361860_) {
               this.putInternal(p_368360_, holder.value());
            }
         });
         return this;
      }

      public FuelValues.Builder add(ItemLike p_365111_, int p_364289_) {
         Item item = p_365111_.asItem();
         this.putInternal(p_364289_, item);
         return this;
      }

      private void putInternal(int p_361402_, Item p_366165_) {
         if (p_366165_.isEnabled(this.enabledFeatures)) {
            this.values.put(p_366165_, p_361402_);
         }
      }
   }
}
