package net.minecraft.world.item.component;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.slot.SlotSelector;

public abstract class SimpleMutableContainer<T> implements ContainerComponent.Mutable<T> {
   protected final List<ItemStack> items;

   public SimpleMutableContainer(final List<ItemStack> items) {
      this.items = items;
   }

   @Override
   public Stream<ItemStack> itemCopies() {
      return this.items.stream().map(ItemStack::copy);
   }

   @Override
   public int size() {
      return this.items.size();
   }

   @Override
   public int replaceSlotItems(final ItemProvider newItems, final SlotSelector slotSelector) {
      int successCount = 0;

      int index;
      for (index = 0; index < this.items.size() && newItems.hasNext(); index++) {
         ItemStack currentItem = this.items.get(index);
         if (slotSelector.trySelectSlot(currentItem)) {
            boolean success = this.setItem(index, newItems.next());
            if (success) {
               successCount++;
            }
         }
      }

      if (index == this.items.size()) {
         successCount += this.insertNewSlots(newItems, slotSelector);
      }

      return successCount;
   }

   @Override
   public void modifySlots(final Consumer<? super SlotAccess> consumer, final SlotSelector slotSelector) {
      for (int i = 0; i < this.items.size(); i++) {
         ItemStack currentItem = this.items.get(i);
         if (slotSelector.trySelectSlot(currentItem)) {
            int slot = i;
            consumer.accept(SlotAccess.of(() -> this.items.get(slot), stack -> this.setItem(slot, stack)));
         }
      }
   }

   protected int insertNewSlots(final ItemProvider newItems, final SlotSelector slotSelector) {
      int successCount = 0;

      while (newItems.hasNext() && this.canInsertNewSlots() && slotSelector.trySelectSlot(ItemStack.EMPTY)) {
         boolean success = this.addSlotWithItem(newItems.next());
         if (success) {
            successCount++;
         }
      }

      return successCount;
   }

   protected boolean setItem(final int slot, final ItemStack itemStack) {
      this.items.set(slot, itemStack);
      return true;
   }

   protected boolean addSlotWithItem(final ItemStack itemStack) {
      this.items.add(itemStack);
      return true;
   }

   public boolean canInsertNewSlots() {
      return false;
   }
}
