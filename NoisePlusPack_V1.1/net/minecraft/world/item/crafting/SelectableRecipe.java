package net.minecraft.world.item.crafting;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public record SelectableRecipe<T extends Recipe<?>>(SlotDisplay optionDisplay, Optional<RecipeHolder<T>> recipe) {
   public static <T extends Recipe<?>> StreamCodec<RegistryFriendlyByteBuf, SelectableRecipe<T>> noRecipeCodec() {
      return StreamCodec.composite(SlotDisplay.STREAM_CODEC, SelectableRecipe::optionDisplay, p_367448_ -> new SelectableRecipe<>(p_367448_, Optional.empty()));
   }

   public record SingleInputEntry<T extends Recipe<?>>(Ingredient input, SelectableRecipe<T> recipe) {
      public static <T extends Recipe<?>> StreamCodec<RegistryFriendlyByteBuf, SelectableRecipe.SingleInputEntry<T>> noRecipeCodec() {
         return StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            SelectableRecipe.SingleInputEntry::input,
            SelectableRecipe.noRecipeCodec(),
            SelectableRecipe.SingleInputEntry::recipe,
            SelectableRecipe.SingleInputEntry::new
         );
      }
   }

   public record SingleInputSet<T extends Recipe<?>>(List<SelectableRecipe.SingleInputEntry<T>> entries) {
      public static <T extends Recipe<?>> SelectableRecipe.SingleInputSet<T> empty() {
         return new SelectableRecipe.SingleInputSet<>(List.of());
      }

      public static <T extends Recipe<?>> StreamCodec<RegistryFriendlyByteBuf, SelectableRecipe.SingleInputSet<T>> noRecipeCodec() {
         return StreamCodec.composite(
            SelectableRecipe.SingleInputEntry.<T>noRecipeCodec().apply(ByteBufCodecs.list()),
            SelectableRecipe.SingleInputSet::entries,
            SelectableRecipe.SingleInputSet::new
         );
      }

      public boolean acceptsInput(ItemStack p_366997_) {
         return this.entries.stream().anyMatch(p_368592_ -> p_368592_.input.test(p_366997_));
      }

      public SelectableRecipe.SingleInputSet<T> selectByInput(ItemStack p_368693_) {
         return new SelectableRecipe.SingleInputSet<>(this.entries.stream().filter(p_364349_ -> p_364349_.input.test(p_368693_)).toList());
      }

      public boolean isEmpty() {
         return this.entries.isEmpty();
      }

      public int size() {
         return this.entries.size();
      }
   }
}
