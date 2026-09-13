package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ChestMenu extends AbstractContainerMenu {
   private final Container container;
   private final int containerRows;

   private ChestMenu(final MenuType<?> menuType, final int containerId, final Inventory inventory, final int rows) {
      this(menuType, containerId, inventory, new SimpleContainer(9 * rows), rows);
   }

   public static ChestMenu oneRow(final int containerId, final Inventory inventory) {
      return new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, 1);
   }

   public static ChestMenu twoRows(final int containerId, final Inventory inventory) {
      return new ChestMenu(MenuType.GENERIC_9x2, containerId, inventory, 2);
   }

   public static ChestMenu threeRows(final int containerId, final Inventory inventory) {
      return new ChestMenu(MenuType.GENERIC_9x3, containerId, inventory, 3);
   }

   public static ChestMenu fourRows(final int containerId, final Inventory inventory) {
      return new ChestMenu(MenuType.GENERIC_9x4, containerId, inventory, 4);
   }

   public static ChestMenu fiveRows(final int containerId, final Inventory inventory) {
      return new ChestMenu(MenuType.GENERIC_9x5, containerId, inventory, 5);
   }

   public static ChestMenu sixRows(final int containerId, final Inventory inventory) {
      return new ChestMenu(MenuType.GENERIC_9x6, containerId, inventory, 6);
   }

   public static ChestMenu threeRows(final int containerId, final Inventory inventory, final Container container) {
      return new ChestMenu(MenuType.GENERIC_9x3, containerId, inventory, container, 3);
   }

   public static ChestMenu sixRows(final int containerId, final Inventory inventory, final Container container) {
      return new ChestMenu(MenuType.GENERIC_9x6, containerId, inventory, container, 6);
   }

   public ChestMenu(final MenuType<?> menuType, final int containerId, final Inventory inventory, final Container container, final int rows) {
      super(menuType, containerId);
      checkContainerSize(container, rows * 9);
      this.container = container;
      this.containerRows = rows;
      container.startOpen(inventory.player);
      int chestGridTop = 18;
      this.addChestGrid(container, 8, 18);
      int inventoryTop = 18 + this.containerRows * 18 + 13;
      this.addStandardInventorySlots(inventory, 8, inventoryTop);
   }

   private void addChestGrid(final Container container, final int left, final int top) {
      for (int y = 0; y < this.containerRows; y++) {
         for (int x = 0; x < 9; x++) {
            this.addSlot(new Slot(container, x + y * 9, left + x * 18, top + y * 18));
         }
      }
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
         if (slotIndex < this.containerRows * 9) {
            if (!this.moveItemStackTo(stack, this.containerRows * 9, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(stack, 0, this.containerRows * 9, false)) {
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

   public Container getContainer() {
      return this.container;
   }

   public int getRowCount() {
      return this.containerRows;
   }
}
