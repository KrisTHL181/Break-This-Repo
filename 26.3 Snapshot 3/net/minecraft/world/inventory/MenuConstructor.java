package net.minecraft.world.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface MenuConstructor {
   @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, final Player player);
}
