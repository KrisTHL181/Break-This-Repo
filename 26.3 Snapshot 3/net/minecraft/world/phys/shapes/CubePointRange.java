package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;

public class CubePointRange extends AbstractDoubleList {
   private final int parts;

   public CubePointRange(final int parts) {
      if (parts <= 0) {
         throw new IllegalArgumentException("Need at least 1 part");
      }

      this.parts = parts;
   }

   public double getDouble(final int index) {
      return (double)index / this.parts;
   }

   public int size() {
      return this.parts + 1;
   }
}
