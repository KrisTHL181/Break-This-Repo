package net.minecraft.core.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Map.Entry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record TypedDataComponent<T>(DataComponentType<T> type, T value) {
   public static final StreamCodec<RegistryFriendlyByteBuf, TypedDataComponent<?>> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, TypedDataComponent<?>>() {
      public TypedDataComponent<?> decode(RegistryFriendlyByteBuf p_333264_) {
         DataComponentType<?> datacomponenttype = DataComponentType.STREAM_CODEC.decode(p_333264_);
         return decodeTyped(p_333264_, (DataComponentType<T>)datacomponenttype);
      }

      private static <T> TypedDataComponent<T> decodeTyped(RegistryFriendlyByteBuf p_329132_, DataComponentType<T> p_330664_) {
         return new TypedDataComponent<>(p_330664_, p_330664_.streamCodec().decode(p_329132_));
      }

      public void encode(RegistryFriendlyByteBuf p_334022_, TypedDataComponent<?> p_331938_) {
         encodeCap(p_334022_, (TypedDataComponent<T>)p_331938_);
      }

      private static <T> void encodeCap(RegistryFriendlyByteBuf p_331689_, TypedDataComponent<T> p_331096_) {
         DataComponentType.STREAM_CODEC.encode(p_331689_, p_331096_.type());
         p_331096_.type().streamCodec().encode(p_331689_, p_331096_.value());
      }
   };

   static TypedDataComponent<?> fromEntryUnchecked(Entry<DataComponentType<?>, Object> p_335332_) {
      return createUnchecked(p_335332_.getKey(), p_335332_.getValue());
   }

   public static <T> TypedDataComponent<T> createUnchecked(DataComponentType<T> p_332647_, Object p_330924_) {
      return new TypedDataComponent<>(p_332647_, (T)p_330924_);
   }

   public void applyTo(PatchedDataComponentMap p_334157_) {
      p_334157_.set(this.type, this.value);
   }

   public <D> DataResult<D> encodeValue(DynamicOps<D> p_331110_) {
      Codec<T> codec = this.type.codec();
      return codec == null ? DataResult.error(() -> "Component of type " + this.type + " is not encodable") : codec.encodeStart(p_331110_, this.value);
   }

   @Override
   public String toString() {
      return this.type + "=>" + this.value;
   }
}
