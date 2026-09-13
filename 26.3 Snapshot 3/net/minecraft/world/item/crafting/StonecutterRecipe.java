package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

public class StonecutterRecipe extends SingleItemRecipe {
   public static final MapCodec<StonecutterRecipe> MAP_CODEC = simpleMapCodec(StonecutterRecipe::new);
   public static final StreamCodec<RegistryFriendlyByteBuf, StonecutterRecipe> STREAM_CODEC = simpleStreamCodec(StonecutterRecipe::new);
   public static final RecipeSerializer<StonecutterRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

   public StonecutterRecipe(final Recipe.CommonInfo commonInfo, final Ingredient ingredient, final ItemStackTemplate result) {
      super(commonInfo, ingredient, result);
   }

   @Override
   public RecipeType<StonecutterRecipe> getType() {
      return RecipeType.STONECUTTING;
   }

   @Override
   public RecipeSerializer<StonecutterRecipe> getSerializer() {
      return SERIALIZER;
   }

   @Override
   public String group() {
      return "";
   }

   @Override
   public List<RecipeDisplay> display() {
      return List.of(new StonecutterRecipeDisplay(this.input().display(), this.resultDisplay(), new SlotDisplay.ItemSlotDisplay(Items.STONECUTTER)));
   }

   public SlotDisplay resultDisplay() {
      return new SlotDisplay.ItemStackSlotDisplay(this.result());
   }

   @Override
   public RecipeBookCategory recipeBookCategory() {
      return RecipeBookCategories.STONECUTTER;
   }
}
