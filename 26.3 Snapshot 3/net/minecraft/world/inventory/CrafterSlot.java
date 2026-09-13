package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class CrafterSlot extends Slot {
   private final CrafterMenu menu;

   public CrafterSlot(final Container container, final int slot, final int x, final int y, final CrafterMenu menu) {
      super(container, slot, x, y);
      this.menu = menu;
   }

   @Override
   public boolean mayPlace(final ItemStack itemStack) {
      return !this.menu.isSlotDisabled(this.index) && super.mayPlace(itemStack);
   }

   @Override
   public void setChanged() {
      super.setChanged();
      this.menu.slotsChanged(this.container);
   }
}
