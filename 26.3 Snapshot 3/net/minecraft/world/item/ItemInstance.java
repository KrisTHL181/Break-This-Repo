package net.minecraft.world.item;

import net.minecraft.core.TypedInstance;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;

public interface ItemInstance extends TypedInstance<Item>, DataComponentGetter {
   String FIELD_ID = "id";
   String FIELD_COUNT = "count";
   String FIELD_COMPONENTS = "components";

   int count();

   default int getMaxStackSize() {
      return this.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
   }
}
