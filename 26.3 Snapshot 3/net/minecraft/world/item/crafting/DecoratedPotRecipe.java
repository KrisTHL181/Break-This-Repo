package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.PotDecorations;

public class DecoratedPotRecipe extends CustomRecipe {
   public static final MapCodec<DecoratedPotRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Ingredient.CODEC.fieldOf("back").forGetter(o -> o.backPattern),
            Ingredient.CODEC.fieldOf("left").forGetter(o -> o.leftPattern),
            Ingredient.CODEC.fieldOf("right").forGetter(o -> o.rightPattern),
            Ingredient.CODEC.fieldOf("front").forGetter(o -> o.frontPattern),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
         )
         .apply(i, DecoratedPotRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, DecoratedPotRecipe> STREAM_CODEC = StreamCodec.composite(
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.backPattern,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.leftPattern,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.rightPattern,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.frontPattern,
      ItemStackTemplate.STREAM_CODEC,
      o -> o.result,
      DecoratedPotRecipe::new
   );
   public static final RecipeSerializer<DecoratedPotRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
   private final Ingredient backPattern;
   private final Ingredient leftPattern;
   private final Ingredient rightPattern;
   private final Ingredient frontPattern;
   private final ItemStackTemplate result;

   public DecoratedPotRecipe(final Ingredient wallPattern, final ItemStackTemplate result) {
      this(wallPattern, wallPattern, wallPattern, wallPattern, result);
   }

   public DecoratedPotRecipe(
      final Ingredient backPattern, final Ingredient leftPattern, final Ingredient rightPattern, final Ingredient frontPattern, final ItemStackTemplate result
   ) {
      this.backPattern = backPattern;
      this.leftPattern = leftPattern;
      this.rightPattern = rightPattern;
      this.frontPattern = frontPattern;
      this.result = result;
   }

   private static ItemStack back(final CraftingInput input) {
      return input.getItem(1, 0);
   }

   private static ItemStack left(final CraftingInput input) {
      return input.getItem(0, 1);
   }

   private static ItemStack right(final CraftingInput input) {
      return input.getItem(2, 1);
   }

   private static ItemStack front(final CraftingInput input) {
      return input.getItem(1, 2);
   }

   public boolean matches(final CraftingInput input, final Level level) {
      return input.width() == 3 && input.height() == 3 && input.ingredientCount() == 4
         ? this.backPattern.test(back(input))
            && this.leftPattern.test(left(input))
            && this.rightPattern.test(right(input))
            && this.frontPattern.test(front(input))
         : false;
   }

   public ItemStack assemble(final CraftingInput input) {
      PotDecorations decorations = new PotDecorations(
         convertToOptional(back(input)), convertToOptional(left(input)), convertToOptional(right(input)), convertToOptional(front(input))
      );
      DataComponentPatch components = DataComponentPatch.builder().set(DataComponents.POT_DECORATIONS, decorations).build();
      return this.result.apply(components);
   }

   private static Optional<ItemStackTemplate> convertToOptional(final ItemStack input) {
      return input.isEmpty() ? Optional.empty() : Optional.of(ItemStackTemplate.fromNonEmptyStack(input, 1));
   }

   @Override
   public RecipeSerializer<DecoratedPotRecipe> getSerializer() {
      return SERIALIZER;
   }
}
