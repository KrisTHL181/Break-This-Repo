package net.minecraft.world.level.block.entity;

import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

public interface ListBackedContainer extends Container {
   NonNullList<ItemStack> getItems();

   default int count() {
      return (int)this.getItems().stream().filter(Predicate.not(ItemStack::isEmpty)).count();
   }

   @Override
   default int getContainerSize() {
      return this.getItems().size();
   }

   @Override
   default void clearContent() {
      this.getItems().clear();
   }

   @Override
   default boolean isEmpty() {
      return this.getItems().stream().allMatch(ItemStack::isEmpty);
   }

   @Override
   default ItemStack getItem(int p_429344_) {
      return this.getItems().get(p_429344_);
   }

   @Override
   default ItemStack removeItem(int p_427176_, int p_430870_) {
      ItemStack itemstack = ContainerHelper.removeItem(this.getItems(), p_427176_, p_430870_);
      if (!itemstack.isEmpty()) {
         this.setChanged();
      }

      return itemstack;
   }

   @Override
   default ItemStack removeItemNoUpdate(int p_423060_) {
      return ContainerHelper.removeItem(this.getItems(), p_423060_, this.getMaxStackSize());
   }

   @Override
   default boolean canPlaceItem(int p_427311_, ItemStack p_425253_) {
      return this.acceptsItemType(p_425253_) && (this.getItem(p_427311_).isEmpty() || this.getItem(p_427311_).getCount() < this.getMaxStackSize(p_425253_));
   }

   default boolean acceptsItemType(ItemStack p_424253_) {
      return true;
   }

   @Override
   default void setItem(int p_425609_, ItemStack p_424516_) {
      this.setItemNoUpdate(p_425609_, p_424516_);
      this.setChanged();
   }

   default void setItemNoUpdate(int p_427306_, ItemStack p_428005_) {
      this.getItems().set(p_427306_, p_428005_);
      p_428005_.limitSize(this.getMaxStackSize(p_428005_));
   }
}
