package net.minecraft.world.inventory;

public class SimpleContainerData implements ContainerData {
   private final int[] ints;

   public SimpleContainerData(final int count) {
      this.ints = new int[count];
   }

   @Override
   public int get(final int dataId) {
      return this.ints[dataId];
   }

   @Override
   public void set(final int dataId, final int value) {
      this.ints[dataId] = value;
   }

   @Override
   public int getCount() {
      return this.ints.length;
   }
}
