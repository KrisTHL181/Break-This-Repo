package com.mojang.serialization;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.codecs.KeyDispatchCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public abstract class MapCodec<A> extends CompressorHolder implements MapDecoder<A>, MapEncoder<A> {
   public static <A> MapCodec<A> assumeMapUnsafe(final Codec<A> codec) {
      return new MapCodec<A>() {
         private static final String COMPRESSED_VALUE_KEY = "value";

         @Override
         public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString("value"));
         }

         @Override
         public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
            if (ops.compressMaps()) {
               T value = input.get("value");
               return value == null ? DataResult.error(() -> "Missing value") : codec.parse(ops, value);
            } else {
               return codec.parse(ops, ops.createMap(input.entries()));
            }
         }

         @Override
         public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            DataResult<T> encoded = codec.encodeStart(ops, input);
            if (ops.compressMaps()) {
               return prefix.add("value", encoded);
            }

            DataResult<MapLike<T>> encodedMapResult = encoded.flatMap(ops::getMap);
            return encodedMapResult.map(encodedMap -> {
               encodedMap.entries().forEach(pair -> prefix.add(pair.getFirst(), pair.getSecond()));
               return prefix;
            }).result().orElseGet(() -> prefix.withErrorsFrom(encodedMapResult));
         }
      };
   }

   public final <O> RecordCodecBuilder<O, A> forGetter(Function<O, A> getter) {
      return RecordCodecBuilder.of(getter, this);
   }

   public static <A> MapCodec<A> of(MapEncoder<A> encoder, MapDecoder<A> decoder) {
      return of(encoder, decoder, () -> "MapCodec[" + encoder + " " + decoder + "]");
   }

   public static <A> MapCodec<A> of(final MapEncoder<A> encoder, final MapDecoder<A> decoder, final Supplier<String> name) {
      return new MapCodec<A>() {
         @Override
         public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.concat(encoder.keys(ops), decoder.keys(ops));
         }

         @Override
         public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
            return decoder.decode(ops, input);
         }

         @Override
         public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return encoder.encode(input, ops, prefix);
         }

         @Override
         public String toString() {
            return name.get();
         }
      };
   }

   public static <A> MapCodec<A> recursive(String name, Function<Codec<A>, MapCodec<A>> wrapped) {
      return new MapCodec.RecursiveMapCodec<>(name, wrapped);
   }

   public MapCodec<A> fieldOf(String name) {
      return this.codec().fieldOf(name);
   }

   public MapCodec<A> withLifecycle(final Lifecycle lifecycle) {
      return new MapCodec<A>() {
         @Override
         public <T> Stream<T> keys(DynamicOps<T> ops) {
            return MapCodec.this.keys(ops);
         }

         @Override
         public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
            return MapCodec.this.decode(ops, input).setLifecycle(lifecycle);
         }

         @Override
         public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return MapCodec.this.encode(input, ops, prefix).setLifecycle(lifecycle);
         }

         @Override
         public String toString() {
            return MapCodec.this.toString();
         }
      };
   }

   public Codec<A> codec() {
      return new MapCodec.MapCodecCodec<>(this);
   }

   public MapCodec<A> stable() {
      return this.withLifecycle(Lifecycle.stable());
   }

   public MapCodec<A> deprecated(int since) {
      return this.withLifecycle(Lifecycle.deprecated(since));
   }

   public <S> MapCodec<S> xmap(Function<? super A, ? extends S> to, Function<? super S, ? extends A> from) {
      return of(this.comap(from), this.map(to), () -> this.toString() + "[xmapped]");
   }

   public <S> MapCodec<S> flatXmap(Function<? super A, ? extends DataResult<? extends S>> to, Function<? super S, ? extends DataResult<? extends A>> from) {
      return Codec.of(this.flatComap(from), this.flatMap(to), () -> this.toString() + "[flatXmapped]");
   }

   public MapCodec<A> validate(Function<A, DataResult<A>> checker) {
      return this.flatXmap(checker, checker);
   }

   public <E> MapCodec<A> dependent(MapCodec<E> initialInstance, Function<A, Pair<E, MapCodec<E>>> splitter, BiFunction<A, E, A> combiner) {
      return new MapCodec.Dependent<>(this, initialInstance, splitter, combiner);
   }

   public <E> Codec<E> dispatch(Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
      return this.partialDispatch(type.andThen(DataResult::success), codec.andThen(DataResult::success));
   }

   public <E> Codec<E> dispatchStable(Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
      return this.partialDispatch(e -> DataResult.success(type.apply(e), Lifecycle.stable()), a -> DataResult.success(codec.apply(a), Lifecycle.stable()));
   }

   public <E> Codec<E> partialDispatch(
      Function<? super E, ? extends DataResult<? extends A>> type, Function<? super A, ? extends DataResult<? extends MapCodec<? extends E>>> codec
   ) {
      return new KeyDispatchCodec<>(this, type, codec).codec();
   }

   public <E> MapCodec<E> dispatchMap(Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
      return new KeyDispatchCodec<>(this, type.andThen(DataResult::success), codec.andThen(DataResult::success));
   }

   @Override
   public abstract <T> Stream<T> keys(DynamicOps<T> var1);

   public MapCodec<A> mapResult(final MapCodec.ResultFunction<A> function) {
      return new MapCodec<A>() {
         @Override
         public <T> Stream<T> keys(DynamicOps<T> ops) {
            return MapCodec.this.keys(ops);
         }

         @Override
         public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return function.coApply(ops, input, MapCodec.this.encode(input, ops, prefix));
         }

         @Override
         public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
            return function.apply(ops, input, MapCodec.this.decode(ops, input));
         }

         @Override
         public String toString() {
            return MapCodec.this + "[mapResult " + function + "]";
         }
      };
   }

   public MapCodec<A> orElse(Consumer<String> onError, A value) {
      return this.orElse(DataFixUtils.consumerToFunction(onError), value);
   }

   public MapCodec<A> orElse(final UnaryOperator<String> onError, final A value) {
      return this.mapResult(new MapCodec.ResultFunction<A>() {
         @Override
         public <T> DataResult<A> apply(DynamicOps<T> ops, MapLike<T> input, DataResult<A> a) {
            return DataResult.success(a.mapError(onError).result().orElse(value));
         }

         @Override
         public <T> RecordBuilder<T> coApply(DynamicOps<T> ops, A input, RecordBuilder<T> t) {
            return t.mapError(onError);
         }

         @Override
         public String toString() {
            return "OrElse[" + onError + " " + value + "]";
         }
      });
   }

   public MapCodec<A> orElseGet(Consumer<String> onError, Supplier<? extends A> value) {
      return this.orElseGet(DataFixUtils.consumerToFunction(onError), value);
   }

   public MapCodec<A> orElseGet(final UnaryOperator<String> onError, final Supplier<? extends A> value) {
      return this.mapResult(new MapCodec.ResultFunction<A>() {
         @Override
         public <T> DataResult<A> apply(DynamicOps<T> ops, MapLike<T> input, DataResult<A> a) {
            return DataResult.success(a.mapError(onError).result().orElseGet(value));
         }

         @Override
         public <T> RecordBuilder<T> coApply(DynamicOps<T> ops, A input, RecordBuilder<T> t) {
            return t.mapError(onError);
         }

         @Override
         public String toString() {
            return "OrElseGet[" + onError + " " + value.get() + "]";
         }
      });
   }

   public MapCodec<A> orElse(final A value) {
      return this.mapResult(new MapCodec.ResultFunction<A>() {
         @Override
         public <T> DataResult<A> apply(DynamicOps<T> ops, MapLike<T> input, DataResult<A> a) {
            return DataResult.success(a.result().orElse(value));
         }

         @Override
         public <T> RecordBuilder<T> coApply(DynamicOps<T> ops, A input, RecordBuilder<T> t) {
            return t;
         }

         @Override
         public String toString() {
            return "OrElse[" + value + "]";
         }
      });
   }

   public MapCodec<A> orElseGet(final Supplier<? extends A> value) {
      return this.mapResult(new MapCodec.ResultFunction<A>() {
         @Override
         public <T> DataResult<A> apply(DynamicOps<T> ops, MapLike<T> input, DataResult<A> a) {
            return DataResult.success(a.result().orElseGet(value));
         }

         @Override
         public <T> RecordBuilder<T> coApply(DynamicOps<T> ops, A input, RecordBuilder<T> t) {
            return t;
         }

         @Override
         public String toString() {
            return "OrElseGet[" + value.get() + "]";
         }
      });
   }

   public MapCodec<A> setPartial(final Supplier<A> value) {
      return this.mapResult(new MapCodec.ResultFunction<A>() {
         @Override
         public <T> DataResult<A> apply(DynamicOps<T> ops, MapLike<T> input, DataResult<A> a) {
            return a.setPartial(value);
         }

         @Override
         public <T> RecordBuilder<T> coApply(DynamicOps<T> ops, A input, RecordBuilder<T> t) {
            return t;
         }

         @Override
         public String toString() {
            return "SetPartial[" + value + "]";
         }
      });
   }

   public static <A> MapCodec<A> unit(A defaultValue) {
      return unit(() -> defaultValue);
   }

   public static <A> MapCodec<A> unit(final Supplier<A> value) {
      return new MapCodec<A>() {
         @Override
         public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
         }

         @Override
         public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
            return DataResult.success(value.get());
         }

         @Override
         public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return prefix;
         }

         @Override
         public Codec<A> codec() {
            return unitCodec(value);
         }

         @Override
         public String toString() {
            return "Unit[" + value.get() + "]";
         }
      };
   }

   public static <A> Codec<A> unitCodec(A value) {
      return unitCodec(() -> value);
   }

   public static <A> Codec<A> unitCodec(final Supplier<A> value) {
      return new Codec<A>() {
         @Override
         public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
            DataResult<?> check = ops.compressMaps() ? ops.getList(input) : ops.getMap(input);
            return check.map(ignore -> Pair.of(value.get(), input));
         }

         @Override
         public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
            return ops.mergeToMap(prefix, MapLike.empty());
         }

         @Override
         public String toString() {
            return "Unit[" + value.get() + "]";
         }
      };
   }

   private static class Dependent<O, E> extends MapCodec<O> {
      private final MapCodec<E> initialInstance;
      private final Function<O, Pair<E, MapCodec<E>>> splitter;
      private final MapCodec<O> codec;
      private final BiFunction<O, E, O> combiner;

      public Dependent(MapCodec<O> codec, MapCodec<E> initialInstance, Function<O, Pair<E, MapCodec<E>>> splitter, BiFunction<O, E, O> combiner) {
         this.initialInstance = initialInstance;
         this.splitter = splitter;
         this.codec = codec;
         this.combiner = combiner;
      }

      @Override
      public <T> Stream<T> keys(DynamicOps<T> ops) {
         return Stream.concat(this.codec.keys(ops), this.initialInstance.keys(ops));
      }

      // ===== 修改：去掉多余的强制转换，修复类型推断 =====
      @Override
      public <T> DataResult<O> decode(DynamicOps<T> ops, MapLike<T> input) {
         return this.codec
            .decode(ops, input)
            .flatMap(
               base -> this.splitter
                  .apply(base)
                  .getSecond()
                  .decode(ops, input)
                  .map(e -> this.combiner.apply(base, e))
                  .setLifecycle(Lifecycle.experimental())
            );
      }

      @Override
      public <T> RecordBuilder<T> encode(O input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
         this.codec.encode(input, ops, prefix);
         Pair<E, MapCodec<E>> e = this.splitter.apply(input);
         e.getSecond().encode(e.getFirst(), ops, prefix);
         return prefix.setLifecycle(Lifecycle.experimental());
      }
   }

   public record MapCodecCodec<A>(MapCodec<A> codec) implements Codec<A> {
      @Override
      public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
         return this.codec.compressedDecode(ops, input).map(r -> Pair.of((A)r, input));
      }

      @Override
      public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
         return this.codec.encode(input, ops, this.codec.compressedBuilder(ops)).build(prefix);
      }

      @Override
      public String toString() {
         return this.codec.toString();
      }
   }

   private static class RecursiveMapCodec<A> extends MapCodec<A> {
      private final String name;
      private final Supplier<MapCodec<A>> wrapped;

      private RecursiveMapCodec(String name, Function<Codec<A>, MapCodec<A>> wrapped) {
         this.name = name;
         this.wrapped = Suppliers.memoize(() -> wrapped.apply(this.codec()));
      }

      @Override
      public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
         return this.wrapped.get().encode(input, ops, prefix);
      }

      @Override
      public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
         return this.wrapped.get().decode(ops, input);
      }

      @Override
      public <T> Stream<T> keys(DynamicOps<T> ops) {
         return this.wrapped.get().keys(ops);
      }

      @Override
      public String toString() {
         return "RecursiveMapCodec[" + this.name + "]";
      }
   }

   public interface ResultFunction<A> {
      <T> DataResult<A> apply(DynamicOps<T> var1, MapLike<T> var2, DataResult<A> var3);

      <T> RecordBuilder<T> coApply(DynamicOps<T> var1, A var2, RecordBuilder<T> var3);
   }
}