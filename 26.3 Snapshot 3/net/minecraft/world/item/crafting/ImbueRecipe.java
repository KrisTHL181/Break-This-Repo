package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

public class ImbueRecipe extends NormalCraftingRecipe {
   public static final MapCodec<ImbueRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
            Ingredient.CODEC.fieldOf("source").forGetter(o -> o.source),
            Ingredient.CODEC.fieldOf("material").forGetter(o -> o.material),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
         )
         .apply(i, ImbueRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ImbueRecipe> STREAM_CODEC = StreamCodec.composite(
      Recipe.CommonInfo.STREAM_CODEC,
      o -> o.commonInfo,
      CraftingRecipe.CraftingBookInfo.STREAM_CODEC,
      o -> o.bookInfo,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.source,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.material,
      ItemStackTemplate.STREAM_CODEC,
      o -> o.result,
      ImbueRecipe::new
   );
   public static final RecipeSerializer<ImbueRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
   private final Ingredient source;
   private final Ingredient material;
   private final ItemStackTemplate result;

   public ImbueRecipe(
      final Recipe.CommonInfo commonInfo,
      final CraftingRecipe.CraftingBookInfo bookInfo,
      final Ingredient source,
      final Ingredient material,
      final ItemStackTemplate result
   ) {
      super(commonInfo, bookInfo);
      this.source = source;
      this.material = material;
      this.result = result;
   }

   public boolean matches(final CraftingInput input, final Level level) {
      if (input.width() == 3 && input.height() == 3 && input.ingredientCount() == 9) {
         for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
               ItemStack itemStack = input.getItem(x, y);
               if (itemStack.isEmpty()) {
                  return false;
               }

               Ingredient ingredient = x == 1 && y == 1 ? this.source : this.material;
               if (!ingredient.test(itemStack)) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public ItemStack assemble(final CraftingInput input) {
      ItemStack source = input.getItem(1, 1);
      ItemStack result = this.result.create();
      result.set(DataComponents.POTION_CONTENTS, source.get(DataComponents.POTION_CONTENTS));
      return result;
   }

   @Override
   public RecipeSerializer<ImbueRecipe> getSerializer() {
      return SERIALIZER;
   }

   @Override
   protected PlacementInfo createPlacementInfo() {
      return PlacementInfo.create(
         List.of(this.material, this.material, this.material, this.material, this.source, this.material, this.material, this.material, this.material)
      );
   }

   @Override
   public List<RecipeDisplay> display() {
      SlotDisplay material = this.material.display();
      SlotDisplay.WithAnyPotion source = new SlotDisplay.WithAnyPotion(this.source.display());
      return List.of(
         new ShapedCraftingRecipeDisplay(
            3,
            3,
            List.of(material, material, material, material, source, material, material, material, material),
            new SlotDisplay.WithAnyPotion(new SlotDisplay.ItemStackSlotDisplay(this.result)),
            new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
         )
      );
   }
}
