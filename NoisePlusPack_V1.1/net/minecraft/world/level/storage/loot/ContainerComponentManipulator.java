package net.minecraft.world.level.storage.loot;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.slot.SlotCollection;

public interface ContainerComponentManipulator<T> {
   DataComponentType<T> type();

   T empty();

   T setContents(T var1, Stream<ItemStack> var2);

   Stream<ItemStack> getContents(T var1);

   default void setContents(ItemStack p_333844_, T p_334408_, Stream<ItemStack> p_331739_) {
      T t = p_333844_.getOrDefault(this.type(), p_334408_);
      T t1 = this.setContents(t, p_331739_);
      p_333844_.set(this.type(), t1);
   }

   default void setContents(ItemStack p_331343_, Stream<ItemStack> p_333653_) {
      this.setContents(p_331343_, this.empty(), p_333653_);
   }

   default void modifyItems(ItemStack p_335094_, UnaryOperator<ItemStack> p_330990_) {
      T t = p_335094_.get(this.type());
      if (t != null) {
         UnaryOperator<ItemStack> unaryoperator = p_341972_ -> {
            if (p_341972_.isEmpty()) {
               return p_341972_;
            }

            ItemStack itemstack = p_330990_.apply(p_341972_);
            itemstack.limitSize(itemstack.getMaxStackSize());
            return itemstack;
         };
         this.setContents(p_335094_, this.getContents(t).map(unaryoperator));
      }
   }

   default SlotCollection getSlots(ItemStack p_459268_) {
      return () -> {
         T t = p_459268_.get(this.type());
         return t != null ? this.getContents(t).filter(p_450067_ -> !p_450067_.isEmpty()) : Stream.empty();
      };
   }
}
