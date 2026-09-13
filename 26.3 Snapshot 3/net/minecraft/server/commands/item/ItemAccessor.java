package net.minecraft.server.commands.item;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.CommandResponseTracker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.slot.SlotCollection;
import net.minecraft.world.item.slot.SlotSource;
import org.jspecify.annotations.Nullable;

public interface ItemAccessor<Target> {
   void setItems(CommandSourceStack source, SlotSource slotSource, ItemAccessor.SetterFunction<Target> function) throws CommandSyntaxException;

   SlotCollection getSlots(CommandSourceStack source, SlotSource slotSource) throws CommandSyntaxException;

   int getReplaceSuccess(CommandSourceStack source, CommandResponseTracker<Target> tracker, @Nullable ItemStack knownItem) throws CommandSyntaxException;

   int getModifySuccess(CommandSourceStack source, CommandResponseTracker<Target> tracker) throws CommandSyntaxException;

   @FunctionalInterface
   interface SetterFunction<Target> {
      int apply(Target target, SlotCollection slots);
   }
}
