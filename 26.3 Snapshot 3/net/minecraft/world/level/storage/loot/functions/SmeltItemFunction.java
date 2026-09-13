package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
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
   private final boolean useInputCount;
   public static final MapCodec<SmeltItemFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i).and(Codec.BOOL.optionalFieldOf("use_input_count", true).forGetter(o -> o.useInputCount)).apply(i, SmeltItemFunction::new)
   );

   private SmeltItemFunction(final List<LootItemCondition> predicates, final boolean useInputCount) {
      super(predicates);
      this.useInputCount = useInputCount;
   }

   @Override
   public MapCodec<SmeltItemFunction> codec() {
      return MAP_CODEC;
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      if (itemStack.isEmpty()) {
         return itemStack;
      }

      SingleRecipeInput input = new SingleRecipeInput(itemStack);
      Optional<RecipeHolder<SmeltingRecipe>> recipe = context.getLevel().recipeAccess().getRecipeFor(RecipeType.SMELTING, input, context.getLevel());
      if (recipe.isPresent()) {
         ItemStack result = recipe.get().value().assemble(input);
         if (!result.isEmpty()) {
            int newCount = (this.useInputCount ? itemStack.count() : 1) * result.getCount();
            return result.copyWithCount(Math.min(newCount, result.getMaxStackSize()));
         }
      }

      LOGGER.warn("Couldn't smelt {} because there is no smelting recipe", itemStack);
      return itemStack;
   }

   public static LootItemConditionalFunction.Builder<?> smelted() {
      return smelted(true);
   }

   public static LootItemConditionalFunction.Builder<?> smelted(final boolean useInputCount) {
      return simpleBuilder(predicates -> new SmeltItemFunction(predicates, useInputCount));
   }
}
