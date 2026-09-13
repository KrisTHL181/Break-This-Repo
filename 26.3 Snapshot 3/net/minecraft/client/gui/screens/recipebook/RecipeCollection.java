package net.minecraft.client.gui.screens.recipebook;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

public class RecipeCollection {
   public static final RecipeCollection EMPTY = new RecipeCollection(List.of());
   private final List<RecipeDisplayEntry> entries;
   private final Set<RecipeDisplayId> craftable = new HashSet<>();
   private final Set<RecipeDisplayId> selected = new HashSet<>();

   public RecipeCollection(final List<RecipeDisplayEntry> recipes) {
      this.entries = recipes;
   }

   public void selectRecipes(final StackedItemContents stackedContents, final Predicate<RecipeDisplay> selector) {
      for (RecipeDisplayEntry entry : this.entries) {
         boolean isSelected = selector.test(entry.display());
         if (isSelected) {
            this.selected.add(entry.id());
         } else {
            this.selected.remove(entry.id());
         }

         if (isSelected && entry.canCraft(stackedContents)) {
            this.craftable.add(entry.id());
         } else {
            this.craftable.remove(entry.id());
         }
      }
   }

   public boolean isCraftable(final RecipeDisplayId recipe) {
      return this.craftable.contains(recipe);
   }

   public boolean hasCraftable() {
      return !this.craftable.isEmpty();
   }

   public boolean hasAnySelected() {
      return !this.selected.isEmpty();
   }

   public List<RecipeDisplayEntry> getRecipes() {
      return this.entries;
   }

   public List<RecipeDisplayEntry> getSelectedRecipes(final RecipeCollection.CraftableStatus selector) {
      Predicate<RecipeDisplayId> predicate = switch (selector) {
         case ANY -> this.selected::contains;
         case CRAFTABLE -> this.craftable::contains;
         case NOT_CRAFTABLE -> recipe -> this.selected.contains(recipe) && !this.craftable.contains(recipe);
      };
      List<RecipeDisplayEntry> result = new ArrayList<>();

      for (RecipeDisplayEntry entries : this.entries) {
         if (predicate.test(entries.id())) {
            result.add(entries);
         }
      }

      return result;
   }

   public enum CraftableStatus {
      ANY,
      CRAFTABLE,
      NOT_CRAFTABLE;
   }
}
