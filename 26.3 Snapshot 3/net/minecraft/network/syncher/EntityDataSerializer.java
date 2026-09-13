package net.minecraft.network.syncher;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface EntityDataSerializer<T> {
   StreamCodec<? super RegistryFriendlyByteBuf, T> codec();

   default EntityDataAccessor<T> createAccessor(final int id) {
      return new EntityDataAccessor<>(id, this);
   }

   T copy(T value);

   static <T> EntityDataSerializer<T> forValueType(final StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
      return () -> codec;
   }

   interface ForValueType<T> extends EntityDataSerializer<T> {
      @Override
      default T copy(final T value) {
         return value;
      }
   }
}
