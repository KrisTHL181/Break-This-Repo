package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;

public interface LootItemFunction extends LootContextUser, BiFunction<ItemStack, LootContext, ItemStack> {
   MapCodec<? extends LootItemFunction> codec();

   static Consumer<ItemStack> decorate(
      final BiFunction<ItemStack, LootContext, ItemStack> function, final Consumer<ItemStack> output, final LootContext context
   ) {
      return drop -> output.accept(function.apply(drop, context));
   }

   interface Builder {
      LootItemFunction build();
   }
}
