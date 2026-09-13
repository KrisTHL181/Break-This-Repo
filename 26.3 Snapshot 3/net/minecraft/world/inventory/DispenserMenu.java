package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DispenserMenu extends AbstractContainerMenu {
   private static final int SLOT_COUNT = 9;
   private static final int INV_SLOT_START = 9;
   private static final int INV_SLOT_END = 36;
   private static final int USE_ROW_SLOT_START = 36;
   private static final int USE_ROW_SLOT_END = 45;
   private final Container dispenser;

   public DispenserMenu(final int containerId, final Inventory inventory) {
      this(containerId, inventory, new SimpleContainer(9));
   }

   public DispenserMenu(final int containerId, final Inventory inventory, final Container dispenser) {
      super(MenuType.GENERIC_3x3, containerId);
      checkContainerSize(dispenser, 9);
      this.dispenser = dispenser;
      dispenser.startOpen(inventory.player);
      this.add3x3GridSlots(dispenser, 62, 17);
      this.addStandardInventorySlots(inventory, 8, 84);
   }

   protected void add3x3GridSlots(final Container container, final int left, final int top) {
      for (int y = 0; y < 3; y++) {
         for (int x = 0; x < 3; x++) {
            int slot = x + y * 3;
            this.addSlot(new Slot(container, slot, left + x * 18, top + y * 18));
         }
      }
   }

   @Override
   public boolean stillValid(final Player player) {
      return this.dispenser.stillValid(player);
   }

   @Override
   public ItemStack quickMoveStack(final Player player, final int slotIndex) {
      ItemStack clicked = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         clicked = stack.copy();
         if (slotIndex < 9) {
            if (!this.moveItemStackTo(stack, 9, 45, true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(stack, 0, 9, false)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (stack.getCount() == clicked.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(player, stack);
      }

      return clicked;
   }

   @Override
   public void removed(final Player player) {
      super.removed(player);
      this.dispenser.stopOpen(player);
   }
}
