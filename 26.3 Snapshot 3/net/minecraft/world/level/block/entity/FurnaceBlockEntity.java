package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

public class FurnaceBlockEntity extends AbstractFurnaceBlockEntity {
   private static final Component DEFAULT_NAME = Component.translatable("container.furnace");

   public FurnaceBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.FURNACE, worldPosition, blockState, RecipeType.SMELTING);
   }

   @Override
   protected Component getDefaultName() {
      return DEFAULT_NAME;
   }

   @Override
   protected AbstractContainerMenu createMenu(final int containerId, final Inventory inventory) {
      return new FurnaceMenu(containerId, inventory, this, this.dataAccess);
   }
}
