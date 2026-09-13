package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ShulkerBoxMenu extends AbstractContainerMenu {
   private static final int CONTAINER_SIZE = 27;
   private final Container container;

   public ShulkerBoxMenu(final int containerId, final Inventory inventory) {
      this(containerId, inventory, new SimpleContainer(27));
   }

   public ShulkerBoxMenu(final int containerId, final Inventory inventory, final Container container) {
      super(MenuType.SHULKER_BOX, containerId);
      checkContainerSize(container, 27);
      this.container = container;
      container.startOpen(inventory.player);
      int rows = 3;
      int columns = 9;

      for (int y = 0; y < 3; y++) {
         for (int x = 0; x < 9; x++) {
            this.addSlot(new ShulkerBoxSlot(container, x + y * 9, 8 + x * 18, 18 + y * 18));
         }
      }

      this.addStandardInventorySlots(inventory, 8, 84);
   }

   @Override
   public boolean stillValid(final Player player) {
      return this.container.stillValid(player);
   }

   @Override
   public ItemStack quickMoveStack(final Player player, final int slotIndex) {
      ItemStack clicked = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         clicked = stack.copy();
         if (slotIndex < this.container.getContainerSize()) {
            if (!this.moveItemStackTo(stack, this.container.getContainerSize(), this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(stack, 0, this.container.getContainerSize(), false)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }

      return clicked;
   }

   @Override
   public void removed(final Player player) {
      super.removed(player);
      this.container.stopOpen(player);
   }
}
