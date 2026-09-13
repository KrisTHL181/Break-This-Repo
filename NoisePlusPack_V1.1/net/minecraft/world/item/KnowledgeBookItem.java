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

   public KnowledgeBookItem(Item.Properties p_42822_) {
      super(p_42822_);
   }

   @Override
   public InteractionResult use(Level p_42824_, Player p_42825_, InteractionHand p_42826_) {
      ItemStack itemstack = p_42825_.getItemInHand(p_42826_);
      List<ResourceKey<Recipe<?>>> list = itemstack.getOrDefault(DataComponents.RECIPES, List.of());
      itemstack.consume(1, p_42825_);
      if (list.isEmpty()) {
         return InteractionResult.FAIL;
      }

      if (!p_42824_.isClientSide()) {
         RecipeManager recipemanager = p_42824_.getServer().getRecipeManager();
         List<RecipeHolder<?>> list1 = new ArrayList<>(list.size());

         for (ResourceKey<Recipe<?>> resourcekey : list) {
            Optional<RecipeHolder<?>> optional = recipemanager.byKey(resourcekey);
            if (!optional.isPresent()) {
               LOGGER.error("Invalid recipe: {}", resourcekey);
               return InteractionResult.FAIL;
            }

            list1.add(optional.get());
         }

         p_42825_.awardRecipes(list1);
         p_42825_.awardStat(Stats.ITEM_USED.get(this));
      }

      return InteractionResult.SUCCESS;
   }
}
