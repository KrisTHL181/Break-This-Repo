package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

public class SmokingRecipe extends AbstractCookingRecipe {
   public static final MapCodec<SmokingRecipe> MAP_CODEC = cookingMapCodec(SmokingRecipe::new, 100);
   public static final StreamCodec<RegistryFriendlyByteBuf, SmokingRecipe> STREAM_CODEC = cookingStreamCodec(SmokingRecipe::new);
   public static final RecipeSerializer<SmokingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

   public SmokingRecipe(
      final Recipe.CommonInfo commonInfo,
      final AbstractCookingRecipe.CookingBookInfo bookInfo,
      final Ingredient ingredient,
      final ItemStackTemplate result,
      final float experience,
      final int cookingTime
   ) {
      super(commonInfo, bookInfo, ingredient, result, experience, cookingTime);
   }

   @Override
   protected Item furnaceIcon() {
      return Items.SMOKER;
   }

   @Override
   public RecipeType<SmokingRecipe> getType() {
      return RecipeType.SMOKING;
   }

   @Override
   public RecipeSerializer<SmokingRecipe> getSerializer() {
      return SERIALIZER;
   }

   @Override
   public RecipeBookCategory recipeBookCategory() {
      return RecipeBookCategories.SMOKER_FOOD;
   }
}
