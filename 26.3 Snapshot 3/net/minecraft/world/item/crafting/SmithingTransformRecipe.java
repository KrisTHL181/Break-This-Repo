package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;

public class SmithingTransformRecipe extends SimpleSmithingRecipe {
   public static final MapCodec<SmithingTransformRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
            Ingredient.CODEC.optionalFieldOf("template").forGetter(o -> o.template),
            Ingredient.CODEC.fieldOf("base").forGetter(o -> o.base),
            Ingredient.CODEC.optionalFieldOf("addition").forGetter(o -> o.addition),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
         )
         .apply(i, SmithingTransformRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, SmithingTransformRecipe> STREAM_CODEC = StreamCodec.composite(
      Recipe.CommonInfo.STREAM_CODEC,
      o -> o.commonInfo,
      Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC,
      o -> o.template,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.base,
      Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC,
      o -> o.addition,
      ItemStackTemplate.STREAM_CODEC,
      o -> o.result,
      SmithingTransformRecipe::new
   );
   public static final RecipeSerializer<SmithingTransformRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
   private final Optional<Ingredient> template;
   private final Ingredient base;
   private final Optional<Ingredient> addition;
   private final ItemStackTemplate result;

   public SmithingTransformRecipe(
      final Recipe.CommonInfo commonInfo,
      final Optional<Ingredient> template,
      final Ingredient base,
      final Optional<Ingredient> addition,
      final ItemStackTemplate result
   ) {
      super(commonInfo);
      this.template = template;
      this.base = base;
      this.addition = addition;
      this.result = result;
   }

   public ItemStack assemble(final SmithingRecipeInput input) {
      return TransmuteRecipe.createWithOriginalComponents(this.result, input.base());
   }

   @Override
   public Optional<Ingredient> templateIngredient() {
      return this.template;
   }

   @Override
   public Ingredient baseIngredient() {
      return this.base;
   }

   @Override
   public Optional<Ingredient> additionIngredient() {
      return this.addition;
   }

   @Override
   public RecipeSerializer<SmithingTransformRecipe> getSerializer() {
      return SERIALIZER;
   }

   @Override
   protected PlacementInfo createPlacementInfo() {
      return PlacementInfo.createFromOptionals(List.of(this.template, Optional.of(this.base), this.addition));
   }

   @Override
   public List<RecipeDisplay> display() {
      return List.of(
         new SmithingRecipeDisplay(
            Ingredient.optionalIngredientToDisplay(this.template),
            this.base.display(),
            Ingredient.optionalIngredientToDisplay(this.addition),
            new SlotDisplay.ItemStackSlotDisplay(this.result),
            new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)
         )
      );
   }
}
