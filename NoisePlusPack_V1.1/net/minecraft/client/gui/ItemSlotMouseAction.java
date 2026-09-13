package net.minecraft.client.gui;

import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ItemSlotMouseAction {
   boolean matches(Slot var1);

   boolean onMouseScrolled(double var1, double var3, int var5, ItemStack var6);

   void onStopHovering(Slot var1);

   void onSlotClicked(Slot var1, ClickType var2);
}
