package net.minecraft.world;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;

public final class SimpleMenuProvider implements MenuProvider {
   private final Component title;
   private final MenuConstructor menuConstructor;

   public SimpleMenuProvider(final MenuConstructor menuConstructor, final Component title) {
      this.menuConstructor = menuConstructor;
      this.title = title;
   }

   @Override
   public Component getDisplayName() {
      return this.title;
   }

   @Override
   public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
      return this.menuConstructor.createMenu(containerId, inventory, player);
   }
}
