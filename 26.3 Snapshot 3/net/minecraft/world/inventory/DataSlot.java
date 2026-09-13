package net.minecraft.world.inventory;

public abstract class DataSlot {
   private int prevValue;

   public static DataSlot forContainer(final ContainerData container, final int dataId) {
      return new DataSlot() {
         @Override
         public int get() {
            return container.get(dataId);
         }

         @Override
         public void set(final int value) {
            container.set(dataId, value);
         }
      };
   }

   public static DataSlot shared(final int[] storage, final int index) {
      return new DataSlot() {
         @Override
         public int get() {
            return storage[index];
         }

         @Override
         public void set(final int value) {
            storage[index] = value;
         }
      };
   }

   public static DataSlot standalone() {
      return new DataSlot() {
         private int value;

         @Override
         public int get() {
            return this.value;
         }

         @Override
         public void set(final int value) {
            this.value = value;
         }
      };
   }

   public abstract int get();

   public abstract void set(int value);

   public boolean checkAndClearUpdateFlag() {
      int currentValue = this.get();
      boolean result = currentValue != this.prevValue;
      this.prevValue = currentValue;
      return result;
   }
}
