package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.RecipePropertySet;

public class SmokerMenu extends AbstractFurnaceMenu {
   public SmokerMenu(final int containerId, final Inventory inventory) {
      super(MenuType.SMOKER, RecipePropertySet.SMOKER_INPUT, RecipeBookType.SMOKER, containerId, inventory);
   }

   public SmokerMenu(final int containerId, final Inventory inventory, final Container container, final ContainerData data) {
      super(MenuType.SMOKER, RecipePropertySet.SMOKER_INPUT, RecipeBookType.SMOKER, containerId, inventory, container, data);
   }
}
