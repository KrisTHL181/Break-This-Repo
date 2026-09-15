package net.minecraft.world.level.saveddata;

public abstract class SavedData {
   private boolean dirty;

   public void setDirty() {
      this.setDirty(true);
   }

   public void setDirty(boolean p_77761_) {
      this.dirty = p_77761_;
   }

   public boolean isDirty() {
      return this.dirty;
   }
}
