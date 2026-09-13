package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class SmeltItemFunction extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<SmeltItemFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_298512_ -> commonFields(p_298512_).apply(p_298512_, SmeltItemFunction::new)
   );

   private SmeltItemFunction(List<LootItemCondition> p_298857_) {
      super(p_298857_);
   }

   @Override
   public LootItemFunctionType<SmeltItemFunction> getType() {
      return LootItemFunctions.FURNACE_SMELT;
   }

   @Override
   public ItemStack run(ItemStack p_81268_, LootContext p_81269_) {
      if (p_81268_.isEmpty()) {
         return p_81268_;
      }

      SingleRecipeInput singlerecipeinput = new SingleRecipeInput(p_81268_);
      Optional<RecipeHolder<SmeltingRecipe>> optional = p_81269_.getLevel()
         .recipeAccess()
         .getRecipeFor(RecipeType.SMELTING, singlerecipeinput, p_81269_.getLevel());
      if (optional.isPresent()) {
         ItemStack itemstack = optional.get().value().assemble(singlerecipeinput, p_81269_.getLevel().registryAccess());
         if (!itemstack.isEmpty()) {
            return itemstack.copyWithCount(p_81268_.getCount());
         }
      }

      LOGGER.warn("Couldn't smelt {} because there is no smelting recipe", p_81268_);
      return p_81268_;
   }

   public static LootItemConditionalFunction.Builder<?> smelted() {
      return simpleBuilder(SmeltItemFunction::new);
   }
}
