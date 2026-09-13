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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeCollection {
   public static final RecipeCollection EMPTY = new RecipeCollection(List.of());
   private final List<RecipeDisplayEntry> entries;
   private final Set<RecipeDisplayId> craftable = new HashSet<>();
   private final Set<RecipeDisplayId> selected = new HashSet<>();

   public RecipeCollection(List<RecipeDisplayEntry> p_267051_) {
      this.entries = p_267051_;
   }

   public void selectRecipes(StackedItemContents p_361916_, Predicate<RecipeDisplay> p_365877_) {
      for (RecipeDisplayEntry recipedisplayentry : this.entries) {
         boolean flag = p_365877_.test(recipedisplayentry.display());
         if (flag) {
            this.selected.add(recipedisplayentry.id());
         } else {
            this.selected.remove(recipedisplayentry.id());
         }

         if (flag && recipedisplayentry.canCraft(p_361916_)) {
            this.craftable.add(recipedisplayentry.id());
         } else {
            this.craftable.remove(recipedisplayentry.id());
         }
      }
   }

   public boolean isCraftable(RecipeDisplayId p_366818_) {
      return this.craftable.contains(p_366818_);
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

   public List<RecipeDisplayEntry> getSelectedRecipes(RecipeCollection.CraftableStatus p_369775_) {
      Predicate<RecipeDisplayId> predicate = switch (p_369775_) {
         case ANY -> this.selected::contains;
         case CRAFTABLE -> this.craftable::contains;
         case NOT_CRAFTABLE -> p_361783_ -> this.selected.contains(p_361783_) && !this.craftable.contains(p_361783_);
      };
      List<RecipeDisplayEntry> list = new ArrayList<>();

      for (RecipeDisplayEntry recipedisplayentry : this.entries) {
         if (predicate.test(recipedisplayentry.id())) {
            list.add(recipedisplayentry);
         }
      }

      return list;
   }

   @OnlyIn(Dist.CLIENT)
   public enum CraftableStatus {
      ANY,
      CRAFTABLE,
      NOT_CRAFTABLE;
   }
}
