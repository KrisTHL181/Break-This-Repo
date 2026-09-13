package net.minecraft.network.syncher;

public record EntityDataAccessor<T>(int id, EntityDataSerializer<T> serializer) {
   @Override
   public boolean equals(Object p_135018_) {
      if (this == p_135018_) {
         return true;
      } else if (p_135018_ != null && this.getClass() == p_135018_.getClass()) {
         EntityDataAccessor<?> entitydataaccessor = (EntityDataAccessor<?>)p_135018_;
         return this.id == entitydataaccessor.id;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.id;
   }

   @Override
   public String toString() {
      return "<entity data: " + this.id + ">";
   }
}
