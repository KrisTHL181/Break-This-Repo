package net.minecraft.world.entity;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.slot.SlotCollection;
import org.jspecify.annotations.Nullable;

public interface SlotProvider {
   @Nullable SlotAccess getSlot(int var1);

   default SlotCollection getSlotsFromRange(IntList p_453704_) {
      List<SlotAccess> list = p_453704_.intStream().mapToObj(this::getSlot).filter(Objects::nonNull).toList();
      return SlotCollection.of(list);
   }
}
