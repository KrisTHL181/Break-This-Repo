package net.minecraft.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.Tag;

public class EncoderCache {
   final LoadingCache<EncoderCache.Key<?, ?>, DataResult<?>> cache;

   public EncoderCache(int p_328135_) {
      this.cache = CacheBuilder.newBuilder()
         .maximumSize(p_328135_)
         .concurrencyLevel(1)
         .softValues()
         .build(new CacheLoader<EncoderCache.Key<?, ?>, DataResult<?>>() {
            public DataResult<?> load(EncoderCache.Key<?, ?> p_334212_) {
               return p_334212_.resolve();
            }
         });
   }

   public <A> Codec<A> wrap(final Codec<A> p_332774_) {
      return new Codec<A>() {
         public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> p_335845_, T p_329817_) {
            return p_332774_.decode(p_335845_, p_329817_);
         }

         public <T> DataResult<T> encode(A p_328409_, DynamicOps<T> p_330058_, T p_328392_) {
            return ((DataResult)EncoderCache.this.cache.getUnchecked(new EncoderCache.Key(p_332774_, p_328409_, p_330058_)))
               .map(p_336406_ -> p_336406_ instanceof Tag tag ? tag.copy() : p_336406_);
         }
      };
   }

   record Key<A, T>(Codec<A> codec, A value, DynamicOps<T> ops) {
      public DataResult<T> resolve() {
         return this.codec.encodeStart(this.ops, this.value);
      }

      @Override
      public boolean equals(Object p_334040_) {
         if (this == p_334040_) {
            return true;
         } else {
            return p_334040_ instanceof EncoderCache.Key<?, ?> key
               ? this.codec == key.codec && this.value.equals(key.value) && this.ops.equals(key.ops)
               : false;
         }
      }

      @Override
      public int hashCode() {
         int i = System.identityHashCode(this.codec);
         i = 31 * i + this.value.hashCode();
         return 31 * i + this.ops.hashCode();
      }
   }
}
