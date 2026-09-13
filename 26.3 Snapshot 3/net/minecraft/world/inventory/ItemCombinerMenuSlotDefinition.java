package net.minecraft.world.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

public class ItemCombinerMenuSlotDefinition {
   private final List<ItemCombinerMenuSlotDefinition.SlotDefinition> slots;
   private final ItemCombinerMenuSlotDefinition.SlotDefinition resultSlot;

   private ItemCombinerMenuSlotDefinition(
      final List<ItemCombinerMenuSlotDefinition.SlotDefinition> inputSlots, final ItemCombinerMenuSlotDefinition.SlotDefinition resultSlot
   ) {
      if (!inputSlots.isEmpty() && !resultSlot.equals(ItemCombinerMenuSlotDefinition.SlotDefinition.EMPTY)) {
         this.slots = inputSlots;
         this.resultSlot = resultSlot;
      } else {
         throw new IllegalArgumentException("Need to define both inputSlots and resultSlot");
      }
   }

   public static ItemCombinerMenuSlotDefinition.Builder create() {
      return new ItemCombinerMenuSlotDefinition.Builder();
   }

   public ItemCombinerMenuSlotDefinition.SlotDefinition getSlot(final int index) {
      return this.slots.get(index);
   }

   public ItemCombinerMenuSlotDefinition.SlotDefinition getResultSlot() {
      return this.resultSlot;
   }

   public List<ItemCombinerMenuSlotDefinition.SlotDefinition> getSlots() {
      return this.slots;
   }

   public int getNumOfInputSlots() {
      return this.slots.size();
   }

   public int getResultSlotIndex() {
      return this.getNumOfInputSlots();
   }

   public static class Builder {
      private final List<ItemCombinerMenuSlotDefinition.SlotDefinition> inputSlots = new ArrayList<>();
      private ItemCombinerMenuSlotDefinition.SlotDefinition resultSlot = ItemCombinerMenuSlotDefinition.SlotDefinition.EMPTY;

      public ItemCombinerMenuSlotDefinition.Builder withSlot(
         final int slotIndex, final int xPlacement, final int yPlacement, final Predicate<ItemStack> mayPlace
      ) {
         this.inputSlots.add(new ItemCombinerMenuSlotDefinition.SlotDefinition(slotIndex, xPlacement, yPlacement, mayPlace));
         return this;
      }

      public ItemCombinerMenuSlotDefinition.Builder withResultSlot(final int slotIndex, final int xPlacement, final int yPlacement) {
         this.resultSlot = new ItemCombinerMenuSlotDefinition.SlotDefinition(slotIndex, xPlacement, yPlacement, itemStack -> false);
         return this;
      }

      public ItemCombinerMenuSlotDefinition build() {
         int inputCount = this.inputSlots.size();

         for (int i = 0; i < inputCount; i++) {
            ItemCombinerMenuSlotDefinition.SlotDefinition inputDefinition = this.inputSlots.get(i);
            if (inputDefinition.slotIndex != i) {
               throw new IllegalArgumentException("Expected input slots to have continous indexes");
            }
         }

         if (this.resultSlot.slotIndex != inputCount) {
            throw new IllegalArgumentException("Expected result slot index to follow last input slot");
         } else {
            return new ItemCombinerMenuSlotDefinition(this.inputSlots, this.resultSlot);
         }
      }
   }

   public record SlotDefinition(int slotIndex, int x, int y, Predicate<ItemStack> mayPlace) {
      private static final ItemCombinerMenuSlotDefinition.SlotDefinition EMPTY = new ItemCombinerMenuSlotDefinition.SlotDefinition(0, 0, 0, itemStack -> true);
   }
}
