package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;

public class BrewingRecipe implements Recipe<BrewingInput> {
   public static final MapCodec<BrewingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            PotionIngredient.MAP_CODEC.fieldOf("input").forGetter(o -> o.input),
            PotionIngredient.MAP_CODEC.fieldOf("reagent").forGetter(o -> o.reagent),
            ItemStackTemplate.CODEC.fieldOf("output").forGetter(o -> o.output)
         )
         .apply(i, BrewingRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, BrewingRecipe> STREAM_CODEC = StreamCodec.composite(
      PotionIngredient.STREAM_CODEC,
      o -> o.input,
      PotionIngredient.STREAM_CODEC,
      o -> o.reagent,
      ItemStackTemplate.STREAM_CODEC,
      o -> o.output,
      BrewingRecipe::new
   );
   public static final RecipeSerializer<BrewingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
   private final PotionIngredient input;
   private final PotionIngredient reagent;
   private final ItemStackTemplate output;

   public BrewingRecipe(final PotionIngredient input, final PotionIngredient reagent, final ItemStackTemplate output) {
      this.input = input;
      this.reagent = reagent;
      this.output = output;
   }

   public PotionIngredient getInput() {
      return this.input;
   }

   public PotionIngredient getReagent() {
      return this.reagent;
   }

   public ItemStackTemplate getOutput() {
      return this.output;
   }

   public boolean matches(final BrewingInput brewingInput, final Level level) {
      return this.matches(brewingInput);
   }

   public boolean matches(final BrewingInput brewingInput) {
      return this.input.test(brewingInput.input()) && this.reagent.test(brewingInput.reagent());
   }

   public ItemStack assemble(final BrewingInput input) {
      return this.output.create();
   }

   @Override
   public RecipeType<BrewingRecipe> getType() {
      return RecipeType.BREWING;
   }

   @Override
   public PlacementInfo placementInfo() {
      return PlacementInfo.NOT_PLACEABLE;
   }

   @Override
   public boolean isSpecial() {
      return true;
   }

   @Override
   public boolean showNotification() {
      return false;
   }

   @Override
   public String group() {
      return "";
   }

   @Override
   public RecipeBookCategory recipeBookCategory() {
      return RecipeBookCategories.CRAFTING_MISC;
   }

   @Override
   public RecipeSerializer<BrewingRecipe> getSerializer() {
      return SERIALIZER;
   }

   @FunctionalInterface
   public interface Factory<T extends BrewingRecipe> {
      T create(final Recipe.CommonInfo commonInfo, final PotionIngredient input, final PotionIngredient reagent, final ItemStackTemplate output);
   }
}
