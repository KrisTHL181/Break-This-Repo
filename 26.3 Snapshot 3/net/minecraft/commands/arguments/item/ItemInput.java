package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ItemInput(Holder<Item> item, DataComponentPatch components) {
   private static final Dynamic2CommandExceptionType ERROR_STACK_TOO_BIG = new Dynamic2CommandExceptionType(
      (item, count) -> Component.translatableEscape("arguments.item.overstacked", item, count)
   );
   private static final DynamicCommandExceptionType ERROR_MALFORMED_ITEM = new DynamicCommandExceptionType(
      error -> Component.translatableEscape("arguments.item.malformed", error)
   );

   public ItemStack createItemStack(final int count) throws CommandSyntaxException {
      ItemStack result = new ItemStack(this.item, count, this.components);
      if (count > result.getMaxStackSize()) {
         throw ERROR_STACK_TOO_BIG.create(this.item.getRegisteredName(), result.getMaxStackSize());
      }

      DataResult<ItemStack> validationResult = ItemStack.validateStrict(result);
      return (ItemStack)validationResult.getOrThrow(ERROR_MALFORMED_ITEM::create);
   }
}
