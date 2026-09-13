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

   public BlastFurnaceBlockEntity(BlockPos p_155225_, BlockState p_155226_) {
      super(BlockEntityType.BLAST_FURNACE, p_155225_, p_155226_, RecipeType.BLASTING);
   }

   @Override
   protected Component getDefaultName() {
      return DEFAULT_NAME;
   }

   @Override
   protected int getBurnDuration(FuelValues p_366938_, ItemStack p_58852_) {
      return super.getBurnDuration(p_366938_, p_58852_) / 2;
   }

   @Override
   protected AbstractContainerMenu createMenu(int p_58849_, Inventory p_58850_) {
      return new BlastFurnaceMenu(p_58849_, p_58850_, this, this.dataAccess);
   }
}
