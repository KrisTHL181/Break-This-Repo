package com.mojang.serialization.codecs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.RecordBuilder;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class CompoundListCodec<K, V> implements Codec<List<Pair<K, V>>> {
   private final Codec<K> keyCodec;
   private final Codec<V> elementCodec;

   public CompoundListCodec(Codec<K> keyCodec, Codec<V> elementCodec) {
      this.keyCodec = keyCodec;
      this.elementCodec = elementCodec;
   }

   @Override
   public <T> DataResult<Pair<List<Pair<K, V>>, T>> decode(DynamicOps<T> ops, T input) {
      return ops.getMapEntries(input).flatMap(map -> {
         Builder<Pair<K, V>> read = ImmutableList.builder();
         com.google.common.collect.ImmutableMap.Builder<T, T> failed = ImmutableMap.builder();
         AtomicReference<DataResult<Unit>> result = new AtomicReference<>(DataResult.success(Unit.INSTANCE, Lifecycle.experimental()));
         map.accept((key, value) -> {
            DataResult<K> k = this.keyCodec.parse(ops, key);
            DataResult<V> v = this.elementCodec.parse(ops, value);
            DataResult<Pair<K, V>> readEntry = k.apply2stable(Pair::new, v);
            readEntry.error().ifPresent(e -> failed.put(key, value));
            result.setPlain(result.getPlain().apply2stable((u, e) -> {
               read.add(e);
               return u;
            }, readEntry));
         });
         ImmutableList<Pair<K, V>> elements = read.build();
         T errors = ops.createMap(failed.build());
         Pair<List<Pair<K, V>>, T> pair = Pair.of(elements, errors);
         return result.getPlain().<Pair<List<Pair<K, V>>, T>>map(unit -> pair).setPartial(pair);
      });
   }

   public <T> DataResult<T> encode(List<Pair<K, V>> input, DynamicOps<T> ops, T prefix) {
      RecordBuilder<T> builder = ops.mapBuilder();

      for (Pair<K, V> pair : input) {
         builder.add(this.keyCodec.encodeStart(ops, pair.getFirst()), this.elementCodec.encodeStart(ops, pair.getSecond()));
      }

      return builder.build(prefix);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         CompoundListCodec<?, ?> that = (CompoundListCodec<?, ?>)o;
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
      return "CompoundListCodec[" + this.keyCodec + " -> " + this.elementCodec + "]";
   }
}
