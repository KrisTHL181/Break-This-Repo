package net.minecraft.world.level.entity;

import org.jspecify.annotations.Nullable;

public interface EntityTypeTest<B, T extends B> {
   static <B, T extends B> EntityTypeTest<B, T> forClass(final Class<T> cls) {
      return new EntityTypeTest<B, T>() {
         @Override
         public @Nullable T tryCast(final B entity) {
            return (T)(cls.isInstance(entity) ? entity : null);
         }

         @Override
         public Class<? extends B> getBaseClass() {
            return cls;
         }
      };
   }

   static <B, T extends B> EntityTypeTest<B, T> forExactClass(final Class<T> cls) {
      return new EntityTypeTest<B, T>() {
         @Override
         public @Nullable T tryCast(final B entity) {
            return (T)(cls.equals(entity.getClass()) ? entity : null);
         }

         @Override
         public Class<? extends B> getBaseClass() {
            return cls;
         }
      };
   }

   @Nullable T tryCast(B entity);

   Class<? extends B> getBaseClass();
}
