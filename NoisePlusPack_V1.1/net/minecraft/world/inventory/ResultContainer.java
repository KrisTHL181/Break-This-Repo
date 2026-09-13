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
      for (ItemStack itemstack : this.itemStacks) {
         if (!itemstack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public ItemStack getItem(int p_40147_) {
      return this.itemStacks.get(0);
   }

   @Override
   public ItemStack removeItem(int p_40149_, int p_40150_) {
      return ContainerHelper.takeItem(this.itemStacks, 0);
   }

   @Override
   public ItemStack removeItemNoUpdate(int p_40160_) {
      return ContainerHelper.takeItem(this.itemStacks, 0);
   }

   @Override
   public void setItem(int p_40152_, ItemStack p_40153_) {
      this.itemStacks.set(0, p_40153_);
   }

   @Override
   public void setChanged() {
   }

   @Override
   public boolean stillValid(Player p_40155_) {
      return true;
   }

   @Override
   public void clearContent() {
      this.itemStacks.clear();
   }

   @Override
   public void setRecipeUsed(@Nullable RecipeHolder<?> p_297508_) {
      this.recipeUsed = p_297508_;
   }

   @Override
   public @Nullable RecipeHolder<?> getRecipeUsed() {
      return this.recipeUsed;
   }
}
