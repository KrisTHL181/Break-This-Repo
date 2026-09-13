package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeCraftedTrigger extends SimpleCriterionTrigger<RecipeCraftedTrigger.TriggerInstance> {
   @Override
   public Codec<RecipeCraftedTrigger.TriggerInstance> codec() {
      return RecipeCraftedTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final ResourceKey<Recipe<?>> id, final List<ItemStack> usedIngredients) {
      this.trigger(player, t -> t.matches(id, usedIngredients));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, ResourceKey<Recipe<?>> recipeId, List<ItemPredicate> ingredients)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<RecipeCraftedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(RecipeCraftedTrigger.TriggerInstance::player),
               Recipe.KEY_CODEC.fieldOf("recipe_id").forGetter(RecipeCraftedTrigger.TriggerInstance::recipeId),
               ItemPredicate.CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(RecipeCraftedTrigger.TriggerInstance::ingredients)
            )
            .apply(i, RecipeCraftedTrigger.TriggerInstance::new)
      );

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> craftedItem(
         final ResourceKey<Recipe<?>> recipeId, final List<ItemPredicate.Builder> predicates
      ) {
         return CriteriaTriggers.RECIPE_CRAFTED
            .createCriterion(
               new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), recipeId, predicates.stream().map(ItemPredicate.Builder::build).toList())
            );
      }

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> craftedItem(final ResourceKey<Recipe<?>> recipeId) {
         return CriteriaTriggers.RECIPE_CRAFTED.createCriterion(new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), recipeId, List.of()));
      }

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> crafterCraftedItem(final ResourceKey<Recipe<?>> recipeId) {
         return CriteriaTriggers.CRAFTER_RECIPE_CRAFTED.createCriterion(new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), recipeId, List.of()));
      }

      private boolean matches(final ResourceKey<Recipe<?>> id, final List<ItemStack> usedIngredients) {
         if (id != this.recipeId) {
            return false;
         }

         List<ItemStack> remaining = new ArrayList<>(usedIngredients);

         for (ItemPredicate predicate : this.ingredients) {
            boolean found = false;
            Iterator<ItemStack> iterator = remaining.iterator();

            while (iterator.hasNext()) {
               if (predicate.test(iterator.next())) {
                  iterator.remove();
                  found = true;
                  break;
               }
            }

            if (!found) {
               return false;
            }
         }

         return true;
      }
   }
}
