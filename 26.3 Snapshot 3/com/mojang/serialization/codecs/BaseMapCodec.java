package com.mojang.serialization.codecs;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

public interface BaseMapCodec<K, V> {
   Codec<K> keyCodec();

   Codec<V> elementCodec();

   default <T> DataResult<Map<K, V>> decode(DynamicOps<T> ops, MapLike<T> input) {
      Object2ObjectMap<K, V> read = new Object2ObjectArrayMap();
      Builder<Pair<T, T>> failed = Stream.builder();
      DataResult<Unit> result = input.entries().reduce(DataResult.success(Unit.INSTANCE, Lifecycle.stable()), (r, pair) -> {
         DataResult<K> key = this.keyCodec().parse(ops, pair.getFirst());
         DataResult<V> value = this.elementCodec().parse(ops, pair.getSecond());
         DataResult<Pair<K, V>> entryResult = key.apply2stable(Pair::of, value);
         Optional<Pair<K, V>> entry = entryResult.resultOrPartial();
         if (entry.isPresent()) {
            V existingValue = (V)read.putIfAbsent(entry.get().getFirst(), entry.get().getSecond());
            if (existingValue != null) {
               failed.add((Pair<T, T>)pair);
               return r.apply2stable((u, p) -> u, DataResult.error(() -> "Duplicate entry for key: '" + entry.get().getFirst() + "'"));
            }
         }

         if (entryResult.isError()) {
            failed.add((Pair<T, T>)pair);
         }

         return r.apply2stable((u, p) -> u, entryResult);
      }, (r1, r2) -> r1.apply2stable((u1, u2) -> u1, r2));
      Map<K, V> elements = ImmutableMap.copyOf(read);
      T errors = ops.createMap(failed.build());
      return result.<Map<K, V>>map(unit -> elements).setPartial(elements).mapError(e -> e + " missed input: " + errors);
   }

   default <T> RecordBuilder<T> encode(Map<K, V> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
      for (Entry<K, V> entry : input.entrySet()) {
         prefix.add(this.keyCodec().encodeStart(ops, entry.getKey()), this.elementCodec().encodeStart(ops, entry.getValue()));
      }

      return prefix;
   }
}
