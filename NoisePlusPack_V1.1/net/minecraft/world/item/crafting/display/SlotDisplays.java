package net.minecraft.world.item.crafting.display;

import net.minecraft.core.Registry;

public class SlotDisplays {
   public static SlotDisplay.Type<?> bootstrap(Registry<SlotDisplay.Type<?>> p_361360_) {
      Registry.register(p_361360_, "empty", SlotDisplay.Empty.TYPE);
      Registry.register(p_361360_, "any_fuel", SlotDisplay.AnyFuel.TYPE);
      Registry.register(p_361360_, "item", SlotDisplay.ItemSlotDisplay.TYPE);
      Registry.register(p_361360_, "item_stack", SlotDisplay.ItemStackSlotDisplay.TYPE);
      Registry.register(p_361360_, "tag", SlotDisplay.TagSlotDisplay.TYPE);
      Registry.register(p_361360_, "smithing_trim", SlotDisplay.SmithingTrimDemoSlotDisplay.TYPE);
      Registry.register(p_361360_, "with_remainder", SlotDisplay.WithRemainder.TYPE);
      return Registry.register(p_361360_, "composite", SlotDisplay.Composite.TYPE);
   }
}
