package net.minecraft.client.gui;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public interface ItemSlotMouseAction {
   boolean matches(final Slot slot);

   boolean onMouseScrolled(final double scrollX, final double scrollY, final int slotIndex, final ItemStack itemStack);

   void onStopHovering(final Slot hoveredSlot);

   void onSlotClicked(final Slot slot, ContainerInput containerInput);
}
