package com.mojang.serialization.codecs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class SimpleMapCodec<K, V> extends MapCodec<Map<K, V>> implements BaseMapCodec<K, V> {
   private final Codec<K> keyCodec;
   private final Codec<V> elementCodec;
   private final Keyable keys;

   public SimpleMapCodec(Codec<K> keyCodec, Codec<V> elementCodec, Keyable keys) {
      this.keyCodec = keyCodec;
      this.elementCodec = elementCodec;
      this.keys = keys;
   }

   @Override
   public Codec<K> keyCodec() {
      return this.keyCodec;
   }

   @Override
   public Codec<V> elementCodec() {
      return this.elementCodec;
   }

   @Override
   public <T> Stream<T> keys(DynamicOps<T> ops) {
      return this.keys.keys(ops);
   }

   @Override
   public <T> DataResult<Map<K, V>> decode(DynamicOps<T> ops, MapLike<T> input) {
      return BaseMapCodec.super.decode(ops, input);
   }

   @Override
   public <T> RecordBuilder<T> encode(Map<K, V> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
      return BaseMapCodec.super.encode(input, ops, prefix);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         SimpleMapCodec<?, ?> that = (SimpleMapCodec<?, ?>)o;
         return Objects.equals(this.keyCodec, that.keyCodec) && Objects.equals(this.elementCodec, that.elementCodec);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.keyCodec, this.elementCodec);
   }

   @Override
   public String toString() {
      return "SimpleMapCodec[" + this.keyCodec + " -> " + this.elementCodec + "]";
   }
}
