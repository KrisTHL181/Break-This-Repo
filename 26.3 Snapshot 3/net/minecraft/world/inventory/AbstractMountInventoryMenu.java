package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractMountInventoryMenu extends AbstractContainerMenu {
   protected final Container mountContainer;
   protected final LivingEntity mount;
   protected final int SLOT_SADDLE = 0;
   protected final int SLOT_BODY_ARMOR = 1;
   protected final int SLOT_INVENTORY_START = 2;
   protected static final int INVENTORY_ROWS = 3;

   protected AbstractMountInventoryMenu(final int containerId, final Inventory playerInventory, final Container mountInventory, final LivingEntity mount) {
      super(null, containerId);
      this.mountContainer = mountInventory;
      this.mount = mount;
      mountInventory.startOpen(playerInventory.player);
   }

   protected abstract boolean hasInventoryChanged(final Container container);

   @Override
   public boolean stillValid(final Player player) {
      return !this.hasInventoryChanged(this.mountContainer)
         && this.mountContainer.stillValid(player)
         && this.mount.isAlive()
         && player.isWithinEntityInteractionRange(this.mount, 4.0);
   }

   @Override
   public void removed(final Player player) {
      super.removed(player);
      this.mountContainer.stopOpen(player);
   }

   @Override
   public ItemStack quickMoveStack(final Player player, final int slotIndex) {
      ItemStack clicked = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         clicked = stack.copy();
         int playerContainerStart = 2 + this.mountContainer.getContainerSize();
         if (slotIndex < playerContainerStart) {
            if (!this.moveItemStackTo(stack, playerContainerStart, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (this.getSlot(1).mayPlace(stack) && !this.getSlot(1).hasItem()) {
            if (!this.moveItemStackTo(stack, 1, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.getSlot(0).mayPlace(stack) && !this.getSlot(0).hasItem()) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.mountContainer.getContainerSize() == 0 || !this.moveItemStackTo(stack, 2, playerContainerStart, false)) {
            int playerContainerEnd = playerContainerStart + 27;
            int playerHotBarStart = playerContainerEnd;
            int playerHotBarEnd = playerHotBarStart + 9;
            if (slotIndex >= playerHotBarStart && slotIndex < playerHotBarEnd) {
               if (!this.moveItemStackTo(stack, playerContainerStart, playerContainerEnd, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (slotIndex >= playerContainerStart && slotIndex < playerContainerEnd) {
               if (!this.moveItemStackTo(stack, playerHotBarStart, playerHotBarEnd, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(stack, playerHotBarStart, playerContainerEnd, false)) {
               return ItemStack.EMPTY;
            }

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

   public static int getInventorySize(final int inventoryColumns) {
      return inventoryColumns * 3;
   }
}
