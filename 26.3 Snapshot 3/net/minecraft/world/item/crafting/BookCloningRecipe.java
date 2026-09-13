package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

public class BookCloningRecipe extends CustomRecipe {
   public static final MinMaxBounds.Ints ALLOWED_BOOK_GENERATION_RANGES = MinMaxBounds.Ints.between(0, 2);
   public static final MinMaxBounds.Ints DEFAULT_BOOK_GENERATION_RANGES = MinMaxBounds.Ints.between(0, 1);
   private static final Codec<MinMaxBounds.Ints> ALLOWED_GENERATION_CODEC = MinMaxBounds.Ints.CODEC
      .validate(MinMaxBounds.validateContainedInRange(ALLOWED_BOOK_GENERATION_RANGES));
   public static final MapCodec<BookCloningRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Ingredient.CODEC.fieldOf("source").forGetter(o -> o.source),
            Ingredient.CODEC.fieldOf("material").forGetter(o -> o.material),
            ALLOWED_GENERATION_CODEC.optionalFieldOf("allowed_generations", DEFAULT_BOOK_GENERATION_RANGES).forGetter(o -> o.allowedGenerations),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
         )
         .apply(i, BookCloningRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, BookCloningRecipe> STREAM_CODEC = StreamCodec.composite(
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.source,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.material,
      MinMaxBounds.Ints.STREAM_CODEC,
      o -> o.allowedGenerations,
      ItemStackTemplate.STREAM_CODEC,
      o -> o.result,
      BookCloningRecipe::new
   );
   public static final RecipeSerializer<BookCloningRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
   private final Ingredient source;
   private final Ingredient material;
   private final MinMaxBounds.Ints allowedGenerations;
   private final ItemStackTemplate result;

   public BookCloningRecipe(final Ingredient source, final Ingredient material, final MinMaxBounds.Ints allowedGenerations, final ItemStackTemplate result) {
      this.source = source;
      this.material = material;
      this.allowedGenerations = allowedGenerations;
      this.result = result;
   }

   private boolean canCraftCopy(final WrittenBookContent writtenBookContent) {
      return this.allowedGenerations.matches(writtenBookContent.generation());
   }

   public boolean matches(final CraftingInput input, final Level level) {
      if (input.ingredientCount() < 2) {
         return false;
      }

      boolean hasMaterial = false;
      boolean hasSource = false;

      for (int slot = 0; slot < input.size(); slot++) {
         ItemStack itemStack = input.getItem(slot);
         if (!itemStack.isEmpty()) {
            if (this.source.test(itemStack)) {
               WrittenBookContent writtenBookContent = itemStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
               if (writtenBookContent == null || !this.canCraftCopy(writtenBookContent)) {
                  return false;
               }

               if (hasSource) {
                  return false;
               }

               hasSource = true;
            } else {
               if (!this.material.test(itemStack)) {
                  return false;
               }

               hasMaterial = true;
            }
         }
      }

      return hasSource && hasMaterial;
   }

   public ItemStack assemble(final CraftingInput input) {
      int count = 0;
      ItemStack source = ItemStack.EMPTY;

      for (int slot = 0; slot < input.size(); slot++) {
         ItemStack itemStack = input.getItem(slot);
         if (!itemStack.isEmpty()) {
            if (this.source.test(itemStack) && itemStack.has(DataComponents.WRITTEN_BOOK_CONTENT)) {
               if (!source.isEmpty()) {
                  return ItemStack.EMPTY;
               }

               source = itemStack;
            } else {
               if (!this.material.test(itemStack)) {
                  return ItemStack.EMPTY;
               }

               count++;
            }
         }
      }

      WrittenBookContent sourceContent = source.get(DataComponents.WRITTEN_BOOK_CONTENT);
      if (sourceContent == null) {
         return ItemStack.EMPTY;
      }

      WrittenBookContent copiedContent = sourceContent.craftCopy();
      ItemStack result = TransmuteRecipe.createWithOriginalComponents(this.result, source, count - 1);
      result.set(DataComponents.WRITTEN_BOOK_CONTENT, copiedContent);
      return result;
   }

   @Override
   public NonNullList<ItemStack> getRemainingItems(final CraftingInput input) {
      NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);

      for (int slot = 0; slot < result.size(); slot++) {
         ItemStack itemStack = input.getItem(slot);
         ItemStackTemplate remainder = itemStack.getItem().getCraftingRemainder();
         if (remainder != null) {
            result.set(slot, remainder.create());
         } else if (itemStack.has(DataComponents.WRITTEN_BOOK_CONTENT)) {
            result.set(slot, itemStack.copyWithCount(1));
            break;
         }
      }

      return result;
   }

   @Override
   public RecipeSerializer<BookCloningRecipe> getSerializer() {
      return SERIALIZER;
   }
}
