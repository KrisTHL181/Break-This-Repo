package net.minecraft.recipebook;

import java.util.Iterator;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

public interface PlaceRecipeHelper {
   static <T> void placeRecipe(int p_369532_, int p_361267_, Recipe<?> p_361929_, Iterable<T> p_367857_, PlaceRecipeHelper.Output<T> p_369633_) {
      if (p_361929_ instanceof ShapedRecipe shapedrecipe) {
         placeRecipe(p_369532_, p_361267_, shapedrecipe.getWidth(), shapedrecipe.getHeight(), p_367857_, p_369633_);
      } else {
         placeRecipe(p_369532_, p_361267_, p_369532_, p_361267_, p_367857_, p_369633_);
      }
   }

   static <T> void placeRecipe(int p_363279_, int p_367196_, int p_360764_, int p_364759_, Iterable<T> p_367701_, PlaceRecipeHelper.Output<T> p_369100_) {
      Iterator<T> iterator = p_367701_.iterator();
      int i = 0;

      for (int j = 0; j < p_367196_; j++) {
         boolean flag = p_364759_ < p_367196_ / 2.0F;
         int k = Mth.floor(p_367196_ / 2.0F - p_364759_ / 2.0F);
         if (flag && k > j) {
            i += p_363279_;
            j++;
         }

         for (int l = 0; l < p_363279_; l++) {
            if (!iterator.hasNext()) {
               return;
            }

            flag = p_360764_ < p_363279_ / 2.0F;
            k = Mth.floor(p_363279_ / 2.0F - p_360764_ / 2.0F);
            int i1 = p_360764_;
            boolean flag1 = l < p_360764_;
            if (flag) {
               i1 = k + p_360764_;
               flag1 = k <= l && l < k + p_360764_;
            }

            if (flag1) {
               p_369100_.addItemToSlot(iterator.next(), i, l, j);
            } else if (i1 == l) {
               i += p_363279_ - l;
               break;
            }

            i++;
         }
      }
   }

   @FunctionalInterface
   interface Output<T> {
      void addItemToSlot(T var1, int var2, int var3, int var4);
   }
}
