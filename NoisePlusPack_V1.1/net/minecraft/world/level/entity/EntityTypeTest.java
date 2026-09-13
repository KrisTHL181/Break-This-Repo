package net.minecraft.world.level.entity;

import org.jspecify.annotations.Nullable;

public interface EntityTypeTest<B, T extends B> {
   static <B, T extends B> EntityTypeTest<B, T> forClass(final Class<T> p_156917_) {
      return new EntityTypeTest<B, T>() {
         @Override
         public @Nullable T tryCast(B p_156924_) {
            return (T)(p_156917_.isInstance(p_156924_) ? p_156924_ : null);
         }

         @Override
         public Class<? extends B> getBaseClass() {
            return p_156917_;
         }
      };
   }

   static <B, T extends B> EntityTypeTest<B, T> forExactClass(final Class<T> p_310060_) {
      return new EntityTypeTest<B, T>() {
         @Override
         public @Nullable T tryCast(B p_309868_) {
            return (T)(p_310060_.equals(p_309868_.getClass()) ? p_309868_ : null);
         }

         @Override
         public Class<? extends B> getBaseClass() {
            return p_310060_;
         }
      };
   }

   @Nullable T tryCast(B var1);

   Class<? extends B> getBaseClass();
}
