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
   default ItemStack getItem(final int slot) {
      return this.getItems().get(slot);
   }

   @Override
   default ItemStack removeItem(final int slot, final int count) {
      ItemStack result = ContainerHelper.removeItem(this.getItems(), slot, count);
      if (!result.isEmpty()) {
         this.setChanged();
      }

      return result;
   }

   @Override
   default ItemStack removeItemNoUpdate(final int slot) {
      return ContainerHelper.removeItem(this.getItems(), slot, this.getMaxStackSize());
   }

   @Override
   default boolean canPlaceItem(final int slot, final ItemStack itemStack) {
      return this.acceptsItemType(itemStack) && (this.getItem(slot).isEmpty() || this.getItem(slot).getCount() < this.getMaxStackSize(itemStack));
   }

   default boolean acceptsItemType(final ItemStack itemStack) {
      return true;
   }

   @Override
   default void setItem(final int slot, final ItemStack itemStack) {
      this.setItemNoUpdate(slot, itemStack);
      this.setChanged();
   }

   default void setItemNoUpdate(final int slot, final ItemStack itemStack) {
      this.getItems().set(slot, itemStack);
      itemStack.limitSize(this.getMaxStackSize(itemStack));
   }
}
