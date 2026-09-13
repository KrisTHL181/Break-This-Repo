package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HopperMenu extends AbstractContainerMenu {
   public static final int CONTAINER_SIZE = 5;
   private final Container hopper;

   public HopperMenu(final int containerId, final Inventory inventory) {
      this(containerId, inventory, new SimpleContainer(5));
   }

   public HopperMenu(final int containerId, final Inventory inventory, final Container hopper) {
      super(MenuType.HOPPER, containerId);
      this.hopper = hopper;
      checkContainerSize(hopper, 5);
      hopper.startOpen(inventory.player);

      for (int x = 0; x < 5; x++) {
         this.addSlot(new Slot(hopper, x, 44 + x * 18, 20));
      }

      this.addStandardInventorySlots(inventory, 8, 51);
   }

   @Override
   public boolean stillValid(final Player player) {
      return this.hopper.stillValid(player);
   }

   @Override
   public ItemStack quickMoveStack(final Player player, final int slotIndex) {
      ItemStack clicked = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         clicked = stack.copy();
         if (slotIndex < this.hopper.getContainerSize()) {
            if (!this.moveItemStackTo(stack, this.hopper.getContainerSize(), this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(stack, 0, this.hopper.getContainerSize(), false)) {
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
      this.hopper.stopOpen(player);
   }
}
