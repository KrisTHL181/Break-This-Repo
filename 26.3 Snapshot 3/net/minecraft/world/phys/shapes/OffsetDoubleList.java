package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public class OffsetDoubleList extends AbstractDoubleList {
   private final DoubleList delegate;
   private final double offset;

   public OffsetDoubleList(final DoubleList delegate, final double offset) {
      this.delegate = delegate;
      this.offset = offset;
   }

   public double getDouble(final int index) {
      return this.delegate.getDouble(index) + this.offset;
   }

   public int size() {
      return this.delegate.size();
   }
}
