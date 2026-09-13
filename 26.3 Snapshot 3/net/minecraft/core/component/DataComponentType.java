package net.minecraft.core.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public interface DataComponentType<T> {
   Codec<DataComponentType<?>> CODEC = Codec.lazyInitialized(() -> BuiltInRegistries.DATA_COMPONENT_TYPE.byNameCodec());
   StreamCodec<RegistryFriendlyByteBuf, DataComponentType<?>> STREAM_CODEC = StreamCodec.recursive(c -> ByteBufCodecs.registry(Registries.DATA_COMPONENT_TYPE));
   Codec<DataComponentType<?>> PERSISTENT_CODEC = CODEC.validate(
      type -> type.isTransient()
         ? DataResult.error(() -> "Encountered transient component " + BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type))
         : DataResult.success(type)
   );
   Codec<Map<DataComponentType<?>, Object>> VALUE_MAP_CODEC = Codec.dispatchedMap(PERSISTENT_CODEC, DataComponentType::codecOrThrow);

   static <T> DataComponentType.Builder<T> builder() {
      return new DataComponentType.Builder<>();
   }

   @Nullable Codec<T> codec();

   default Codec<T> codecOrThrow() {
      Codec<T> codec = this.codec();
      if (codec == null) {
         throw new IllegalStateException(this + " is not a persistent component");
      } else {
         return codec;
      }
   }

   default boolean isTransient() {
      return this.codec() == null;
   }

   boolean ignoreSwapAnimation();

   StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec();

   class Builder<T> {
      private @Nullable Codec<T> codec;
      private @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;
      private boolean cacheEncoding;
      private boolean ignoreSwapAnimation;

      public DataComponentType.Builder<T> persistent(final Codec<T> codec) {
         this.codec = codec;
         return this;
      }

      public DataComponentType.Builder<T> networkSynchronized(final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
         this.streamCodec = streamCodec;
         return this;
      }

      public DataComponentType.Builder<T> cacheEncoding() {
         this.cacheEncoding = true;
         return this;
      }

      public DataComponentType<T> build() {
         StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec = Objects.requireNonNullElseGet(
            this.streamCodec, () -> ByteBufCodecs.fromCodecWithRegistries(Objects.requireNonNull(this.codec, "Missing Codec for component"))
         );
         Codec<T> cachingCodec = this.cacheEncoding && this.codec != null ? DataComponents.ENCODER_CACHE.wrap(this.codec) : this.codec;
         return new DataComponentType.Builder.SimpleType<>(cachingCodec, streamCodec, this.ignoreSwapAnimation);
      }

      public DataComponentType.Builder<T> ignoreSwapAnimation() {
         this.ignoreSwapAnimation = true;
         return this;
      }

      private static class SimpleType<T> implements DataComponentType<T> {
         private final @Nullable Codec<T> codec;
         private final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;
         private final boolean ignoreSwapAnimation;

         private SimpleType(
            final @Nullable Codec<T> codec, final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec, final boolean ignoreSwapAnimation
         ) {
            this.codec = codec;
            this.streamCodec = streamCodec;
            this.ignoreSwapAnimation = ignoreSwapAnimation;
         }

         @Override
         public boolean ignoreSwapAnimation() {
            return this.ignoreSwapAnimation;
         }

         @Override
         public @Nullable Codec<T> codec() {
            return this.codec;
         }

         @Override
         public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
            return this.streamCodec;
         }

         @Override
         public String toString() {
            return Util.getRegisteredName((Registry<DataComponentType.Builder.SimpleType<T>>)BuiltInRegistries.DATA_COMPONENT_TYPE, this);
         }
      }
   }
}
