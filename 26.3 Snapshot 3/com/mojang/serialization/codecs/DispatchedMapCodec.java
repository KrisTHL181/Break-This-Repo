package com.mojang.serialization.codecs;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

public record DispatchedMapCodec<K, V>(Codec<K> keyCodec, Function<K, Codec<? extends V>> valueCodecFunction) implements Codec<Map<K, V>> {
   public <T> DataResult<T> encode(Map<K, V> input, DynamicOps<T> ops, T prefix) {
      RecordBuilder<T> mapBuilder = ops.mapBuilder();

      for (Entry<K, V> entry : input.entrySet()) {
         mapBuilder.add(this.keyCodec.encodeStart(ops, entry.getKey()), this.encodeValue(this.valueCodecFunction.apply(entry.getKey()), entry.getValue(), ops));
      }

      return mapBuilder.build(prefix);
   }

   private <T, V2 extends V> DataResult<T> encodeValue(Codec<V2> codec, V input, DynamicOps<T> ops) {
      return codec.encodeStart(ops, (V2)input);
   }

   @Override
   public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> ops, T input) {
      return ops.getMap(input)
         .flatMap(
            map -> {
               Map<K, V> entries = new Object2ObjectArrayMap();
               Builder<Pair<T, T>> failed = Stream.builder();
               DataResult<Unit> finalResult = map.entries()
                  .reduce(
                     DataResult.success(Unit.INSTANCE, Lifecycle.stable()),
                     (result, entry) -> this.parseEntry(result, ops, (Pair<T, T>)entry, entries, failed),
                     (r1, r2) -> r1.apply2stable((u1, u2) -> u1, r2)
                  );
               Pair<Map<K, V>, T> pair = Pair.of(ImmutableMap.copyOf(entries), input);
               T errors = ops.createMap(failed.build());
               return finalResult.<Pair<Map<K, V>, T>>map(ignored -> pair).setPartial(pair).mapError(error -> error + " missed input: " + errors);
            }
         );
   }

   private <T> DataResult<Unit> parseEntry(DataResult<Unit> result, DynamicOps<T> ops, Pair<T, T> input, Map<K, V> entries, Builder<Pair<T, T>> failed) {
      DataResult<K> keyResult = this.keyCodec.parse(ops, input.getFirst());
      DataResult<V> valueResult = keyResult.map(this.valueCodecFunction)
         .flatMap(valueCodec -> valueCodec.parse(ops, input.getSecond()).map(Function.identity()));
      DataResult<Pair<K, V>> entryResult = keyResult.apply2stable(Pair::of, valueResult);
      Optional<Pair<K, V>> entry = entryResult.resultOrPartial();
      if (entry.isPresent()) {
         K key = entry.get().getFirst();
         V value = entry.get().getSecond();
         if (entries.putIfAbsent(key, value) != null) {
            failed.add(input);
            return result.apply2stable((u, p) -> u, DataResult.error(() -> "Duplicate entry for key: '" + key + "'"));
         }
      }

      if (entryResult.isError()) {
         failed.add(input);
      }

      return result.apply2stable((u, p) -> u, entryResult);
   }
}
