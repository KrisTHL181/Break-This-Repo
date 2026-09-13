package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public interface CraftingRecipe extends Recipe<CraftingInput> {
   @Override
   default RecipeType<CraftingRecipe> getType() {
      return RecipeType.CRAFTING;
   }

   @Override
   RecipeSerializer<? extends CraftingRecipe> getSerializer();

   CraftingBookCategory category();

   default NonNullList<ItemStack> getRemainingItems(final CraftingInput input) {
      return defaultCraftingReminder(input);
   }

   static NonNullList<ItemStack> defaultCraftingReminder(final CraftingInput input) {
      NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);

      for (int slot = 0; slot < result.size(); slot++) {
         Item item = input.getItem(slot).getItem();
         ItemStackTemplate remainder = item.getCraftingRemainder();
         result.set(slot, remainder != null ? remainder.create() : ItemStack.EMPTY);
      }

      return result;
   }

   @Override
   default RecipeBookCategory recipeBookCategory() {
      return switch (this.category()) {
         case BUILDING -> RecipeBookCategories.CRAFTING_BUILDING_BLOCKS;
         case EQUIPMENT -> RecipeBookCategories.CRAFTING_EQUIPMENT;
         case REDSTONE -> RecipeBookCategories.CRAFTING_REDSTONE;
         case MISC -> RecipeBookCategories.CRAFTING_MISC;
      };
   }

   record CraftingBookInfo(CraftingBookCategory category, String group) implements Recipe.BookInfo<CraftingBookCategory> {
      public static final MapCodec<CraftingRecipe.CraftingBookInfo> MAP_CODEC = Recipe.BookInfo.mapCodec(
         CraftingBookCategory.CODEC, CraftingBookCategory.MISC, CraftingRecipe.CraftingBookInfo::new
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, CraftingRecipe.CraftingBookInfo> STREAM_CODEC = Recipe.BookInfo.streamCodec(
         CraftingBookCategory.STREAM_CODEC, CraftingRecipe.CraftingBookInfo::new
      );
   }
}
