package net.minecraft.world.inventory;

public abstract class DataSlot {
   private int prevValue;

   public static DataSlot forContainer(final ContainerData p_39404_, final int p_39405_) {
      return new DataSlot() {
         @Override
         public int get() {
            return p_39404_.get(p_39405_);
         }

         @Override
         public void set(int p_39416_) {
            p_39404_.set(p_39405_, p_39416_);
         }
      };
   }

   public static DataSlot shared(final int[] p_39407_, final int p_39408_) {
      return new DataSlot() {
         @Override
         public int get() {
            return p_39407_[p_39408_];
         }

         @Override
         public void set(int p_39424_) {
            p_39407_[p_39408_] = p_39424_;
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
         public void set(int p_39429_) {
            this.value = p_39429_;
         }
      };
   }

   public abstract int get();

   public abstract void set(int var1);

   public boolean checkAndClearUpdateFlag() {
      int i = this.get();
      boolean flag = i != this.prevValue;
      this.prevValue = i;
      return flag;
   }
}
