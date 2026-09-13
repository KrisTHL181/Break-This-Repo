package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RecipeManager extends SimplePreparableReloadListener<RecipeMap> implements RecipeAccess {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<ResourceKey<RecipePropertySet>, RecipeManager.IngredientExtractor> RECIPE_PROPERTY_SETS = Map.of(
      RecipePropertySet.SMITHING_ADDITION,
      p_359832_ -> p_359832_ instanceof SmithingRecipe smithingrecipe ? smithingrecipe.additionIngredient() : Optional.empty(),
      RecipePropertySet.SMITHING_BASE,
      p_390824_ -> p_390824_ instanceof SmithingRecipe smithingrecipe ? Optional.of(smithingrecipe.baseIngredient()) : Optional.empty(),
      RecipePropertySet.SMITHING_TEMPLATE,
      p_359833_ -> p_359833_ instanceof SmithingRecipe smithingrecipe ? smithingrecipe.templateIngredient() : Optional.empty(),
      RecipePropertySet.FURNACE_INPUT,
      forSingleInput(RecipeType.SMELTING),
      RecipePropertySet.BLAST_FURNACE_INPUT,
      forSingleInput(RecipeType.BLASTING),
      RecipePropertySet.SMOKER_INPUT,
      forSingleInput(RecipeType.SMOKING),
      RecipePropertySet.CAMPFIRE_INPUT,
      forSingleInput(RecipeType.CAMPFIRE_COOKING)
   );
   private static final FileToIdConverter RECIPE_LISTER = FileToIdConverter.registry(Registries.RECIPE);
   private final HolderLookup.Provider registries;
   private RecipeMap recipes = RecipeMap.EMPTY;
   private Map<ResourceKey<RecipePropertySet>, RecipePropertySet> propertySets = Map.of();
   private SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes = SelectableRecipe.SingleInputSet.empty();
   private List<RecipeManager.ServerDisplayInfo> allDisplays = List.of();
   private Map<ResourceKey<Recipe<?>>, List<RecipeManager.ServerDisplayInfo>> recipeToDisplay = Map.of();

   public RecipeManager(HolderLookup.Provider p_330459_) {
      this.registries = p_330459_;
   }

   protected RecipeMap prepare(ResourceManager p_368640_, ProfilerFiller p_361102_) {
      SortedMap<Identifier, Recipe<?>> sortedmap = new TreeMap<>();
      SimpleJsonResourceReloadListener.scanDirectory(
         p_368640_, RECIPE_LISTER, this.registries.createSerializationContext(JsonOps.INSTANCE), Recipe.CODEC, sortedmap
      );
      List<RecipeHolder<?>> list = new ArrayList<>(sortedmap.size());
      sortedmap.forEach((p_449831_, p_449832_) -> {
         ResourceKey<Recipe<?>> resourcekey = ResourceKey.create(Registries.RECIPE, p_449831_);
         RecipeHolder<?> recipeholder = new RecipeHolder<>(resourcekey, p_449832_);
         list.add(recipeholder);
      });
      return RecipeMap.create(list);
   }

   protected void apply(RecipeMap p_369166_, ResourceManager p_44038_, ProfilerFiller p_44039_) {
      this.recipes = p_369166_;
      LOGGER.info("Loaded {} recipes", p_369166_.values().size());
   }

   public void finalizeRecipeLoading(FeatureFlagSet p_360842_) {
      List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> list = new ArrayList<>();
      List<RecipeManager.IngredientCollector> list1 = RECIPE_PROPERTY_SETS.entrySet()
         .stream()
         .map(p_359831_ -> new RecipeManager.IngredientCollector(p_359831_.getKey(), p_359831_.getValue()))
         .toList();
      this.recipes
         .values()
         .forEach(
            p_359840_ -> {
               Recipe<?> recipe = p_359840_.value();
               if (!recipe.isSpecial() && recipe.placementInfo().isImpossibleToPlace()) {
                  LOGGER.warn("Recipe {} can't be placed due to empty ingredients and will be ignored", p_359840_.id().identifier());
               } else {
                  list1.forEach(p_359842_ -> p_359842_.accept(recipe));
                  if (recipe instanceof StonecutterRecipe stonecutterrecipe
                     && isIngredientEnabled(p_360842_, stonecutterrecipe.input())
                     && stonecutterrecipe.resultDisplay().isEnabled(p_360842_)) {
                     list.add(
                        new SelectableRecipe.SingleInputEntry<>(
                           stonecutterrecipe.input(),
                           new SelectableRecipe<>(stonecutterrecipe.resultDisplay(), Optional.of((RecipeHolder<StonecutterRecipe>)p_359840_))
                        )
                     );
                  }
               }
            }
         );
      this.propertySets = list1.stream().collect(Collectors.toUnmodifiableMap(p_359830_ -> p_359830_.key, p_359826_ -> p_359826_.asPropertySet(p_360842_)));
      this.stonecutterRecipes = new SelectableRecipe.SingleInputSet<>(list);
      this.allDisplays = unpackRecipeInfo(this.recipes.values(), p_360842_);
      this.recipeToDisplay = this.allDisplays
         .stream()
         .collect(Collectors.groupingBy(p_359820_ -> p_359820_.parent.id(), IdentityHashMap::new, Collectors.toList()));
   }

   static List<Ingredient> filterDisabled(FeatureFlagSet p_369580_, List<Ingredient> p_367920_) {
      p_367920_.removeIf(p_359829_ -> !isIngredientEnabled(p_369580_, p_359829_));
      return p_367920_;
   }

   private static boolean isIngredientEnabled(FeatureFlagSet p_361535_, Ingredient p_369900_) {
      return p_369900_.items().allMatch(p_359822_ -> p_359822_.value().isEnabled(p_361535_));
   }

   public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(
      RecipeType<T> p_220249_, I p_344518_, Level p_220251_, @Nullable ResourceKey<Recipe<?>> p_361142_
   ) {
      RecipeHolder<T> recipeholder = p_361142_ != null ? this.byKeyTyped(p_220249_, p_361142_) : null;
      return this.getRecipeFor(p_220249_, p_344518_, p_220251_, recipeholder);
   }

   public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(
      RecipeType<T> p_343647_, I p_342793_, Level p_344483_, @Nullable RecipeHolder<T> p_345187_
   ) {
      return p_345187_ != null && p_345187_.value().matches(p_342793_, p_344483_) ? Optional.of(p_345187_) : this.getRecipeFor(p_343647_, p_342793_, p_344483_);
   }

   public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> p_44016_, I p_344358_, Level p_44018_) {
      return this.recipes.getRecipesFor(p_44016_, p_344358_, p_44018_).findFirst();
   }

   public Optional<RecipeHolder<?>> byKey(ResourceKey<Recipe<?>> p_364678_) {
      return Optional.ofNullable(this.recipes.byKey(p_364678_));
   }

   private <T extends Recipe<?>> @Nullable RecipeHolder<T> byKeyTyped(RecipeType<T> p_332930_, ResourceKey<Recipe<?>> p_367936_) {
      RecipeHolder<?> recipeholder = this.recipes.byKey(p_367936_);
      return (RecipeHolder<T>)(recipeholder != null && recipeholder.value().getType().equals(p_332930_) ? recipeholder : null);
   }

   public Map<ResourceKey<RecipePropertySet>, RecipePropertySet> getSynchronizedItemProperties() {
      return this.propertySets;
   }

   public SelectableRecipe.SingleInputSet<StonecutterRecipe> getSynchronizedStonecutterRecipes() {
      return this.stonecutterRecipes;
   }

   @Override
   public RecipePropertySet propertySet(ResourceKey<RecipePropertySet> p_367484_) {
      return this.propertySets.getOrDefault(p_367484_, RecipePropertySet.EMPTY);
   }

   @Override
   public SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes() {
      return this.stonecutterRecipes;
   }

   public Collection<RecipeHolder<?>> getRecipes() {
      return this.recipes.values();
   }

   public RecipeManager.@Nullable ServerDisplayInfo getRecipeFromDisplay(RecipeDisplayId p_362633_) {
      int i = p_362633_.index();
      return i >= 0 && i < this.allDisplays.size() ? this.allDisplays.get(i) : null;
   }

   public void listDisplaysForRecipe(ResourceKey<Recipe<?>> p_360782_, Consumer<RecipeDisplayEntry> p_368559_) {
      List<RecipeManager.ServerDisplayInfo> list = this.recipeToDisplay.get(p_360782_);
      if (list != null) {
         list.forEach(p_359824_ -> p_368559_.accept(p_359824_.display));
      }
   }

   @VisibleForTesting
   protected static RecipeHolder<?> fromJson(ResourceKey<Recipe<?>> p_366256_, JsonObject p_44047_, HolderLookup.Provider p_328308_) {
      Recipe<?> recipe = (Recipe<?>)Recipe.CODEC.parse(p_328308_.createSerializationContext(JsonOps.INSTANCE), p_44047_).getOrThrow(JsonParseException::new);
      return new RecipeHolder<>(p_366256_, recipe);
   }

   public static <I extends RecipeInput, T extends Recipe<I>> RecipeManager.CachedCheck<I, T> createCheck(final RecipeType<T> p_220268_) {
      return new RecipeManager.CachedCheck<I, T>() {
         private @Nullable ResourceKey<Recipe<?>> lastRecipe;

         @Override
         public Optional<RecipeHolder<T>> getRecipeFor(I p_343525_, ServerLevel p_364008_) {
            RecipeManager recipemanager = p_364008_.recipeAccess();
            Optional<RecipeHolder<T>> optional = recipemanager.getRecipeFor(p_220268_, p_343525_, p_364008_, this.lastRecipe);
            if (optional.isPresent()) {
               RecipeHolder<T> recipeholder = optional.get();
               this.lastRecipe = recipeholder.id();
               return Optional.of(recipeholder);
            } else {
               return Optional.empty();
            }
         }
      };
   }

   private static List<RecipeManager.ServerDisplayInfo> unpackRecipeInfo(Iterable<RecipeHolder<?>> p_361848_, FeatureFlagSet p_362319_) {
      List<RecipeManager.ServerDisplayInfo> list = new ArrayList<>();
      Object2IntMap<String> object2intmap = new Object2IntOpenHashMap();

      for (RecipeHolder<?> recipeholder : p_361848_) {
         Recipe<?> recipe = recipeholder.value();
         OptionalInt optionalint;
         if (recipe.group().isEmpty()) {
            optionalint = OptionalInt.empty();
         } else {
            optionalint = OptionalInt.of(object2intmap.computeIfAbsent(recipe.group(), p_359844_ -> object2intmap.size()));
         }

         Optional<List<Ingredient>> optional;
         if (recipe.isSpecial()) {
            optional = Optional.empty();
         } else {
            optional = Optional.of(recipe.placementInfo().ingredients());
         }

         for (RecipeDisplay recipedisplay : recipe.display()) {
            if (recipedisplay.isEnabled(p_362319_)) {
               int i = list.size();
               RecipeDisplayId recipedisplayid = new RecipeDisplayId(i);
               RecipeDisplayEntry recipedisplayentry = new RecipeDisplayEntry(
                  recipedisplayid, recipedisplay, optionalint, recipe.recipeBookCategory(), optional
               );
               list.add(new RecipeManager.ServerDisplayInfo(recipedisplayentry, recipeholder));
            }
         }
      }

      return list;
   }

   private static RecipeManager.IngredientExtractor forSingleInput(RecipeType<? extends SingleItemRecipe> p_361054_) {
      return p_359846_ -> p_359846_.getType() == p_361054_ && p_359846_ instanceof SingleItemRecipe singleitemrecipe
         ? Optional.of(singleitemrecipe.input())
         : Optional.empty();
   }

   public interface CachedCheck<I extends RecipeInput, T extends Recipe<I>> {
      Optional<RecipeHolder<T>> getRecipeFor(I var1, ServerLevel var2);
   }

   public static class IngredientCollector implements Consumer<Recipe<?>> {
      final ResourceKey<RecipePropertySet> key;
      private final RecipeManager.IngredientExtractor extractor;
      private final List<Ingredient> ingredients = new ArrayList<>();

      protected IngredientCollector(ResourceKey<RecipePropertySet> p_364661_, RecipeManager.IngredientExtractor p_368104_) {
         this.key = p_364661_;
         this.extractor = p_368104_;
      }

      public void accept(Recipe<?> p_361793_) {
         this.extractor.apply(p_361793_).ifPresent(this.ingredients::add);
      }

      public RecipePropertySet asPropertySet(FeatureFlagSet p_363031_) {
         return RecipePropertySet.create(RecipeManager.filterDisabled(p_363031_, this.ingredients));
      }
   }

   @FunctionalInterface
   public interface IngredientExtractor {
      Optional<Ingredient> apply(Recipe<?> var1);
   }

   public record ServerDisplayInfo(RecipeDisplayEntry display, RecipeHolder<?> parent) {
   }
}
