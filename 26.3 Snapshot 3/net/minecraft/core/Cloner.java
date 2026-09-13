package net.minecraft.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

public class Cloner<T> {
   private final Codec<T> directCodec;

   private Cloner(final Codec<T> directCodec) {
      this.directCodec = directCodec;
   }

   public T clone(final T value, final HolderLookup.Provider from, final HolderLookup.Provider to) {
      DynamicOps<Object> sourceOps = from.createSerializationContext(JavaOps.INSTANCE);
      DynamicOps<Object> targetOps = to.createSerializationContext(JavaOps.INSTANCE);
      Object serialized = this.directCodec.encodeStart(sourceOps, value).getOrThrow(error -> new IllegalStateException("Failed to encode: " + error));
      return (T)this.directCodec.parse(targetOps, serialized).getOrThrow(error -> new IllegalStateException("Failed to decode: " + error));
   }

   public static class Factory {
      private final Map<ResourceKey<? extends Registry<?>>, Cloner<?>> codecs = new HashMap<>();

      public <T> Cloner.Factory addCodec(final ResourceKey<? extends Registry<? extends T>> key, final Codec<T> codec) {
         this.codecs.put(key, new Cloner(codec));
         return this;
      }

      public <T> @Nullable Cloner<T> cloner(final ResourceKey<? extends Registry<? extends T>> key) {
         return (Cloner<T>)this.codecs.get(key);
      }
   }
}
