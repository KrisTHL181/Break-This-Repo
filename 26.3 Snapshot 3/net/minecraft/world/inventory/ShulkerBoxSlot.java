package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class ShulkerBoxSlot extends Slot {
   public ShulkerBoxSlot(final Container container, final int slot, final int x, final int y) {
      super(container, slot, x, y);
   }

   @Override
   public boolean mayPlace(final ItemStack itemStack) {
      return itemStack.getItem().canFitInsideContainerItems();
   }
}
