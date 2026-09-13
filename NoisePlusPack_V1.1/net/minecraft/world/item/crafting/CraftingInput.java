package net.minecraft.world.item.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;

public class CraftingInput implements RecipeInput {
   public static final CraftingInput EMPTY = new CraftingInput(0, 0, List.of());
   private final int width;
   private final int height;
   private final List<ItemStack> items;
   private final StackedItemContents stackedContents = new StackedItemContents();
   private final int ingredientCount;

   private CraftingInput(int p_344026_, int p_345334_, List<ItemStack> p_343256_) {
      this.width = p_344026_;
      this.height = p_345334_;
      this.items = p_343256_;
      int i = 0;

      for (ItemStack itemstack : p_343256_) {
         if (!itemstack.isEmpty()) {
            i++;
            this.stackedContents.accountStack(itemstack, 1);
         }
      }

      this.ingredientCount = i;
   }

   public static CraftingInput of(int p_345026_, int p_344893_, List<ItemStack> p_343663_) {
      return ofPositioned(p_345026_, p_344893_, p_343663_).input();
   }

   public static CraftingInput.Positioned ofPositioned(int p_345256_, int p_344157_, List<ItemStack> p_342879_) {
      if (p_345256_ != 0 && p_344157_ != 0) {
         int i = p_345256_ - 1;
         int j = 0;
         int k = p_344157_ - 1;
         int l = 0;

         for (int i1 = 0; i1 < p_344157_; i1++) {
            boolean flag = true;

            for (int j1 = 0; j1 < p_345256_; j1++) {
               ItemStack itemstack = p_342879_.get(j1 + i1 * p_345256_);
               if (!itemstack.isEmpty()) {
                  i = Math.min(i, j1);
                  j = Math.max(j, j1);
                  flag = false;
               }
            }

            if (!flag) {
               k = Math.min(k, i1);
               l = Math.max(l, i1);
            }
         }

         int i2 = j - i + 1;
         int j2 = l - k + 1;
         if (i2 <= 0 || j2 <= 0) {
            return CraftingInput.Positioned.EMPTY;
         }

         if (i2 == p_345256_ && j2 == p_344157_) {
            return new CraftingInput.Positioned(new CraftingInput(p_345256_, p_344157_, p_342879_), i, k);
         }

         List<ItemStack> list = new ArrayList<>(i2 * j2);

         for (int k2 = 0; k2 < j2; k2++) {
            for (int k1 = 0; k1 < i2; k1++) {
               int l1 = k1 + i + (k2 + k) * p_345256_;
               list.add(p_342879_.get(l1));
            }
         }

         return new CraftingInput.Positioned(new CraftingInput(i2, j2, list), i, k);
      } else {
         return CraftingInput.Positioned.EMPTY;
      }
   }

   @Override
   public ItemStack getItem(int p_342671_) {
      return this.items.get(p_342671_);
   }

   public ItemStack getItem(int p_343752_, int p_345443_) {
      return this.items.get(p_343752_ + p_345443_ * this.width);
   }

   @Override
   public int size() {
      return this.items.size();
   }

   @Override
   public boolean isEmpty() {
      return this.ingredientCount == 0;
   }

   public StackedItemContents stackedContents() {
      return this.stackedContents;
   }

   public List<ItemStack> items() {
      return this.items;
   }

   public int ingredientCount() {
      return this.ingredientCount;
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   @Override
   public boolean equals(Object p_343121_) {
      if (p_343121_ == this) {
         return true;
      } else {
         return p_343121_ instanceof CraftingInput craftinginput
            ? this.width == craftinginput.width
               && this.height == craftinginput.height
               && this.ingredientCount == craftinginput.ingredientCount
               && ItemStack.listMatches(this.items, craftinginput.items)
            : false;
      }
   }

   @Override
   public int hashCode() {
      int i = ItemStack.hashStackList(this.items);
      i = 31 * i + this.width;
      return 31 * i + this.height;
   }

   public record Positioned(CraftingInput input, int left, int top) {
      public static final CraftingInput.Positioned EMPTY = new CraftingInput.Positioned(CraftingInput.EMPTY, 0, 0);
   }
}
