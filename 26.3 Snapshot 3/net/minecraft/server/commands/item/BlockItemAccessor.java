package net.minecraft.server.commands.item;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ArgProvider;
import net.minecraft.server.commands.CommandResponseTracker;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.slot.SlotCollection;
import net.minecraft.world.item.slot.SlotSource;
import org.jspecify.annotations.Nullable;

public record BlockItemAccessor(BlockPos pos) implements ItemAccessor<BlockPos> {
   public static final ArgProvider.Factory<ItemAccessor<?>> PROVIDER = arg -> ArgProvider.create(
      "block", () -> Commands.argument(arg, BlockPosArgument.blockPos()), c -> new BlockItemAccessor(BlockPosArgument.getLoadedBlockPos(c, arg))
   );
   private static final CommandResponseTracker.Messages<BlockPos> RESPONSE_SET = CommandResponseTracker.messages(
      ItemCommands.ERROR_TARGET_NO_CHANGES,
      (pos, slotCount) -> Component.translatable("commands.item.block.replace.success", slotCount, pos.getX(), pos.getY(), pos.getZ()),
      (var0, var1) -> Component.empty()
   );
   private static final CommandResponseTracker.MessagesWithArg<BlockPos, ItemStack> RESPONSE_SET_KNOWN_ITEM = CommandResponseTracker.messages(
      ItemCommands.ERROR_TARGET_NO_CHANGES_KNOWN_ITEM::create,
      (pos, slotCount, itemStack) -> Component.translatable(
         "commands.item.block.replace.success.known_item", slotCount, pos.getX(), pos.getY(), pos.getZ(), itemStack.getDisplayName()
      ),
      (var0, var1, var2) -> Component.empty()
   );
   private static final CommandResponseTracker.Messages<BlockPos> RESPONSE_MODIFY = CommandResponseTracker.messages(
      ItemCommands.ERROR_TARGET_NO_CHANGES,
      (pos, slotCount) -> Component.translatable("commands.item.block.modify.success", slotCount, pos.getX(), pos.getY(), pos.getZ()),
      (var0, var1) -> Component.empty()
   );

   @Override
   public SlotCollection getSlots(final CommandSourceStack source, final SlotSource slotSource) throws CommandSyntaxException {
      return this.getBlockSlots(source, slotSource, ItemCommands.ERROR_SOURCE_NOT_A_CONTAINER);
   }

   private SlotCollection getBlockSlots(final CommandSourceStack source, final SlotSource slotSource, final Dynamic3CommandExceptionType exceptionType) throws CommandSyntaxException {
      Container container = getContainer(source, this.pos, exceptionType);
      return ItemCommands.getSlotsFromProvider(source, container, slotSource);
   }

   @Override
   public void setItems(final CommandSourceStack source, final SlotSource slotSource, final ItemAccessor.SetterFunction<BlockPos> function) throws CommandSyntaxException {
      SlotCollection targetSlots = this.getBlockSlots(source, slotSource, ItemCommands.ERROR_TARGET_NOT_A_CONTAINER);
      function.apply(this.pos, targetSlots);
   }

   @Override
   public int getReplaceSuccess(final CommandSourceStack source, final CommandResponseTracker<BlockPos> tracker, final @Nullable ItemStack knownItem) throws CommandSyntaxException {
      return knownItem != null ? tracker.sendFeedback(source, true, RESPONSE_SET_KNOWN_ITEM, knownItem) : tracker.sendFeedback(source, true, RESPONSE_SET);
   }

   @Override
   public int getModifySuccess(final CommandSourceStack source, final CommandResponseTracker<BlockPos> tracker) throws CommandSyntaxException {
      return tracker.sendFeedback(source, true, RESPONSE_MODIFY);
   }

   public static Container getContainer(final CommandSourceStack source, final BlockPos pos, final Dynamic3CommandExceptionType exceptionType) throws CommandSyntaxException {
      if (source.getLevel().getBlockEntity(pos) instanceof Container container) {
         return container;
      } else {
         throw exceptionType.create(pos.getX(), pos.getY(), pos.getZ());
      }
   }
}
