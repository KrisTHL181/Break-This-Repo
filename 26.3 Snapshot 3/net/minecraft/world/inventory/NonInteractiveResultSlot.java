package net.minecraft.world.inventory;

import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NonInteractiveResultSlot extends Slot {
   public NonInteractiveResultSlot(final Container container, final int id, final int x, final int y) {
      super(container, id, x, y);
   }

   @Override
   public void onQuickCraft(final ItemStack picked, final ItemStack original) {
   }

   @Override
   public boolean mayPickup(final Player player) {
      return false;
   }

   @Override
   public Optional<ItemStack> tryRemove(final int amount, final int maxAmount, final Player player) {
      return Optional.empty();
   }

   @Override
   public ItemStack safeTake(final int amount, final int maxAmount, final Player player) {
      return ItemStack.EMPTY;
   }

   @Override
   public ItemStack safeInsert(final ItemStack stack) {
      return stack;
   }

   @Override
   public ItemStack safeInsert(final ItemStack inputStack, final int inputAmount) {
      return this.safeInsert(inputStack);
   }

   @Override
   public boolean allowModification(final Player player) {
      return false;
   }

   @Override
   public boolean mayPlace(final ItemStack itemStack) {
      return false;
   }

   @Override
   public ItemStack remove(final int amount) {
      return ItemStack.EMPTY;
   }

   @Override
   public void onTake(final Player player, final ItemStack carried) {
   }

   @Override
   public boolean isHighlightable() {
      return false;
   }

   @Override
   public boolean isFake() {
      return true;
   }
}
