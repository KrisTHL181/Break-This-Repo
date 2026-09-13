package net.minecraft.world;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SimpleContainer implements Container, StackedContentsCompatible {
   private final int size;
   private final NonNullList<ItemStack> items;
   private @Nullable List<ContainerListener> listeners;

   public SimpleContainer(int p_19150_) {
      this.size = p_19150_;
      this.items = NonNullList.withSize(p_19150_, ItemStack.EMPTY);
   }

   public SimpleContainer(ItemStack... p_19152_) {
      this.size = p_19152_.length;
      this.items = NonNullList.of(ItemStack.EMPTY, p_19152_);
   }

   public void addListener(ContainerListener p_19165_) {
      if (this.listeners == null) {
         this.listeners = Lists.newArrayList();
      }

      this.listeners.add(p_19165_);
   }

   public void removeListener(ContainerListener p_19182_) {
      if (this.listeners != null) {
         this.listeners.remove(p_19182_);
      }
   }

   @Override
   public ItemStack getItem(int p_19157_) {
      return p_19157_ >= 0 && p_19157_ < this.items.size() ? this.items.get(p_19157_) : ItemStack.EMPTY;
   }

   public List<ItemStack> removeAllItems() {
      List<ItemStack> list = this.items.stream().filter(p_19197_ -> !p_19197_.isEmpty()).collect(Collectors.toList());
      this.clearContent();
      return list;
   }

   @Override
   public ItemStack removeItem(int p_19159_, int p_19160_) {
      ItemStack itemstack = ContainerHelper.removeItem(this.items, p_19159_, p_19160_);
      if (!itemstack.isEmpty()) {
         this.setChanged();
      }

      return itemstack;
   }

   public ItemStack removeItemType(Item p_19171_, int p_19172_) {
      ItemStack itemstack = new ItemStack(p_19171_, 0);

      for (int i = this.size - 1; i >= 0; i--) {
         ItemStack itemstack1 = this.getItem(i);
         if (itemstack1.getItem().equals(p_19171_)) {
            int j = p_19172_ - itemstack.getCount();
            ItemStack itemstack2 = itemstack1.split(j);
            itemstack.grow(itemstack2.getCount());
            if (itemstack.getCount() == p_19172_) {
               break;
            }
         }
      }

      if (!itemstack.isEmpty()) {
         this.setChanged();
      }

      return itemstack;
   }

   public ItemStack addItem(ItemStack p_19174_) {
      if (p_19174_.isEmpty()) {
         return ItemStack.EMPTY;
      }

      ItemStack itemstack = p_19174_.copy();
      this.moveItemToOccupiedSlotsWithSameType(itemstack);
      if (itemstack.isEmpty()) {
         return ItemStack.EMPTY;
      }

      this.moveItemToEmptySlots(itemstack);
      return itemstack.isEmpty() ? ItemStack.EMPTY : itemstack;
   }

   public boolean canAddItem(ItemStack p_19184_) {
      boolean flag = false;

      for (ItemStack itemstack : this.items) {
         if (itemstack.isEmpty() || ItemStack.isSameItemSameComponents(itemstack, p_19184_) && itemstack.getCount() < itemstack.getMaxStackSize()) {
            flag = true;
            break;
         }
      }

      return flag;
   }

   @Override
   public ItemStack removeItemNoUpdate(int p_19180_) {
      ItemStack itemstack = this.items.get(p_19180_);
      if (itemstack.isEmpty()) {
         return ItemStack.EMPTY;
      }

      this.items.set(p_19180_, ItemStack.EMPTY);
      return itemstack;
   }

   @Override
   public void setItem(int p_19162_, ItemStack p_19163_) {
      this.items.set(p_19162_, p_19163_);
      p_19163_.limitSize(this.getMaxStackSize(p_19163_));
      this.setChanged();
   }

   @Override
   public int getContainerSize() {
      return this.size;
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack itemstack : this.items) {
         if (!itemstack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public void setChanged() {
      if (this.listeners != null) {
         for (ContainerListener containerlistener : this.listeners) {
            containerlistener.containerChanged(this);
         }
      }
   }

   @Override
   public boolean stillValid(Player p_19167_) {
      return true;
   }

   @Override
   public void clearContent() {
      this.items.clear();
      this.setChanged();
   }

   @Override
   public void fillStackedContents(StackedItemContents p_361993_) {
      for (ItemStack itemstack : this.items) {
         p_361993_.accountStack(itemstack);
      }
   }

   @Override
   public String toString() {
      return this.items.stream().filter(p_19194_ -> !p_19194_.isEmpty()).collect(Collectors.toList()).toString();
   }

   private void moveItemToEmptySlots(ItemStack p_19190_) {
      for (int i = 0; i < this.size; i++) {
         ItemStack itemstack = this.getItem(i);
         if (itemstack.isEmpty()) {
            this.setItem(i, p_19190_.copyAndClear());
            return;
         }
      }
   }

   private void moveItemToOccupiedSlotsWithSameType(ItemStack p_19192_) {
      for (int i = 0; i < this.size; i++) {
         ItemStack itemstack = this.getItem(i);
         if (ItemStack.isSameItemSameComponents(itemstack, p_19192_)) {
            this.moveItemsBetweenStacks(p_19192_, itemstack);
            if (p_19192_.isEmpty()) {
               return;
            }
         }
      }
   }

   private void moveItemsBetweenStacks(ItemStack p_19186_, ItemStack p_19187_) {
      int i = this.getMaxStackSize(p_19187_);
      int j = Math.min(p_19186_.getCount(), i - p_19187_.getCount());
      if (j > 0) {
         p_19187_.grow(j);
         p_19186_.shrink(j);
         this.setChanged();
      }
   }

   public void fromItemList(ValueInput.TypedInputList<ItemStack> p_410556_) {
      this.clearContent();

      for (ItemStack itemstack : p_410556_) {
         this.addItem(itemstack);
      }
   }

   public void storeAsItemList(ValueOutput.TypedOutputList<ItemStack> p_408131_) {
      for (int i = 0; i < this.getContainerSize(); i++) {
         ItemStack itemstack = this.getItem(i);
         if (!itemstack.isEmpty()) {
            p_408131_.add(itemstack);
         }
      }
   }

   public NonNullList<ItemStack> getItems() {
      return this.items;
   }
}
