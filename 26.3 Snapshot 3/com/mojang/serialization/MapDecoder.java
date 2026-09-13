package com.mojang.serialization;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public interface MapDecoder<A> extends Keyable {
   <T> DataResult<A> decode(DynamicOps<T> var1, MapLike<T> var2);

   default <T> DataResult<A> compressedDecode(DynamicOps<T> ops, T input) {
      if (ops.compressMaps()) {
         Optional<Consumer<Consumer<T>>> inputList = ops.getList(input).result();
         if (!inputList.isPresent()) {
            return DataResult.error(() -> "Input is not a list");
         }

         final KeyCompressor<T> compressor = this.compressor(ops);
         final List<T> entries = new ArrayList<>();
         inputList.get().accept(entries::add);
         MapLike<T> map = new MapLike<T>() {
            @Nullable
            @Override
            public T get(T key) {
               return entries.get(compressor.compress(key));
            }

            @Nullable
            @Override
            public T get(String key) {
               return entries.get(compressor.compress(key));
            }

            @Override
            public Stream<Pair<T, T>> entries() {
               return IntStream.range(0, entries.size()).mapToObj(i -> Pair.of(compressor.decompress(i), entries.get(i))).filter(p -> p.getSecond() != null);
            }
         };
         return this.decode(ops, map);
      } else {
         return ops.getMap(input).setLifecycle(Lifecycle.stable()).flatMap(mapx -> this.decode(ops, mapx));
      }
   }

   <T> KeyCompressor<T> compressor(DynamicOps<T> var1);

   default Decoder<A> decoder() {
      return new Decoder<A>() {
         @Override
         public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
            return MapDecoder.this.compressedDecode(ops, input).map(r -> Pair.of((A)r, input));
         }

         @Override
         public String toString() {
            return MapDecoder.this.toString();
         }
      };
   }

   default <B> MapDecoder<B> flatMap(final Function<? super A, ? extends DataResult<? extends B>> function) {
      return new MapDecoder.Implementation<B>() {
         @Override
         public <T> Stream<T> keys(DynamicOps<T> ops) {
            return MapDecoder.this.keys(ops);
         }

         @Override
         public <T> DataResult<B> decode(DynamicOps<T> ops, MapLike<T> input) {
            return MapDecoder.this.decode(ops, input).flatMap(b -> function.apply((A)b).map(Function.identity()));
         }

         @Override
         public String toString() {
            return MapDecoder.this.toString() + "[flatMapped]";
         }
      };
   }

   default <B> MapDecoder<B> map(final Function<? super A, ? extends B> function) {
      return new MapDecoder.Implementation<B>() {
         @Override
         public <T> DataResult<B> decode(DynamicOps<T> ops, MapLike<T> input) {
            return MapDecoder.this.decode(ops, input).map(function);
         }

         @Override
         public <T> Stream<T> keys(DynamicOps<T> ops) {
            return MapDecoder.this.keys(ops);
         }

         @Override
         public String toString() {
            return MapDecoder.this.toString() + "[mapped]";
         }
      };
   }

   // ===== 修改：去掉多余的强制转换 =====
   default <E> MapDecoder<E> ap(final MapDecoder<Function<? super A, ? extends E>> decoder) {
      return new MapDecoder.Implementation<E>() {
         @Override
         public <T> DataResult<E> decode(DynamicOps<T> ops, MapLike<T> input) {
            return MapDecoder.this.decode(ops, input)
               .flatMap(f -> decoder.decode(ops, input)
                  .map(e -> e.apply(f))
               );
         }

         @Override
         public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.concat(MapDecoder.this.keys(ops), decoder.keys(ops));
         }

         @Override
         public String toString() {
            return decoder.toString() + " * " + MapDecoder.this.toString();
         }
      };
   }

   default MapDecoder<A> withLifecycle(final Lifecycle lifecycle) {
      return new MapDecoder.Implementation<A>() {
         @Override
         public <T> Stream<T> keys(DynamicOps<T> ops) {
            return MapDecoder.this.keys(ops);
         }

         @Override
         public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
            return MapDecoder.this.decode(ops, input).setLifecycle(lifecycle);
         }

         @Override
         public String toString() {
            return MapDecoder.this.toString();
         }
      };
   }

   abstract class Implementation<A> extends CompressorHolder implements MapDecoder<A> {
   }
}