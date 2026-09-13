package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeCraftedTrigger extends SimpleCriterionTrigger<RecipeCraftedTrigger.TriggerInstance> {
   @Override
   public Codec<RecipeCraftedTrigger.TriggerInstance> codec() {
      return RecipeCraftedTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_451841_, ResourceKey<Recipe<?>> p_457971_, List<ItemStack> p_454913_) {
      this.trigger(p_451841_, p_458169_ -> p_458169_.matches(p_457971_, p_454913_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, ResourceKey<Recipe<?>> recipeId, List<ItemPredicate> ingredients)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<RecipeCraftedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_455128_ -> p_455128_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(RecipeCraftedTrigger.TriggerInstance::player),
               Recipe.KEY_CODEC.fieldOf("recipe_id").forGetter(RecipeCraftedTrigger.TriggerInstance::recipeId),
               ItemPredicate.CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(RecipeCraftedTrigger.TriggerInstance::ingredients)
            )
            .apply(p_455128_, RecipeCraftedTrigger.TriggerInstance::new)
      );

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> craftedItem(ResourceKey<Recipe<?>> p_458778_, List<ItemPredicate.Builder> p_450576_) {
         return CriteriaTriggers.RECIPE_CRAFTED
            .createCriterion(
               new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), p_458778_, p_450576_.stream().map(ItemPredicate.Builder::build).toList())
            );
      }

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> craftedItem(ResourceKey<Recipe<?>> p_456339_) {
         return CriteriaTriggers.RECIPE_CRAFTED.createCriterion(new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), p_456339_, List.of()));
      }

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> crafterCraftedItem(ResourceKey<Recipe<?>> p_451603_) {
         return CriteriaTriggers.CRAFTER_RECIPE_CRAFTED.createCriterion(new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), p_451603_, List.of()));
      }

      boolean matches(ResourceKey<Recipe<?>> p_450237_, List<ItemStack> p_455165_) {
         if (p_450237_ != this.recipeId) {
            return false;
         }

         List<ItemStack> list = new ArrayList<>(p_455165_);

         for (ItemPredicate itempredicate : this.ingredients) {
            boolean flag = false;
            Iterator<ItemStack> iterator = list.iterator();

            while (iterator.hasNext()) {
               if (itempredicate.test(iterator.next())) {
                  iterator.remove();
                  flag = true;
                  break;
               }
            }

            if (!flag) {
               return false;
            }
         }

         return true;
      }
   }
}
