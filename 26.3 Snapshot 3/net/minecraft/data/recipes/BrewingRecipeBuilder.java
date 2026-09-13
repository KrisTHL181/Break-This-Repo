package net.minecraft.data.recipes;

import java.util.Optional;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.PotionsPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

public class BrewingRecipeBuilder implements RecipeBuilder {
   private final PotionIngredient input;
   private final PotionIngredient reagent;
   private final ItemStackTemplate output;

   private BrewingRecipeBuilder(final PotionIngredient input, final PotionIngredient reagent, final ItemStackTemplate output) {
      this.input = input;
      this.reagent = reagent;
      this.output = output;
   }

   private static PotionIngredient potionIngredient(final Item potionContainer, final Holder<Potion> potion) {
      return PotionIngredient.of(potionContainer, PotionsPredicate.ofPotion(potion));
   }

   private static ItemStackTemplate potionOutput(final Item potionContainer, final Holder<Potion> potion) {
      return new ItemStackTemplate(potionContainer, DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, new PotionContents(potion)).build());
   }

   public static BrewingRecipeBuilder brewingMix(
      final Item container, final Holder<Potion> inputPotion, final Item reagentItem, final Holder<Potion> outputPotion
   ) {
      PotionIngredient input = potionIngredient(container, inputPotion);
      PotionIngredient reagent = PotionIngredient.of(reagentItem);
      ItemStackTemplate output = potionOutput(container, outputPotion);
      return new BrewingRecipeBuilder(input, reagent, output);
   }

   public static BrewingRecipeBuilder brewingContainerTransform(
      final Item inputContainer, final Holder<Potion> inputPotion, final Item reagentItem, final Item outputContainer
   ) {
      PotionIngredient input = potionIngredient(inputContainer, inputPotion);
      PotionIngredient reagent = PotionIngredient.of(reagentItem);
      ItemStackTemplate output = potionOutput(outputContainer, inputPotion);
      return new BrewingRecipeBuilder(input, reagent, output);
   }

   @Override
   public RecipeBuilder unlockedBy(final String name, final Criterion<?> criterion) {
      throw new IllegalStateException("Brewing recipes cannot be unlocked");
   }

   public BrewingRecipeBuilder group(final @Nullable String group) {
      throw new IllegalStateException("Brewing recipes do not have groups");
   }

   public static Optional<Holder<Potion>> getExactPotion(final PotionsPredicate predicate) {
      if (predicate.potions().isEmpty()) {
         return Optional.empty();
      }

      if (predicate.effects().isPresent()) {
         return Optional.empty();
      }

      HolderSet<Potion> potionSet = predicate.potions().get();
      return potionSet.size() != 1 ? Optional.empty() : Optional.of(potionSet.get(0));
   }

   private Optional<Holder<Potion>> getIngredientPotion(final PotionIngredient ingredient) {
      return ingredient.potions().flatMap(BrewingRecipeBuilder::getExactPotion);
   }

   @Override
   public ResourceKey<Recipe<?>> defaultId() {
      ResourceKey<Item> inputItem = this.input.ingredient().getSingleItem().flatMap(Holder::unwrapKey).orElseThrow();
      ResourceKey<Potion> potionId = this.getIngredientPotion(this.input).flatMap(Holder::unwrapKey).orElseThrow();
      ResourceKey<Item> reagentItem = this.reagent.ingredient().getSingleItem().flatMap(Holder::unwrapKey).orElseThrow();
      Identifier combined = inputItem.identifier()
         .withPath(inputPath -> "brewing/" + inputPath + "_" + potionId.identifier().getPath() + "_" + reagentItem.identifier().getPath());
      return ResourceKey.create(Registries.RECIPE, combined);
   }

   @Override
   public void save(final RecipeOutput recipeOutput, final ResourceKey<Recipe<?>> id) {
      BrewingRecipe recipe = new BrewingRecipe(this.input, this.reagent, this.output);
      recipeOutput.accept(id, recipe, null);
   }
}
