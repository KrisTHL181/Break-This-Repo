package net.minecraft.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

public class ResultContainer implements Container, RecipeCraftingHolder {
   private final NonNullList<ItemStack> itemStacks = NonNullList.withSize(1, ItemStack.EMPTY);
   private @Nullable RecipeHolder<?> recipeUsed;

   @Override
   public int getContainerSize() {
      return 1;
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack itemStack : this.itemStacks) {
         if (!itemStack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public ItemStack getItem(final int slot) {
      return this.itemStacks.get(0);
   }

   @Override
   public ItemStack removeItem(final int slot, final int count) {
      return ContainerHelper.takeItem(this.itemStacks, 0);
   }

   @Override
   public ItemStack removeItemNoUpdate(final int slot) {
      return ContainerHelper.takeItem(this.itemStacks, 0);
   }

   @Override
   public void setItem(final int slot, final ItemStack itemStack) {
      this.itemStacks.set(0, itemStack);
   }

   @Override
   public void setChanged() {
   }

   @Override
   public boolean stillValid(final Player player) {
      return true;
   }

   @Override
   public void clearContent() {
      this.itemStacks.clear();
   }

   @Override
   public void setRecipeUsed(final @Nullable RecipeHolder<?> recipeUsed) {
      this.recipeUsed = recipeUsed;
   }

   @Override
   public @Nullable RecipeHolder<?> getRecipeUsed() {
      return this.recipeUsed;
   }
}
