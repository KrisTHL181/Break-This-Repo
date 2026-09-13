package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.RecipePropertySet;

public class FurnaceMenu extends AbstractFurnaceMenu {
   public FurnaceMenu(final int containerId, final Inventory inventory) {
      super(MenuType.FURNACE, RecipePropertySet.FURNACE_INPUT, RecipeBookType.FURNACE, containerId, inventory);
   }

   public FurnaceMenu(final int containerId, final Inventory inventory, final Container container, final ContainerData data) {
      super(MenuType.FURNACE, RecipePropertySet.FURNACE_INPUT, RecipeBookType.FURNACE, containerId, inventory, container, data);
   }
}
