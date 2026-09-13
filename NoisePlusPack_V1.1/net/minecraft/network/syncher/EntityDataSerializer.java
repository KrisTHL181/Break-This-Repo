package net.minecraft.network.syncher;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface EntityDataSerializer<T> {
   StreamCodec<? super RegistryFriendlyByteBuf, T> codec();

   default EntityDataAccessor<T> createAccessor(int p_135022_) {
      return new EntityDataAccessor<>(p_135022_, this);
   }

   T copy(T var1);

   static <T> EntityDataSerializer<T> forValueType(StreamCodec<? super RegistryFriendlyByteBuf, T> p_332495_) {
      return () -> p_332495_;
   }

   interface ForValueType<T> extends EntityDataSerializer<T> {
      @Override
      default T copy(T p_238112_) {
         return p_238112_;
      }
   }
}
