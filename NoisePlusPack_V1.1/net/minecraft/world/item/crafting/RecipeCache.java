package net.minecraft.world.item.crafting;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class RecipeCache {
   private final RecipeCache.@Nullable Entry[] entries;
   private WeakReference<@Nullable RecipeManager> cachedRecipeManager = new WeakReference<>(null);

   public RecipeCache(int p_309405_) {
      this.entries = new RecipeCache.Entry[p_309405_];
   }

   public Optional<RecipeHolder<CraftingRecipe>> get(ServerLevel p_364836_, CraftingInput p_342819_) {
      if (p_342819_.isEmpty()) {
         return Optional.empty();
      }

      this.validateRecipeManager(p_364836_);

      for (int i = 0; i < this.entries.length; i++) {
         RecipeCache.Entry recipecache$entry = this.entries[i];
         if (recipecache$entry != null && recipecache$entry.matches(p_342819_)) {
            this.moveEntryToFront(i);
            return Optional.ofNullable(recipecache$entry.value());
         }
      }

      return this.compute(p_342819_, p_364836_);
   }

   private void validateRecipeManager(ServerLevel p_369590_) {
      RecipeManager recipemanager = p_369590_.recipeAccess();
      if (recipemanager != this.cachedRecipeManager.get()) {
         this.cachedRecipeManager = new WeakReference<>(recipemanager);
         Arrays.fill(this.entries, null);
      }
   }

   private Optional<RecipeHolder<CraftingRecipe>> compute(CraftingInput p_345296_, ServerLevel p_362775_) {
      Optional<RecipeHolder<CraftingRecipe>> optional = p_362775_.recipeAccess().getRecipeFor(RecipeType.CRAFTING, p_345296_, p_362775_);
      this.insert(p_345296_, optional.orElse(null));
      return optional;
   }

   private void moveEntryToFront(int p_309395_) {
      if (p_309395_ > 0) {
         RecipeCache.Entry recipecache$entry = this.entries[p_309395_];
         System.arraycopy(this.entries, 0, this.entries, 1, p_309395_);
         this.entries[0] = recipecache$entry;
      }
   }

   private void insert(CraftingInput p_342978_, @Nullable RecipeHolder<CraftingRecipe> p_330177_) {
      NonNullList<ItemStack> nonnulllist = NonNullList.withSize(p_342978_.size(), ItemStack.EMPTY);

      for (int i = 0; i < p_342978_.size(); i++) {
         nonnulllist.set(i, p_342978_.getItem(i).copyWithCount(1));
      }

      System.arraycopy(this.entries, 0, this.entries, 1, this.entries.length - 1);
      this.entries[0] = new RecipeCache.Entry(nonnulllist, p_342978_.width(), p_342978_.height(), p_330177_);
   }

   record Entry(NonNullList<ItemStack> key, int width, int height, @Nullable RecipeHolder<CraftingRecipe> value) {
      public boolean matches(CraftingInput p_344906_) {
         if (this.width == p_344906_.width() && this.height == p_344906_.height()) {
            for (int i = 0; i < this.key.size(); i++) {
               if (!ItemStack.isSameItemSameComponents(this.key.get(i), p_344906_.getItem(i))) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }
}
