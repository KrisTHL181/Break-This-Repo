package net.minecraft.world;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface WorldlyContainer extends Container {
   int[] getSlotsForFace(Direction var1);

   boolean canPlaceItemThroughFace(int var1, ItemStack var2, @Nullable Direction var3);

   boolean canTakeItemThroughFace(int var1, ItemStack var2, Direction var3);
}
