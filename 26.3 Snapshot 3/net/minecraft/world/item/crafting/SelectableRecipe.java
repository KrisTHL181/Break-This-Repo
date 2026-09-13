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
      return StreamCodec.composite(
         SlotDisplay.STREAM_CODEC, SelectableRecipe::optionDisplay, slotDisplay -> new SelectableRecipe<>(slotDisplay, Optional.empty())
      );
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

      public boolean acceptsInput(final ItemStack input) {
         return this.entries.stream().anyMatch(e -> e.input.test(input));
      }

      public SelectableRecipe.SingleInputSet<T> selectByInput(final ItemStack input) {
         return new SelectableRecipe.SingleInputSet<>(this.entries.stream().filter(e -> e.input.test(input)).toList());
      }

      public boolean isEmpty() {
         return this.entries.isEmpty();
      }

      public int size() {
         return this.entries.size();
      }
   }
}
