package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

public class SmokerBlockEntity extends AbstractFurnaceBlockEntity {
   private static final Component DEFAULT_NAME = Component.translatable("container.smoker");

   public SmokerBlockEntity(BlockPos p_155749_, BlockState p_155750_) {
      super(BlockEntityType.SMOKER, p_155749_, p_155750_, RecipeType.SMOKING);
   }

   @Override
   protected Component getDefaultName() {
      return DEFAULT_NAME;
   }

   @Override
   protected int getBurnDuration(FuelValues p_365622_, ItemStack p_59786_) {
      return super.getBurnDuration(p_365622_, p_59786_) / 2;
   }

   @Override
   protected AbstractContainerMenu createMenu(int p_59783_, Inventory p_59784_) {
      return new SmokerMenu(p_59783_, p_59784_, this, this.dataAccess);
   }
}
