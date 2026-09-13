package net.minecraft.core.dispenser;

public abstract class OptionalDispenseItemBehavior extends DefaultDispenseItemBehavior {
   private boolean success = true;

   public boolean isSuccess() {
      return this.success;
   }

   public void setSuccess(final boolean success) {
      this.success = success;
   }

   @Override
   protected void playSound(final BlockSource source) {
      source.level().levelEvent(this.isSuccess() ? 1000 : 1001, source.pos(), 0);
   }
}
