package net.minecraft.world.item.slot;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;

public class CountingModifier implements Consumer<SlotAccess> {
   private final UnaryOperator<ItemStack> modifier;
   private int updatedCount;

   public CountingModifier(final UnaryOperator<ItemStack> modifier) {
      this.modifier = modifier;
   }

   public void accept(final SlotAccess slot) {
      boolean success = slot.set(this.modifier.apply(slot.get().copy()));
      if (success) {
         this.updatedCount++;
      }
   }

   public int updatedCount() {
      return this.updatedCount;
   }
}
