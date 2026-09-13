package net.minecraft.world.item.component;

import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.slot.SlotCollection;

public interface ContainerComponent<T extends ContainerComponent<T>> {
   Stream<ItemStack> itemCopies();

   int size();

   T copyWithContents(Stream<ItemStack> newContents);

   ContainerComponent.Mutable<T> asMutable();

   interface Mutable<T> extends SlotCollection {
      T toImmutable();
   }
}
