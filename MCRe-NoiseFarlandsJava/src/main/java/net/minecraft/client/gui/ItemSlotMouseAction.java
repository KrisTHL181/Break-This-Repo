package net.minecraft.client.gui;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ItemSlotMouseAction {
    boolean matches(final Slot slot);

    boolean onMouseScrolled(final double scrollX, final double scrollY, final int slotIndex, final ItemStack itemStack);

    void onStopHovering(final Slot hoveredSlot);

    void onSlotClicked(final Slot slot, ContainerInput containerInput);
}