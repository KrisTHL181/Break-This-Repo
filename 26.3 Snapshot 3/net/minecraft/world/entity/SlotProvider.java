package net.minecraft.world.entity;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.slot.SlotCollection;
import org.jspecify.annotations.Nullable;

public interface SlotProvider {
   @Nullable SlotAccess getSlot(int slot);

   default SlotCollection getSlotsFromRange(final IntList slots) {
      List<SlotAccess> slotList = slots.intStream().mapToObj(this::getSlot).filter(Objects::nonNull).toList();
      return SlotCollection.of(slotList);
   }
}
