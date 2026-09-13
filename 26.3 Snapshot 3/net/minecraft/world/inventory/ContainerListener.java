package net.minecraft.world.inventory;

import net.minecraft.world.item.ItemStack;

public interface ContainerListener {
   void slotChanged(AbstractContainerMenu container, int slotIndex, ItemStack itemStack);

   void dataChanged(AbstractContainerMenu container, int id, int value);
}
