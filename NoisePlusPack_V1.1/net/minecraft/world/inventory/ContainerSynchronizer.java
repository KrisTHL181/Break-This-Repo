package net.minecraft.world.inventory;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public interface ContainerSynchronizer {
   void sendInitialData(AbstractContainerMenu var1, List<ItemStack> var2, ItemStack var3, int[] var4);

   void sendSlotChange(AbstractContainerMenu var1, int var2, ItemStack var3);

   void sendCarriedChange(AbstractContainerMenu var1, ItemStack var2);

   void sendDataChange(AbstractContainerMenu var1, int var2, int var3);

   RemoteSlot createSlot();
}
