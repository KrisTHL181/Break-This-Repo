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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientRecipeBook extends RecipeBook {
   private final Map<RecipeDisplayId, RecipeDisplayEntry> known = new HashMap<>();
   private final Set<RecipeDisplayId> highlight = new HashSet<>();
   private Map<ExtendedRecipeBookCategory, List<RecipeCollection>> collectionsByTab = Map.of();
   private List<RecipeCollection> allCollections = List.of();

   public void add(RecipeDisplayEntry p_367545_) {
      this.known.put(p_367545_.id(), p_367545_);
   }

   public void remove(RecipeDisplayId p_365017_) {
      this.known.remove(p_365017_);
      this.highlight.remove(p_365017_);
   }

   public void clear() {
      this.known.clear();
      this.highlight.clear();
   }

   public boolean willHighlight(RecipeDisplayId p_364304_) {
      return this.highlight.contains(p_364304_);
   }

   public void removeHighlight(RecipeDisplayId p_365808_) {
      this.highlight.remove(p_365808_);
   }

   public void addHighlight(RecipeDisplayId p_364710_) {
      this.highlight.add(p_364710_);
   }

   public void rebuildCollections() {
      Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> map = categorizeAndGroupRecipes(this.known.values());
      Map<ExtendedRecipeBookCategory, List<RecipeCollection>> map1 = new HashMap<>();
      Builder<RecipeCollection> builder = ImmutableList.builder();
      map.forEach(
         (p_357635_, p_357636_) -> map1.put(
            p_357635_, (List)p_357636_.stream().map(RecipeCollection::new).peek(builder::add).collect(ImmutableList.toImmutableList())
         )
      );

      for (SearchRecipeBookCategory searchrecipebookcategory : SearchRecipeBookCategory.values()) {
         map1.put(
            searchrecipebookcategory,
            searchrecipebookcategory.includedCategories()
               .stream()
               .flatMap(p_357639_ -> map1.getOrDefault(p_357639_, List.of()).stream())
               .collect(ImmutableList.toImmutableList())
         );
      }

      this.collectionsByTab = Map.copyOf(map1);
      this.allCollections = builder.build();
   }

   private static Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> categorizeAndGroupRecipes(Iterable<RecipeDisplayEntry> p_90643_) {
      Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> map = new HashMap<>();
      Table<RecipeBookCategory, Integer, List<RecipeDisplayEntry>> table = HashBasedTable.create();

      for (RecipeDisplayEntry recipedisplayentry : p_90643_) {
         RecipeBookCategory recipebookcategory = recipedisplayentry.category();
         OptionalInt optionalint = recipedisplayentry.group();
         if (optionalint.isEmpty()) {
            map.computeIfAbsent(recipebookcategory, p_357637_ -> new ArrayList<>()).add(List.of(recipedisplayentry));
         } else {
            List<RecipeDisplayEntry> list = (List<RecipeDisplayEntry>)table.get(recipebookcategory, optionalint.getAsInt());
            if (list == null) {
               list = new ArrayList<>();
               table.put(recipebookcategory, optionalint.getAsInt(), list);
               map.computeIfAbsent(recipebookcategory, p_357640_ -> new ArrayList<>()).add(list);
            }

            list.add(recipedisplayentry);
         }
      }

      return map;
   }

   public List<RecipeCollection> getCollections() {
      return this.allCollections;
   }

   public List<RecipeCollection> getCollection(ExtendedRecipeBookCategory p_362967_) {
      return this.collectionsByTab.getOrDefault(p_362967_, Collections.emptyList());
   }
}
