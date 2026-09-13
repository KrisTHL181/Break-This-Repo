package com.mojang.serialization;

import com.mojang.datafixers.util.Pair;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public final class OptionalDynamic<T> extends DynamicLike<T> {
   private final DataResult<Dynamic<T>> delegate;

   public OptionalDynamic(DynamicOps<T> ops, DataResult<Dynamic<T>> delegate) {
      super(ops);
      this.delegate = delegate;
   }

   public DataResult<Dynamic<T>> get() {
      return this.delegate;
   }

   public Optional<Dynamic<T>> result() {
      return this.delegate.result();
   }

   public <U> DataResult<U> map(Function<? super Dynamic<T>, U> mapper) {
      return this.delegate.map(mapper);
   }

   public <U> DataResult<U> flatMap(Function<? super Dynamic<T>, ? extends DataResult<U>> mapper) {
      return this.delegate.flatMap(mapper);
   }

   @Override
   public DataResult<Number> asNumber() {
      return this.flatMap(DynamicLike::asNumber);
   }

   @Override
   public DataResult<String> asString() {
      return this.flatMap(DynamicLike::asString);
   }

   @Override
   public DataResult<Boolean> asBoolean() {
      return this.flatMap(DynamicLike::asBoolean);
   }

   @Override
   public DataResult<Stream<Dynamic<T>>> asStreamOpt() {
      return this.flatMap(DynamicLike::asStreamOpt);
   }

   @Override
   public DataResult<Stream<Pair<Dynamic<T>, Dynamic<T>>>> asMapOpt() {
      return this.flatMap(DynamicLike::asMapOpt);
   }

   @Override
   public DataResult<ByteBuffer> asByteBufferOpt() {
      return this.flatMap(DynamicLike::asByteBufferOpt);
   }

   @Override
   public DataResult<IntStream> asIntStreamOpt() {
      return this.flatMap(DynamicLike::asIntStreamOpt);
   }

   @Override
   public DataResult<LongStream> asLongStreamOpt() {
      return this.flatMap(DynamicLike::asLongStreamOpt);
   }

   @Override
   public OptionalDynamic<T> get(String key) {
      return new OptionalDynamic<>(this.ops, this.delegate.flatMap(k -> k.get(key).delegate));
   }

   @Override
   public DataResult<T> getGeneric(T key) {
      return this.flatMap(v -> v.getGeneric(key));
   }

   @Override
   public DataResult<T> getElement(String key) {
      return this.flatMap(v -> v.getElement(key));
   }

   @Override
   public DataResult<T> getElementGeneric(T key) {
      return this.flatMap(v -> v.getElementGeneric(key));
   }

   public Dynamic<T> orElseEmptyMap() {
      return this.result().orElseGet(this::emptyMap);
   }

   public Dynamic<T> orElseEmptyList() {
      return this.result().orElseGet(this::emptyList);
   }

   public <V> DataResult<V> into(Function<? super Dynamic<T>, ? extends V> action) {
      return this.delegate.map(action);
   }

   @Override
   public <A> DataResult<Pair<A, T>> decode(Decoder<? extends A> decoder) {
      return this.delegate.flatMap(t -> t.decode(decoder));
   }
}
