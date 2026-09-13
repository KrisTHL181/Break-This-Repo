package net.minecraft.world.item;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class KnowledgeBookItem extends Item {
   private static final Logger LOGGER = LogUtils.getLogger();

   public KnowledgeBookItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      List<ResourceKey<Recipe<?>>> recipeIds = itemStack.getOrDefault(DataComponents.RECIPES, List.of());
      itemStack.consume(1, player);
      if (recipeIds.isEmpty()) {
         return InteractionResult.FAIL;
      }

      if (!level.isClientSide()) {
         RecipeManager recipeManager = level.getServer().getRecipeManager();
         List<RecipeHolder<?>> recipes = new ArrayList<>(recipeIds.size());

         for (ResourceKey<Recipe<?>> recipeId : recipeIds) {
            Optional<RecipeHolder<?>> recipe = recipeManager.byKey(recipeId);
            if (!recipe.isPresent()) {
               LOGGER.error("Invalid recipe: {}", recipeId);
               return InteractionResult.FAIL;
            }

            recipes.add(recipe.get());
         }

         player.awardRecipes(recipes);
         player.awardStat(Stats.ITEM_USED.get(this));
      }

      return InteractionResult.SUCCESS;
   }
}
