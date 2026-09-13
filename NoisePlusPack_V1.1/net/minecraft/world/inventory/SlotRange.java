package net.minecraft.world.inventory;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.util.StringRepresentable;

public interface SlotRange extends StringRepresentable {
   IntList slots();

   default int size() {
      return this.slots().size();
   }

   static SlotRange of(final String p_332630_, final IntList p_330943_) {
      return new SlotRange() {
         @Override
         public IntList slots() {
            return p_330943_;
         }

         @Override
         public String getSerializedName() {
            return p_332630_;
         }

         @Override
         public String toString() {
            return p_332630_;
         }
      };
   }
}
