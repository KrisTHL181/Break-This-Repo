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

   Cloner(Codec<T> p_312738_) {
      this.directCodec = p_312738_;
   }

   public T clone(T p_311468_, HolderLookup.Provider p_309751_, HolderLookup.Provider p_312597_) {
      DynamicOps<Object> dynamicops = p_309751_.createSerializationContext(JavaOps.INSTANCE);
      DynamicOps<Object> dynamicops1 = p_312597_.createSerializationContext(JavaOps.INSTANCE);
      Object object = this.directCodec.encodeStart(dynamicops, p_311468_).getOrThrow(p_311642_ -> new IllegalStateException("Failed to encode: " + p_311642_));
      return (T)this.directCodec.parse(dynamicops1, object).getOrThrow(p_311707_ -> new IllegalStateException("Failed to decode: " + p_311707_));
   }

   public static class Factory {
      private final Map<ResourceKey<? extends Registry<?>>, Cloner<?>> codecs = new HashMap<>();

      public <T> Cloner.Factory addCodec(ResourceKey<? extends Registry<? extends T>> p_310427_, Codec<T> p_312943_) {
         this.codecs.put(p_310427_, new Cloner(p_312943_));
         return this;
      }

      public <T> @Nullable Cloner<T> cloner(ResourceKey<? extends Registry<? extends T>> p_311946_) {
         return (Cloner<T>)this.codecs.get(p_311946_);
      }
   }
}
