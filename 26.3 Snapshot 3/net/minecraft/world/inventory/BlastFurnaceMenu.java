package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.RecipePropertySet;

public class BlastFurnaceMenu extends AbstractFurnaceMenu {
   public BlastFurnaceMenu(final int containerId, final Inventory inventory) {
      super(MenuType.BLAST_FURNACE, RecipePropertySet.BLAST_FURNACE_INPUT, RecipeBookType.BLAST_FURNACE, containerId, inventory);
   }

   public BlastFurnaceMenu(final int containerId, final Inventory inventory, final Container container, final ContainerData data) {
      super(MenuType.BLAST_FURNACE, RecipePropertySet.BLAST_FURNACE_INPUT, RecipeBookType.BLAST_FURNACE, containerId, inventory, container, data);
   }
}
