package com.mojang.serialization.codecs;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.function.Function;
import java.util.stream.Stream;

public class KeyDispatchCodec<K, V> extends MapCodec<V> {
   private static final String COMPRESSED_VALUE_KEY = "value";
   private final MapCodec<K> keyCodec;
   private final Function<? super V, ? extends DataResult<? extends K>> type;
   private final Function<? super K, ? extends DataResult<? extends MapDecoder<? extends V>>> decoder;
   private final Function<? super V, ? extends DataResult<? extends MapEncoder<V>>> encoder;

   protected KeyDispatchCodec(
      MapCodec<K> keyCodec,
      Function<? super V, ? extends DataResult<? extends K>> type,
      Function<? super K, ? extends DataResult<? extends MapDecoder<? extends V>>> decoder,
      Function<? super V, ? extends DataResult<? extends MapEncoder<V>>> encoder
   ) {
      this.keyCodec = keyCodec;
      this.type = type;
      this.decoder = decoder;
      this.encoder = encoder;
   }

   public KeyDispatchCodec(
      MapCodec<K> keyCodec,
      Function<? super V, ? extends DataResult<? extends K>> type,
      Function<? super K, ? extends DataResult<? extends MapCodec<? extends V>>> codec
   ) {
      this(keyCodec, type, codec, v -> getCodec(type, codec, (V)v));
   }

   @Override
   public <T> DataResult<V> decode(DynamicOps<T> ops, MapLike<T> input) {
      return this.keyCodec
         .decode(ops, input)
         .flatMap(
            type -> this.decoder
               .apply(type)
               .flatMap(
                  elementDecoder -> {
                     if (ops.compressMaps()) {
                        T value = input.get(ops.createString("value"));
                        return value == null
                           ? DataResult.error(() -> "Input does not have a \"value\" entry: " + input)
                           : elementDecoder.decoder().parse(ops, value).map(Function.identity());
                     } else {
                        return elementDecoder.decode(ops, input).map(Function.identity());
                     }
                  }
               )
         );
   }

   @Override
   public <T> RecordBuilder<T> encode(V input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
      DataResult<? extends MapEncoder<V>> encoderResult = (DataResult<? extends MapEncoder<V>>)this.encoder.apply(input);
      DataResult<? extends K> typeResult = (DataResult<? extends K>)this.type.apply(input);
      RecordBuilder<T> builder = prefix.withErrorsFrom(encoderResult).withErrorsFrom(typeResult);
      if (!encoderResult.isError() && !typeResult.isError()) {
         MapEncoder<V> elementEncoder = (MapEncoder<V>)encoderResult.getOrThrow();
         K type = (K)typeResult.getOrThrow();
         if (ops.compressMaps()) {
            return this.keyCodec.encode(type, ops, builder).add("value", elementEncoder.encoder().encodeStart(ops, input));
         }

         RecordBuilder<T> encodedContents = elementEncoder.encode(input, ops, builder);
         return this.keyCodec.encode(type, ops, encodedContents);
      } else {
         return builder;
      }
   }

   @Override
   public <T> Stream<T> keys(DynamicOps<T> ops) {
      return Stream.concat(this.keyCodec.keys(ops), Stream.of(ops.createString("value")));
   }

   private static <K, V> DataResult<? extends MapEncoder<V>> getCodec(
      Function<? super V, ? extends DataResult<? extends K>> type,
      Function<? super K, ? extends DataResult<? extends MapEncoder<? extends V>>> encoder,
      V input
   ) {
      return type.apply(input).flatMap(key -> encoder.apply(key).map(Function.identity())).map(c -> (MapEncoder<V>)c);
   }

   @Override
   public String toString() {
      return "KeyDispatchCodec[" + this.keyCodec.toString() + " " + this.type + " " + this.decoder + "]";
   }
}
