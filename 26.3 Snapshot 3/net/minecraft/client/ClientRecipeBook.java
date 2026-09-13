package net.minecraft.client;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.common.collect.ImmutableList.Builder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

public class ClientRecipeBook extends RecipeBook {
   private final Map<RecipeDisplayId, RecipeDisplayEntry> known = new HashMap<>();
   private final Set<RecipeDisplayId> highlight = new HashSet<>();
   private Map<ExtendedRecipeBookCategory, List<RecipeCollection>> collectionsByTab = Map.of();
   private List<RecipeCollection> allCollections = List.of();

   public void add(final RecipeDisplayEntry display) {
      this.known.put(display.id(), display);
   }

   public void remove(final RecipeDisplayId id) {
      this.known.remove(id);
      this.highlight.remove(id);
   }

   public void clear() {
      this.known.clear();
      this.highlight.clear();
   }

   public boolean willHighlight(final RecipeDisplayId recipe) {
      return this.highlight.contains(recipe);
   }

   public void removeHighlight(final RecipeDisplayId id) {
      this.highlight.remove(id);
   }

   public void addHighlight(final RecipeDisplayId id) {
      this.highlight.add(id);
   }

   public void rebuildCollections() {
      Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> recipeListsByCategory = categorizeAndGroupRecipes(this.known.values());
      Map<ExtendedRecipeBookCategory, List<RecipeCollection>> byCategory = new HashMap<>();
      Builder<RecipeCollection> all = ImmutableList.builder();
      recipeListsByCategory.forEach(
         (category, categoryRecipes) -> byCategory.put(
            category, (List)categoryRecipes.stream().map(RecipeCollection::new).peek(all::add).collect(ImmutableList.toImmutableList())
         )
      );

      for (SearchRecipeBookCategory searchCategory : SearchRecipeBookCategory.values()) {
         byCategory.put(
            searchCategory,
            searchCategory.includedCategories()
               .stream()
               .flatMap(subCategory -> byCategory.getOrDefault(subCategory, List.of()).stream())
               .collect(ImmutableList.toImmutableList())
         );
      }

      this.collectionsByTab = Map.copyOf(byCategory);
      this.allCollections = all.build();
   }

   private static Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> categorizeAndGroupRecipes(final Iterable<RecipeDisplayEntry> recipes) {
      Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> result = new HashMap<>();
      Table<RecipeBookCategory, Integer, List<RecipeDisplayEntry>> multiItemGroups = HashBasedTable.create();

      for (RecipeDisplayEntry entry : recipes) {
         RecipeBookCategory category = entry.category();
         OptionalInt groupId = entry.group();
         if (groupId.isEmpty()) {
            result.computeIfAbsent(category, key -> new ArrayList<>()).add(List.of(entry));
         } else {
            List<RecipeDisplayEntry> groupRecipes = (List<RecipeDisplayEntry>)multiItemGroups.get(category, groupId.getAsInt());
            if (groupRecipes == null) {
               groupRecipes = new ArrayList<>();
               multiItemGroups.put(category, groupId.getAsInt(), groupRecipes);
               result.computeIfAbsent(category, key -> new ArrayList<>()).add(groupRecipes);
            }

            groupRecipes.add(entry);
         }
      }

      return result;
   }

   public List<RecipeCollection> getCollections() {
      return this.allCollections;
   }

   public List<RecipeCollection> getCollection(final ExtendedRecipeBookCategory category) {
      return this.collectionsByTab.getOrDefault(category, Collections.emptyList());
   }
}
