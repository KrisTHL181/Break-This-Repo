package net.minecraft.world.ticks;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface ContainerSingleItem extends Container {
   ItemStack getTheItem();

   default ItemStack splitTheItem(int p_312245_) {
      return this.getTheItem().split(p_312245_);
   }

   void setTheItem(ItemStack var1);

   default ItemStack removeTheItem() {
      return this.splitTheItem(this.getMaxStackSize());
   }

   @Override
   default int getContainerSize() {
      return 1;
   }

   @Override
   default boolean isEmpty() {
      return this.getTheItem().isEmpty();
   }

   @Override
   default void clearContent() {
      this.removeTheItem();
   }

   @Override
   default ItemStack removeItemNoUpdate(int p_273409_) {
      return this.removeItem(p_273409_, this.getMaxStackSize());
   }

   @Override
   default ItemStack getItem(int p_309780_) {
      return p_309780_ == 0 ? this.getTheItem() : ItemStack.EMPTY;
   }

   @Override
   default ItemStack removeItem(int p_313221_, int p_309913_) {
      return p_313221_ != 0 ? ItemStack.EMPTY : this.splitTheItem(p_309913_);
   }

   @Override
   default void setItem(int p_312121_, ItemStack p_312812_) {
      if (p_312121_ == 0) {
         this.setTheItem(p_312812_);
      }
   }

   interface BlockContainerSingleItem extends ContainerSingleItem {
      BlockEntity getContainerBlockEntity();

      @Override
      default boolean stillValid(Player p_335018_) {
         return Container.stillValidBlockEntity(this.getContainerBlockEntity(), p_335018_);
      }
   }
}
