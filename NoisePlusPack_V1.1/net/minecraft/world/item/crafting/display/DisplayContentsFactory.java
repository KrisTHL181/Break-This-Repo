package net.minecraft.world.item.crafting.display;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface DisplayContentsFactory<T> {
   interface ForRemainders<T> extends DisplayContentsFactory<T> {
      T addRemainder(T var1, List<T> var2);
   }

   interface ForStacks<T> extends DisplayContentsFactory<T> {
      default T forStack(Holder<Item> p_364562_) {
         return this.forStack(new ItemStack(p_364562_));
      }

      default T forStack(Item p_361017_) {
         return this.forStack(new ItemStack(p_361017_));
      }

      T forStack(ItemStack var1);
   }
}
