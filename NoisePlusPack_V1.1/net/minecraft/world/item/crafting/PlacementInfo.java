package net.minecraft.world.item.crafting;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlacementInfo {
   public static final int EMPTY_SLOT = -1;
   public static final PlacementInfo NOT_PLACEABLE = new PlacementInfo(List.of(), IntList.of());
   private final List<Ingredient> ingredients;
   private final IntList slotsToIngredientIndex;

   private PlacementInfo(List<Ingredient> p_365245_, IntList p_378164_) {
      this.ingredients = p_365245_;
      this.slotsToIngredientIndex = p_378164_;
   }

   public static PlacementInfo create(Ingredient p_361591_) {
      return p_361591_.isEmpty() ? NOT_PLACEABLE : new PlacementInfo(List.of(p_361591_), IntList.of(0));
   }

   public static PlacementInfo createFromOptionals(List<Optional<Ingredient>> p_362899_) {
      int i = p_362899_.size();
      List<Ingredient> list = new ArrayList<>(i);
      IntList intlist = new IntArrayList(i);
      int j = 0;

      for (Optional<Ingredient> optional : p_362899_) {
         if (optional.isPresent()) {
            Ingredient ingredient = optional.get();
            if (ingredient.isEmpty()) {
               return NOT_PLACEABLE;
            }

            list.add(ingredient);
            intlist.add(j++);
         } else {
            intlist.add(-1);
         }
      }

      return new PlacementInfo(list, intlist);
   }

   public static PlacementInfo create(List<Ingredient> p_366350_) {
      int i = p_366350_.size();
      IntList intlist = new IntArrayList(i);

      for (int j = 0; j < i; j++) {
         Ingredient ingredient = p_366350_.get(j);
         if (ingredient.isEmpty()) {
            return NOT_PLACEABLE;
         }

         intlist.add(j);
      }

      return new PlacementInfo(p_366350_, intlist);
   }

   public IntList slotsToIngredientIndex() {
      return this.slotsToIngredientIndex;
   }

   public List<Ingredient> ingredients() {
      return this.ingredients;
   }

   public boolean isImpossibleToPlace() {
      return this.slotsToIngredientIndex.isEmpty();
   }
}
