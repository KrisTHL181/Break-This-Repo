package net.minecraft.world.inventory;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public interface ContainerSynchronizer {
   void sendInitialData(AbstractContainerMenu container, List<ItemStack> slotItems, ItemStack carried, int[] dataSlots);

   void sendSlotChange(AbstractContainerMenu container, int slotIndex, ItemStack itemStack);

   void sendCarriedChange(AbstractContainerMenu container, ItemStack itemStack);

   void sendDataChange(AbstractContainerMenu container, int id, int value);

   RemoteSlot createSlot();
}
