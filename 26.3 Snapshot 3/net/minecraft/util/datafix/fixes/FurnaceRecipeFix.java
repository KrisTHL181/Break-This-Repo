package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;

public class FurnaceRecipeFix extends DataFix {
   public FurnaceRecipeFix(final Schema schema, final boolean changesType) {
      super(schema, changesType);
   }

   protected TypeRewriteRule makeRule() {
      return this.cap(this.getOutputSchema().getTypeRaw(References.RECIPE));
   }

   private <R> TypeRewriteRule cap(final Type<R> recipeType) {
      Type<Pair<Either<Pair<List<Pair<R, Integer>>, Dynamic<?>>, Unit>, Dynamic<?>>> replacedType = DSL.and(
         DSL.optional(DSL.field("RecipesUsed", DSL.and(DSL.compoundList(recipeType, DSL.intType()), DSL.remainderType()))), DSL.remainderType()
      );
      OpticFinder<?> oldFurnaceFinder = DSL.namedChoice("minecraft:furnace", this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:furnace"));
      OpticFinder<?> oldBlastFurnaceFinder = DSL.namedChoice(
         "minecraft:blast_furnace", this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:blast_furnace")
      );
      OpticFinder<?> oldSmokerFinder = DSL.namedChoice("minecraft:smoker", this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:smoker"));
      Type<?> newFurnaceType = this.getOutputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:furnace");
      Type<?> newBlastFurnaceFinder = this.getOutputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:blast_furnace");
      Type<?> newSmokerFinder = this.getOutputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:smoker");
      Type<?> oldEntityType = this.getInputSchema().getType(References.BLOCK_ENTITY);
      Type<?> newEntityType = this.getOutputSchema().getType(References.BLOCK_ENTITY);
      return this.fixTypeEverywhereTyped(
         "FurnaceRecipesFix",
         oldEntityType,
         newEntityType,
         input -> input.updateTyped(oldFurnaceFinder, newFurnaceType, furnace -> this.updateFurnaceContents(recipeType, replacedType, furnace))
            .updateTyped(oldBlastFurnaceFinder, newBlastFurnaceFinder, blastFurnace -> this.updateFurnaceContents(recipeType, replacedType, blastFurnace))
            .updateTyped(oldSmokerFinder, newSmokerFinder, smoker -> this.updateFurnaceContents(recipeType, replacedType, smoker))
      );
   }

   private <R> Typed<?> updateFurnaceContents(
      final Type<R> recipeType, final Type<Pair<Either<Pair<List<Pair<R, Integer>>, Dynamic<?>>, Unit>, Dynamic<?>>> replacedType, final Typed<?> input
   ) {
      Dynamic<?> tag = (Dynamic<?>)input.getOrCreate(DSL.remainderFinder());
      int recipesUsedSize = tag.get("RecipesUsedSize").asInt(0);
      tag = tag.remove("RecipesUsedSize");
      List<Pair<R, Integer>> results = Lists.newArrayList();

      for (int i = 0; i < recipesUsedSize; i++) {
         String locationKey = "RecipeLocation" + i;
         String amountKey = "RecipeAmount" + i;
         Optional<? extends Dynamic<?>> maybeLocation = tag.get(locationKey).result();
         int amount = tag.get(amountKey).asInt(0);
         if (amount > 0) {
            maybeLocation.ifPresent(location -> {
               Optional<? extends Pair<R, ? extends Dynamic<?>>> parseResult = recipeType.read(location).result();
               parseResult.ifPresent(r -> results.add(Pair.of(r.getFirst(), amount)));
            });
         }

         tag = tag.remove(locationKey).remove(amountKey);
      }

      return input.set(DSL.remainderFinder(), replacedType, Pair.of(Either.left(Pair.of(results, tag.emptyMap())), tag));
   }
}
