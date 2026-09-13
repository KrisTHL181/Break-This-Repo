package net.minecraft.network.syncher;

public record EntityDataAccessor<T>(int id, EntityDataSerializer<T> serializer) {
   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         EntityDataAccessor<?> that = (EntityDataAccessor<?>)o;
         return this.id == that.id;
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
