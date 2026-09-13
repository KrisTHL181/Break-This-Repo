package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

public class TransmuteRecipe extends NormalCraftingRecipe {
   private static final int MIN_MATERIAL_COUNT = 1;
   private static final int MAX_MATERIAL_COUNT = 8;
   public static final MinMaxBounds.Ints DEFAULT_MATERIAL_COUNT = MinMaxBounds.Ints.exactly(1);
   public static final MinMaxBounds.Ints FULL_RANGE_MATERIAL_COUNT = MinMaxBounds.Ints.between(1, 8);
   public static final Codec<MinMaxBounds.Ints> MATERIAL_COUNT_BOUNDS = MinMaxBounds.Ints.CODEC
      .validate(MinMaxBounds.validateContainedInRange(FULL_RANGE_MATERIAL_COUNT));
   public static final MapCodec<TransmuteRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
            Ingredient.CODEC.fieldOf("input").forGetter(o -> o.input),
            Ingredient.CODEC.fieldOf("material").forGetter(o -> o.material),
            MATERIAL_COUNT_BOUNDS.optionalFieldOf("material_count", DEFAULT_MATERIAL_COUNT).forGetter(o -> o.materialCount),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result),
            Codec.BOOL.optionalFieldOf("add_material_count_to_result", false).forGetter(o -> o.addMaterialCountToResult)
         )
         .apply(i, TransmuteRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> STREAM_CODEC = StreamCodec.composite(
      Recipe.CommonInfo.STREAM_CODEC,
      o -> o.commonInfo,
      CraftingRecipe.CraftingBookInfo.STREAM_CODEC,
      o -> o.bookInfo,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.input,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.material,
      MinMaxBounds.Ints.STREAM_CODEC,
      o -> o.materialCount,
      ItemStackTemplate.STREAM_CODEC,
      o -> o.result,
      ByteBufCodecs.BOOL,
      o -> o.addMaterialCountToResult,
      TransmuteRecipe::new
   );
   public static final RecipeSerializer<TransmuteRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
   private final Ingredient input;
   private final Ingredient material;
   private final MinMaxBounds.Ints materialCount;
   private final ItemStackTemplate result;
   private final boolean addMaterialCountToResult;

   public TransmuteRecipe(
      final Recipe.CommonInfo commonInfo,
      final CraftingRecipe.CraftingBookInfo bookInfo,
      final Ingredient input,
      final Ingredient material,
      final MinMaxBounds.Ints materialCount,
      final ItemStackTemplate result,
      final boolean addMaterialCountToResult
   ) {
      super(commonInfo, bookInfo);
      this.input = input;
      this.material = material;
      this.materialCount = materialCount;
      this.result = result;
      this.addMaterialCountToResult = addMaterialCountToResult;
   }

   public static ItemStack createWithOriginalComponents(final ItemStackTemplate target, final ItemStack input) {
      return createWithOriginalComponents(target, input, 0);
   }

   public static ItemStack createWithOriginalComponents(final ItemStackTemplate target, final ItemStack input, final int extraCount) {
      return target.apply(target.count() + extraCount, input.getComponentsPatch());
   }

   private int computeResultSize(final int materialCount) {
      return this.addMaterialCountToResult ? materialCount + this.result.count() : this.result.count();
   }

   private ItemStack computeResult(final ItemStack inputIngredient, final int materialCount) {
      return createWithOriginalComponents(this.result, inputIngredient, materialCount);
   }

   public boolean matches(final CraftingInput input, final Level level) {
      int minMaterialCount = this.minMaterialCount();
      int maxMaterialCount = this.maxMaterialCount();
      if (input.ingredientCount() >= minMaterialCount + 1 && input.ingredientCount() <= maxMaterialCount + 1) {
         ItemStack foundInput = null;
         int materialCount = 0;

         for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty()) {
               if (this.input.test(stack)) {
                  if (foundInput != null) {
                     return false;
                  }

                  foundInput = stack;
               } else {
                  if (!this.material.test(stack)) {
                     return false;
                  }

                  if (++materialCount > maxMaterialCount) {
                     return false;
                  }
               }
            }
         }

         if (foundInput != null && !foundInput.isEmpty() && this.materialCount.matches(materialCount)) {
            int resultCount = this.computeResultSize(materialCount);
            if (resultCount != 1) {
               return true;
            }

            ItemStack result = this.computeResult(foundInput, 0);
            return result.isEmpty() ? false : !ItemStack.isSameItemSameComponents(foundInput, result);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public ItemStack assemble(final CraftingInput input) {
      if (this.addMaterialCountToResult) {
         int materialCount = 0;
         ItemStack inputIngredient = ItemStack.EMPTY;

         for (int slot = 0; slot < input.size(); slot++) {
            ItemStack itemStack = input.getItem(slot);
            if (!itemStack.isEmpty()) {
               if (this.input.test(itemStack)) {
                  inputIngredient = itemStack;
               } else if (this.material.test(itemStack)) {
                  materialCount++;
               }
            }
         }

         return this.computeResult(inputIngredient, materialCount);
      } else {
         for (int slot = 0; slot < input.size(); slot++) {
            ItemStack itemStack = input.getItem(slot);
            if (!itemStack.isEmpty() && this.input.test(itemStack)) {
               return this.computeResult(itemStack, 0);
            }
         }

         return ItemStack.EMPTY;
      }
   }

   @Override
   public List<RecipeDisplay> display() {
      List<RecipeDisplay> displays = new ArrayList<>();
      List<SlotDisplay> ingredientSlots = new ArrayList<>();
      ingredientSlots.add(this.input.display());
      SlotDisplay materialDisplay = this.material.display();
      int maxMaterialCount = this.maxMaterialCount();
      int minMaterialCount = this.minMaterialCount();

      for (int materialCount = minMaterialCount; materialCount <= maxMaterialCount; materialCount++) {
         ingredientSlots.add(materialDisplay);
         int resultCount = this.computeResultSize(materialCount);
         displays.add(
            new ShapelessCraftingRecipeDisplay(
               List.copyOf(ingredientSlots),
               new SlotDisplay.ItemStackSlotDisplay(this.result.withCount(resultCount)),
               new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
            )
         );
      }

      return displays;
   }

   private int minMaterialCount() {
      return this.materialCount.min().orElse(1);
   }

   private int maxMaterialCount() {
      return this.materialCount.max().orElse(8);
   }

   @Override
   public RecipeSerializer<TransmuteRecipe> getSerializer() {
      return SERIALIZER;
   }

   @Override
   protected PlacementInfo createPlacementInfo() {
      int maxMaterialCount = this.maxMaterialCount();
      List<Ingredient> ingredients = new ArrayList<>(1 + maxMaterialCount);
      ingredients.add(this.input);
      ingredients.addAll(Collections.nCopies(maxMaterialCount, this.material));
      return PlacementInfo.create(ingredients);
   }
}
