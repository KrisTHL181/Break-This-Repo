package net.minecraft.world.level.storage.loot;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ContainerComponent;
import net.minecraft.world.item.slot.SlotCollection;
import net.minecraft.world.item.slot.SlotSelector;

public record ContainerComponentManipulator<T extends ContainerComponent<T>>(DataComponentType<T> type, T empty) {
   public void setContents(final ItemStack itemStack, final T defaultValue, final Stream<ItemStack> newContents) {
      T currentValue = itemStack.getOrDefault(this.type(), defaultValue);
      T newValue = currentValue.copyWithContents(newContents);
      itemStack.set(this.type(), newValue);
   }

   public void setContents(final ItemStack itemStack, final Stream<ItemStack> newContents) {
      this.setContents(itemStack, this.empty(), newContents);
   }

   public void modifyItems(final ItemStack itemStack, final UnaryOperator<ItemStack> modifier) {
      T contents = itemStack.get(this.type());
      if (contents != null) {
         UnaryOperator<ItemStack> nonEmptyModifier = currentItemStack -> {
            if (currentItemStack.isEmpty()) {
               return currentItemStack;
            }

            ItemStack newItemStack = modifier.apply(currentItemStack);
            newItemStack.limitSize(newItemStack.getMaxStackSize());
            return newItemStack;
         };
         this.setContents(itemStack, contents.itemCopies().map(nonEmptyModifier));
      }
   }

   public SlotCollection getSlots(final ItemStack itemStack) {
      return new ContainerComponentManipulator.ComponentSlotCollection<>(itemStack, this);
   }

   private record ComponentSlotCollection<T extends ContainerComponent<T>>(ItemStack itemStack, ContainerComponentManipulator<T> component)
      implements SlotCollection {
      @Override
      public Stream<ItemStack> itemCopies() {
         T contents = this.itemStack.get(this.component.type());
         return contents != null ? contents.itemCopies() : Stream.empty();
      }

      @Override
      public int size() {
         T contents = this.itemStack.get(this.component.type());
         return contents != null ? contents.size() : 0;
      }

      @Override
      public int replaceSlotItems(final ItemProvider items, final SlotSelector slotSelector) {
         T currentValue = this.itemStack.getOrDefault(this.component.type(), this.component.empty());
         ContainerComponent.Mutable<T> mutable = currentValue.asMutable();
         int slotsReplaced = mutable.replaceSlotItems(items, slotSelector);
         this.itemStack.set(this.component.type(), mutable.toImmutable());
         return slotsReplaced;
      }

      @Override
      public void modifySlots(final Consumer<? super SlotAccess> consumer, final SlotSelector slotSelector) {
         T currentValue = this.itemStack.getOrDefault(this.component.type(), this.component.empty());
         ContainerComponent.Mutable<T> mutable = currentValue.asMutable();
         mutable.modifySlots(consumer, slotSelector);
         this.itemStack.set(this.component.type(), mutable.toImmutable());
      }
   }
}
