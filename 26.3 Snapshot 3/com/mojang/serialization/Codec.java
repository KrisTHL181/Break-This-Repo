package com.mojang.serialization;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.codecs.CompoundListCodec;
import com.mojang.serialization.codecs.DispatchedMapCodec;
import com.mojang.serialization.codecs.EitherCodec;
import com.mojang.serialization.codecs.EitherMapCodec;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.codecs.OptionalFieldCodec;
import com.mojang.serialization.codecs.PairCodec;
import com.mojang.serialization.codecs.PairMapCodec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.SimpleMapCodec;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import com.mojang.serialization.codecs.XorCodec;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public interface Codec<A> extends Encoder<A>, Decoder<A> {
   PrimitiveCodec<Boolean> BOOL = new PrimitiveCodec<Boolean>() {
      @Override
      public <T> DataResult<Boolean> read(DynamicOps<T> ops, T input) {
         return ops.getBooleanValue(input);
      }

      public <T> T write(DynamicOps<T> ops, Boolean value) {
         return ops.createBoolean(value);
      }

      @Override
      public String toString() {
         return "Bool";
      }
   };
   PrimitiveCodec<Byte> BYTE = new PrimitiveCodec<Byte>() {
      @Override
      public <T> DataResult<Byte> read(DynamicOps<T> ops, T input) {
         return ops.getNumberValue(input).map(Number::byteValue);
      }

      public <T> T write(DynamicOps<T> ops, Byte value) {
         return ops.createByte(value);
      }

      @Override
      public String toString() {
         return "Byte";
      }
   };
   PrimitiveCodec<Short> SHORT = new PrimitiveCodec<Short>() {
      @Override
      public <T> DataResult<Short> read(DynamicOps<T> ops, T input) {
         return ops.getNumberValue(input).map(Number::shortValue);
      }

      public <T> T write(DynamicOps<T> ops, Short value) {
         return ops.createShort(value);
      }

      @Override
      public String toString() {
         return "Short";
      }
   };
   PrimitiveCodec<Integer> INT = new PrimitiveCodec<Integer>() {
      @Override
      public <T> DataResult<Integer> read(DynamicOps<T> ops, T input) {
         return ops.getNumberValue(input).map(Number::intValue);
      }

      public <T> T write(DynamicOps<T> ops, Integer value) {
         return ops.createInt(value);
      }

      @Override
      public String toString() {
         return "Int";
      }
   };
   PrimitiveCodec<Long> LONG = new PrimitiveCodec<Long>() {
      @Override
      public <T> DataResult<Long> read(DynamicOps<T> ops, T input) {
         return ops.getNumberValue(input).map(Number::longValue);
      }

      public <T> T write(DynamicOps<T> ops, Long value) {
         return ops.createLong(value);
      }

      @Override
      public String toString() {
         return "Long";
      }
   };
   PrimitiveCodec<Float> FLOAT = new PrimitiveCodec<Float>() {
      @Override
      public <T> DataResult<Float> read(DynamicOps<T> ops, T input) {
         return ops.getNumberValue(input).map(Number::floatValue);
      }

      public <T> T write(DynamicOps<T> ops, Float value) {
         return ops.createFloat(value);
      }

      @Override
      public String toString() {
         return "Float";
      }
   };
   PrimitiveCodec<Double> DOUBLE = new PrimitiveCodec<Double>() {
      @Override
      public <T> DataResult<Double> read(DynamicOps<T> ops, T input) {
         return ops.getNumberValue(input).map(Number::doubleValue);
      }

      public <T> T write(DynamicOps<T> ops, Double value) {
         return ops.createDouble(value);
      }

      @Override
      public String toString() {
         return "Double";
      }
   };
   PrimitiveCodec<String> STRING = new PrimitiveCodec<String>() {
      @Override
      public <T> DataResult<String> read(DynamicOps<T> ops, T input) {
         return ops.getStringValue(input);
      }

      public <T> T write(DynamicOps<T> ops, String value) {
         return ops.createString(value);
      }

      @Override
      public String toString() {
         return "String";
      }
   };
   PrimitiveCodec<ByteBuffer> BYTE_BUFFER = new PrimitiveCodec<ByteBuffer>() {
      @Override
      public <T> DataResult<ByteBuffer> read(DynamicOps<T> ops, T input) {
         return ops.getByteBuffer(input);
      }

      public <T> T write(DynamicOps<T> ops, ByteBuffer value) {
         return ops.createByteList(value);
      }

      @Override
      public String toString() {
         return "ByteBuffer";
      }
   };
   PrimitiveCodec<IntStream> INT_STREAM = new PrimitiveCodec<IntStream>() {
      @Override
      public <T> DataResult<IntStream> read(DynamicOps<T> ops, T input) {
         return ops.getIntStream(input);
      }

      public <T> T write(DynamicOps<T> ops, IntStream value) {
         return ops.createIntList(value);
      }

      @Override
      public String toString() {
         return "IntStream";
      }
   };
   PrimitiveCodec<LongStream> LONG_STREAM = new PrimitiveCodec<LongStream>() {
      @Override
      public <T> DataResult<LongStream> read(DynamicOps<T> ops, T input) {
         return ops.getLongStream(input);
      }

      public <T> T write(DynamicOps<T> ops, LongStream value) {
         return ops.createLongList(value);
      }

      @Override
      public String toString() {
         return "LongStream";
      }
   };
   Codec<Dynamic<?>> PASSTHROUGH = new Codec<Dynamic<?>>() {
      @Override
      public <T> DataResult<Pair<Dynamic<?>, T>> decode(DynamicOps<T> ops, T input) {
         return DataResult.success(Pair.of(new Dynamic<>(ops, input), ops.empty()));
      }

      public <T> DataResult<T> encode(Dynamic<?> input, DynamicOps<T> ops, T prefix) {
         if (input.getValue() == input.getOps().empty()) {
            return DataResult.success(prefix, Lifecycle.experimental());
         }

         T casted = input.convert(ops).getValue();
         if (prefix == ops.empty()) {
            return DataResult.success(casted, Lifecycle.experimental());
         }

         DataResult<T> toMap = ops.getMap(casted).flatMap(map -> ops.mergeToMap(prefix, (MapLike<T>)map));
         return toMap.result()
            .map(DataResult::success)
            .orElseGet(
               () -> {
                  DataResult<T> toList = ops.getStream(casted).flatMap(stream -> ops.mergeToList(prefix, stream.collect(Collectors.toList())));
                  return toList.result()
                     .map(DataResult::success)
                     .orElseGet(() -> DataResult.error(() -> "Don't know how to merge " + prefix + " and " + casted, prefix, Lifecycle.experimental()));
               }
            );
      }

      @Override
      public String toString() {
         return "passthrough";
      }
   };
   MapCodec<Unit> EMPTY = MapCodec.unit(Unit.INSTANCE);

   default Codec<A> withLifecycle(final Lifecycle lifecycle) {
      return new Codec<A>() {
         @Override
         public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
            return Codec.this.encode(input, ops, prefix).setLifecycle(lifecycle);
         }

         @Override
         public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
            return Codec.this.decode(ops, input).setLifecycle(lifecycle);
         }

         @Override
         public String toString() {
            return Codec.this.toString();
         }
      };
   }

   default Codec<A> stable() {
      return this.withLifecycle(Lifecycle.stable());
   }

   default Codec<A> deprecated(int since) {
      return this.withLifecycle(Lifecycle.deprecated(since));
   }

   static <A> Codec<A> of(Encoder<A> encoder, Decoder<A> decoder) {
      return of(encoder, decoder, "Codec[" + encoder + " " + decoder + "]");
   }

   static <A> Codec<A> of(final Encoder<A> encoder, final Decoder<A> decoder, final String name) {
      return new Codec<A>() {
         @Override
         public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
            return decoder.decode(ops, input);
         }

         @Override
         public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
            return encoder.encode(input, ops, prefix);
         }

         @Override
         public String toString() {
            return name;
         }
      };
   }

   static <A> MapCodec<A> of(MapEncoder<A> encoder, MapDecoder<A> decoder) {
      return of(encoder, decoder, () -> "MapCodec[" + encoder + " " + decoder + "]");
   }

   static <A> MapCodec<A> of(final MapEncoder<A> encoder, final MapDecoder<A> decoder, final Supplier<String> name) {
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

   static <F, S> Codec<Pair<F, S>> pair(Codec<F> first, Codec<S> second) {
      return new PairCodec<>(first, second);
   }

   static <F, S> Codec<Either<F, S>> either(Codec<F> first, Codec<S> second) {
      return new EitherCodec<>(first, second);
   }

   static <F, S> Codec<Either<F, S>> xor(Codec<F> first, Codec<S> second) {
      return new XorCodec<>(first, second);
   }

   static <T> Codec<T> withAlternative(Codec<T> primary, Codec<? extends T> alternative) {
      return primary.withAlternative(alternative);
   }

   default Codec<A> withAlternative(Codec<? extends A> alternative) {
      return either(this, alternative).xmap(Either::unwrap, Either::left);
   }

   static <T, U> Codec<T> withAlternative(Codec<T> primary, Codec<U> alternative, Function<U, T> converter) {
      return primary.withAlternative(alternative, converter);
   }

   default <U> Codec<A> withAlternative(Codec<U> alternative, Function<U, A> converter) {
      return either(this, alternative).xmap(either -> either.map(v -> (A)v, converter), Either::left);
   }

   static <F, S> MapCodec<Pair<F, S>> mapPair(MapCodec<F> first, MapCodec<S> second) {
      return new PairMapCodec<>(first, second);
   }

   static <F, S> MapCodec<Either<F, S>> mapEither(MapCodec<F> first, MapCodec<S> second) {
      return new EitherMapCodec<>(first, second);
   }

   static <E> Codec<List<E>> list(Codec<E> elementCodec) {
      return list(elementCodec, 0, Integer.MAX_VALUE);
   }

   static <E> Codec<List<E>> list(Codec<E> elementCodec, int minSize, int maxSize) {
      return new ListCodec<>(elementCodec, minSize, maxSize);
   }

   static <K, V> Codec<List<Pair<K, V>>> compoundList(Codec<K> keyCodec, Codec<V> elementCodec) {
      return new CompoundListCodec<>(keyCodec, elementCodec);
   }

   static <K, V> SimpleMapCodec<K, V> simpleMap(Codec<K> keyCodec, Codec<V> elementCodec, Keyable keys) {
      return new SimpleMapCodec<>(keyCodec, elementCodec, keys);
   }

   static <K, V> UnboundedMapCodec<K, V> unboundedMap(Codec<K> keyCodec, Codec<V> elementCodec) {
      return new UnboundedMapCodec<>(keyCodec, elementCodec);
   }

   static <K, V> Codec<Map<K, V>> dispatchedMap(Codec<K> keyCodec, Function<K, Codec<? extends V>> valueCodecFunction) {
      return new DispatchedMapCodec<>(keyCodec, valueCodecFunction);
   }

   static <E> Codec<E> stringResolver(Function<E, String> toString, Function<String, E> fromString) {
      return STRING.flatXmap(
         name -> Optional.ofNullable(fromString.apply(name)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown element name:" + name)),
         e -> Optional.ofNullable(toString.apply((E)e)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Element with unknown name: " + e))
      );
   }

   static <F> MapCodec<Optional<F>> optionalField(String name, Codec<F> elementCodec, boolean lenient) {
      return new OptionalFieldCodec<>(name, elementCodec, lenient);
   }

   static <A> Codec<A> recursive(String name, Function<Codec<A>, Codec<A>> wrapped) {
      return new Codec.RecursiveCodec<>(name, wrapped);
   }

   static <A> Codec<A> lazyInitialized(Supplier<Codec<A>> delegate) {
      return new Codec.RecursiveCodec<>(delegate.toString(), self -> delegate.get());
   }

   default Codec<List<A>> listOf() {
      return list(this);
   }

   default Codec<List<A>> listOf(int minSize, int maxSize) {
      return list(this, minSize, maxSize);
   }

   default Codec<List<A>> sizeLimitedListOf(int maxSize) {
      return this.listOf(0, maxSize);
   }

   default <S> Codec<S> xmap(Function<? super A, ? extends S> to, Function<? super S, ? extends A> from) {
      return of(this.comap(from), this.map(to), this.toString() + "[xmapped]");
   }

   default <S> Codec<S> comapFlatMap(Function<? super A, ? extends DataResult<? extends S>> to, Function<? super S, ? extends A> from) {
      return of(this.comap(from), this.flatMap(to), this.toString() + "[comapFlatMapped]");
   }

   default <S> Codec<S> flatComapMap(Function<? super A, ? extends S> to, Function<? super S, ? extends DataResult<? extends A>> from) {
      return of(this.flatComap(from), this.map(to), this.toString() + "[flatComapMapped]");
   }

   default <S> Codec<S> flatXmap(Function<? super A, ? extends DataResult<? extends S>> to, Function<? super S, ? extends DataResult<? extends A>> from) {
      return of(this.flatComap(from), this.flatMap(to), this.toString() + "[flatXmapped]");
   }

   default MapCodec<A> fieldOf(String name) {
      return MapCodec.of(Encoder.super.fieldOf(name), Decoder.super.fieldOf(name), () -> "Field[" + name + ": " + this.toString() + "]");
   }

   default MapCodec<Optional<A>> optionalFieldOf(String name) {
      return optionalField(name, this, false);
   }

   default MapCodec<A> optionalFieldOf(String name, A defaultValue) {
      return this.optionalFieldOf(name, defaultValue, false);
   }

   default MapCodec<A> optionalFieldOf(String name, A defaultValue, Lifecycle lifecycleOfDefault) {
      return this.optionalFieldOf(name, Lifecycle.experimental(), defaultValue, lifecycleOfDefault);
   }

   default MapCodec<A> optionalFieldOf(String name, Lifecycle fieldLifecycle, A defaultValue, Lifecycle lifecycleOfDefault) {
      return this.optionalFieldOf(name, fieldLifecycle, defaultValue, lifecycleOfDefault, false);
   }

   default MapCodec<Optional<A>> lenientOptionalFieldOf(String name) {
      return optionalField(name, this, true);
   }

   default MapCodec<A> lenientOptionalFieldOf(String name, A defaultValue) {
      return this.optionalFieldOf(name, defaultValue, true);
   }

   default MapCodec<A> lenientOptionalFieldOf(String name, A defaultValue, Lifecycle lifecycleOfDefault) {
      return this.lenientOptionalFieldOf(name, Lifecycle.experimental(), defaultValue, lifecycleOfDefault);
   }

   default MapCodec<A> lenientOptionalFieldOf(String name, Lifecycle fieldLifecycle, A defaultValue, Lifecycle lifecycleOfDefault) {
      return this.optionalFieldOf(name, fieldLifecycle, defaultValue, lifecycleOfDefault, true);
   }

   private MapCodec<A> optionalFieldOf(String name, A defaultValue, boolean lenient) {
      return optionalField(name, this, lenient).xmap(o -> o.orElse(defaultValue), a -> Objects.equals(a, defaultValue) ? Optional.empty() : Optional.of((A)a));
   }

   private MapCodec<A> optionalFieldOf(String name, Lifecycle fieldLifecycle, A defaultValue, Lifecycle lifecycleOfDefault, boolean lenient) {
      return optionalField(name, this, lenient)
         .stable()
         .flatXmap(
            o -> o.<DataResult<? extends A>>map(v -> DataResult.success((A)v, fieldLifecycle)).orElse(DataResult.success(defaultValue, lifecycleOfDefault)),
            a -> Objects.equals(a, defaultValue)
               ? DataResult.success(Optional.empty(), lifecycleOfDefault)
               : DataResult.success(Optional.of((A)a), fieldLifecycle)
         );
   }

   default Codec<A> mapResult(final Codec.ResultFunction<A> function) {
      return new Codec<A>() {
         @Override
         public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
            return function.coApply(ops, input, Codec.this.encode(input, ops, prefix));
         }

         @Override
         public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
            return function.apply(ops, input, Codec.this.decode(ops, input));
         }

         @Override
         public String toString() {
            return Codec.this + "[mapResult " + function + "]";
         }
      };
   }

   default Codec<A> orElse(Consumer<String> onError, A value) {
      return this.orElse(DataFixUtils.consumerToFunction(onError), value);
   }

   default Codec<A> orElse(final UnaryOperator<String> onError, final A value) {
      return this.mapResult(new Codec.ResultFunction<A>() {
         @Override
         public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<A, T>> a) {
            return DataResult.success(a.mapError(onError).result().orElseGet(() -> Pair.of(value, input)));
         }

         @Override
         public <T> DataResult<T> coApply(DynamicOps<T> ops, A input, DataResult<T> t) {
            return t.mapError(onError);
         }

         @Override
         public String toString() {
            return "OrElse[" + onError + " " + value + "]";
         }
      });
   }

   default Codec<A> orElseGet(Consumer<String> onError, Supplier<? extends A> value) {
      return this.orElseGet(DataFixUtils.consumerToFunction(onError), value);
   }

   default Codec<A> orElseGet(final UnaryOperator<String> onError, final Supplier<? extends A> value) {
      return this.mapResult(new Codec.ResultFunction<A>() {
         @Override
         public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<A, T>> a) {
            return DataResult.success(a.mapError(onError).result().orElseGet(() -> Pair.of((A)value.get(), input)));
         }

         @Override
         public <T> DataResult<T> coApply(DynamicOps<T> ops, A input, DataResult<T> t) {
            return t.mapError(onError);
         }

         @Override
         public String toString() {
            return "OrElseGet[" + onError + " " + value.get() + "]";
         }
      });
   }

   default Codec<A> orElse(final A value) {
      return this.mapResult(new Codec.ResultFunction<A>() {
         @Override
         public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<A, T>> a) {
            return DataResult.success(a.result().orElseGet(() -> Pair.of(value, input)));
         }

         @Override
         public <T> DataResult<T> coApply(DynamicOps<T> ops, A input, DataResult<T> t) {
            return t;
         }

         @Override
         public String toString() {
            return "OrElse[" + value + "]";
         }
      });
   }

   default Codec<A> orElseGet(final Supplier<? extends A> value) {
      return this.mapResult(new Codec.ResultFunction<A>() {
         @Override
         public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<A, T>> a) {
            return DataResult.success(a.result().orElseGet(() -> Pair.of((A)value.get(), input)));
         }

         @Override
         public <T> DataResult<T> coApply(DynamicOps<T> ops, A input, DataResult<T> t) {
            return t;
         }

         @Override
         public String toString() {
            return "OrElseGet[" + value.get() + "]";
         }
      });
   }

   default Codec<A> promotePartial(Consumer<String> onError) {
      return of(this, Decoder.super.promotePartial(onError));
   }

   default <E> Codec<E> dispatch(Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
      return this.dispatch("type", type, codec);
   }

   default <E> Codec<E> dispatch(String typeKey, Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
      return this.fieldOf(typeKey).dispatch(type, codec);
   }

   default <E> Codec<E> dispatchStable(Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
      return this.fieldOf("type").dispatchStable(type, codec);
   }

   default <E> Codec<E> partialDispatch(
      String typeKey,
      Function<? super E, ? extends DataResult<? extends A>> type,
      Function<? super A, ? extends DataResult<? extends MapCodec<? extends E>>> codec
   ) {
      return this.fieldOf(typeKey).partialDispatch(type, codec);
   }

   default <E> MapCodec<E> dispatchMap(Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
      return this.dispatchMap("type", type, codec);
   }

   default <E> MapCodec<E> dispatchMap(String typeKey, Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
      return this.fieldOf(typeKey).dispatchMap(type, codec);
   }

   default Codec<A> validate(Function<A, DataResult<A>> checker) {
      return this.flatXmap(checker, checker);
   }

   static <N extends Number & Comparable<N>> Function<N, DataResult<N>> checkRange(N minInclusive, N maxInclusive) {
      return value -> value.compareTo(minInclusive) >= 0 && value.compareTo(maxInclusive) <= 0
         ? DataResult.success(value)
         : DataResult.error(() -> "Value " + value + " outside of range [" + minInclusive + ":" + maxInclusive + "]");
   }

   static Codec<Integer> intRange(int minInclusive, int maxInclusive) {
      Function<Integer, DataResult<Integer>> checker = checkRange(minInclusive, maxInclusive);
      return INT.flatXmap(checker, checker);
   }

   static Codec<Float> floatRange(float minInclusive, float maxInclusive) {
      Function<Float, DataResult<Float>> checker = checkRange(minInclusive, maxInclusive);
      return FLOAT.flatXmap(checker, checker);
   }

   static Codec<Double> doubleRange(double minInclusive, double maxInclusive) {
      Function<Double, DataResult<Double>> checker = checkRange(minInclusive, maxInclusive);
      return DOUBLE.flatXmap(checker, checker);
   }

   static Codec<String> string(int minSize, int maxSize) {
      return STRING.validate(
         value -> {
            int length = value.length();
            if (length < minSize) {
               return DataResult.error(() -> "String \"" + value + "\" is too short: " + length + ", expected range [" + minSize + "-" + maxSize + "]");
            } else {
               return length > maxSize
                  ? DataResult.error(() -> "String \"" + value + "\" is too long: " + length + ", expected range [" + minSize + "-" + maxSize + "]")
                  : DataResult.success(value);
            }
         }
      );
   }

   static Codec<String> sizeLimitedString(int maxSize) {
      return string(0, maxSize);
   }

   class RecursiveCodec<T> implements Codec<T> {
      private final String name;
      private final Supplier<Codec<T>> wrapped;

      private RecursiveCodec(String name, Function<Codec<T>, Codec<T>> wrapped) {
         this.name = name;
         this.wrapped = Suppliers.memoize(() -> wrapped.apply(this));
      }

      @Override
      public <S> DataResult<Pair<T, S>> decode(DynamicOps<S> ops, S input) {
         return this.wrapped.get().decode(ops, input);
      }

      @Override
      public <S> DataResult<S> encode(T input, DynamicOps<S> ops, S prefix) {
         return this.wrapped.get().encode(input, ops, prefix);
      }

      @Override
      public String toString() {
         return "RecursiveCodec[" + this.name + "]";
      }
   }

   interface ResultFunction<A> {
      <T> DataResult<Pair<A, T>> apply(DynamicOps<T> var1, T var2, DataResult<Pair<A, T>> var3);

      <T> DataResult<T> coApply(DynamicOps<T> var1, A var2, DataResult<T> var3);
   }
}
