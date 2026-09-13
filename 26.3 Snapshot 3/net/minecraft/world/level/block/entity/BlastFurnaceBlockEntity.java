package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

public class BlastFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
   private static final Component DEFAULT_NAME = Component.translatable("container.blast_furnace");

   public BlastFurnaceBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.BLAST_FURNACE, worldPosition, blockState, RecipeType.BLASTING);
   }

   @Override
   protected Component getDefaultName() {
      return DEFAULT_NAME;
   }

   @Override
   protected int getBurnDuration(final FuelValues fuelValues, final ItemStack itemStack) {
      return super.getBurnDuration(fuelValues, itemStack) / 2;
   }

   @Override
   protected AbstractContainerMenu createMenu(final int containerId, final Inventory inventory) {
      return new BlastFurnaceMenu(containerId, inventory, this, this.dataAccess);
   }
}
