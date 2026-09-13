package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import org.jspecify.annotations.Nullable;

public class SmithingTransformRecipe implements SmithingRecipe {
   final Optional<Ingredient> template;
   final Ingredient base;
   final Optional<Ingredient> addition;
   final TransmuteResult result;
   private @Nullable PlacementInfo placementInfo;

   public SmithingTransformRecipe(Optional<Ingredient> p_366010_, Ingredient p_396043_, Optional<Ingredient> p_367568_, TransmuteResult p_394657_) {
      this.template = p_366010_;
      this.base = p_396043_;
      this.addition = p_367568_;
      this.result = p_394657_;
   }

   public ItemStack assemble(SmithingRecipeInput p_343590_, HolderLookup.Provider p_331030_) {
      return this.result.apply(p_343590_.base());
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
      return RecipeSerializer.SMITHING_TRANSFORM;
   }

   @Override
   public PlacementInfo placementInfo() {
      if (this.placementInfo == null) {
         this.placementInfo = PlacementInfo.createFromOptionals(List.of(this.template, Optional.of(this.base), this.addition));
      }

      return this.placementInfo;
   }

   @Override
   public List<RecipeDisplay> display() {
      return List.of(
         new SmithingRecipeDisplay(
            Ingredient.optionalIngredientToDisplay(this.template),
            this.base.display(),
            Ingredient.optionalIngredientToDisplay(this.addition),
            this.result.display(),
            new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)
         )
      );
   }

   public static class Serializer implements RecipeSerializer<SmithingTransformRecipe> {
      private static final MapCodec<SmithingTransformRecipe> CODEC = RecordCodecBuilder.mapCodec(
         p_390827_ -> p_390827_.group(
               Ingredient.CODEC.optionalFieldOf("template").forGetter(p_359869_ -> p_359869_.template),
               Ingredient.CODEC.fieldOf("base").forGetter(p_390828_ -> p_390828_.base),
               Ingredient.CODEC.optionalFieldOf("addition").forGetter(p_359867_ -> p_359867_.addition),
               TransmuteResult.CODEC.fieldOf("result").forGetter(p_390825_ -> p_390825_.result)
            )
            .apply(p_390827_, SmithingTransformRecipe::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SmithingTransformRecipe> STREAM_CODEC = StreamCodec.composite(
         Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC,
         p_359864_ -> p_359864_.template,
         Ingredient.CONTENTS_STREAM_CODEC,
         p_390826_ -> p_390826_.base,
         Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC,
         p_359870_ -> p_359870_.addition,
         TransmuteResult.STREAM_CODEC,
         p_390829_ -> p_390829_.result,
         SmithingTransformRecipe::new
      );

      @Override
      public MapCodec<SmithingTransformRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, SmithingTransformRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
