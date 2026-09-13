package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ShapelessRecipe implements CraftingRecipe {
   final String group;
   final CraftingBookCategory category;
   final ItemStack result;
   final List<Ingredient> ingredients;
   private @Nullable PlacementInfo placementInfo;

   public ShapelessRecipe(String p_249640_, CraftingBookCategory p_249390_, ItemStack p_252071_, List<Ingredient> p_365863_) {
      this.group = p_249640_;
      this.category = p_249390_;
      this.result = p_252071_;
      this.ingredients = p_365863_;
   }

   @Override
   public RecipeSerializer<ShapelessRecipe> getSerializer() {
      return RecipeSerializer.SHAPELESS_RECIPE;
   }

   @Override
   public String group() {
      return this.group;
   }

   @Override
   public CraftingBookCategory category() {
      return this.category;
   }

   @Override
   public PlacementInfo placementInfo() {
      if (this.placementInfo == null) {
         this.placementInfo = PlacementInfo.create(this.ingredients);
      }

      return this.placementInfo;
   }

   public boolean matches(CraftingInput p_345423_, Level p_44263_) {
      if (p_345423_.ingredientCount() != this.ingredients.size()) {
         return false;
      } else {
         return p_345423_.size() == 1 && this.ingredients.size() == 1
            ? this.ingredients.getFirst().test(p_345423_.getItem(0))
            : p_345423_.stackedContents().canCraft(this, null);
      }
   }

   public ItemStack assemble(CraftingInput p_342466_, HolderLookup.Provider p_334364_) {
      return this.result.copy();
   }

   @Override
   public List<RecipeDisplay> display() {
      return List.of(
         new ShapelessCraftingRecipeDisplay(
            this.ingredients.stream().map(Ingredient::display).toList(),
            new SlotDisplay.ItemStackSlotDisplay(this.result),
            new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
         )
      );
   }

   public static class Serializer implements RecipeSerializer<ShapelessRecipe> {
      private static final MapCodec<ShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(
         p_359856_ -> p_359856_.group(
               Codec.STRING.optionalFieldOf("group", "").forGetter(p_299460_ -> p_299460_.group),
               CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(p_297437_ -> p_297437_.category),
               ItemStack.STRICT_CODEC.fieldOf("result").forGetter(p_300770_ -> p_300770_.result),
               Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(p_359857_ -> p_359857_.ingredients)
            )
            .apply(p_359856_, ShapelessRecipe::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8,
         p_359858_ -> p_359858_.group,
         CraftingBookCategory.STREAM_CODEC,
         p_359859_ -> p_359859_.category,
         ItemStack.STREAM_CODEC,
         p_359860_ -> p_359860_.result,
         Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
         p_359861_ -> p_359861_.ingredients,
         ShapelessRecipe::new
      );

      @Override
      public MapCodec<ShapelessRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, ShapelessRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
