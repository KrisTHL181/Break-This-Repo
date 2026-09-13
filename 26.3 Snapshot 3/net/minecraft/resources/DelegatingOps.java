package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public abstract class DelegatingOps<T> implements DynamicOps<T> {
   protected final DynamicOps<T> delegate;

   protected DelegatingOps(final DynamicOps<T> delegate) {
      this.delegate = delegate;
   }

   public T empty() {
      return (T)this.delegate.empty();
   }

   public T emptyMap() {
      return (T)this.delegate.emptyMap();
   }

   public T emptyList() {
      return (T)this.delegate.emptyList();
   }

   public <U> U convertTo(final DynamicOps<U> outOps, final T input) {
      return (U)(Objects.equals(outOps, this.delegate) ? input : this.delegate.convertTo(outOps, input));
   }

   public DataResult<Number> getNumberValue(final T input) {
      return this.delegate.getNumberValue(input);
   }

   public T createNumeric(final Number i) {
      return (T)this.delegate.createNumeric(i);
   }

   public T createByte(final byte value) {
      return (T)this.delegate.createByte(value);
   }

   public T createShort(final short value) {
      return (T)this.delegate.createShort(value);
   }

   public T createInt(final int value) {
      return (T)this.delegate.createInt(value);
   }

   public T createLong(final long value) {
      return (T)this.delegate.createLong(value);
   }

   public T createFloat(final float value) {
      return (T)this.delegate.createFloat(value);
   }

   public T createDouble(final double value) {
      return (T)this.delegate.createDouble(value);
   }

   public DataResult<Boolean> getBooleanValue(final T input) {
      return this.delegate.getBooleanValue(input);
   }

   public T createBoolean(final boolean value) {
      return (T)this.delegate.createBoolean(value);
   }

   public DataResult<String> getStringValue(final T input) {
      return this.delegate.getStringValue(input);
   }

   public T createString(final String value) {
      return (T)this.delegate.createString(value);
   }

   public DataResult<T> mergeToList(final T list, final T value) {
      return this.delegate.mergeToList(list, value);
   }

   public DataResult<T> mergeToList(final T list, final List<T> values) {
      return this.delegate.mergeToList(list, values);
   }

   public DataResult<T> mergeToMap(final T map, final T key, final T value) {
      return this.delegate.mergeToMap(map, key, value);
   }

   public DataResult<T> mergeToMap(final T map, final MapLike<T> values) {
      return this.delegate.mergeToMap(map, values);
   }

   public DataResult<T> mergeToMap(final T map, final Map<T, T> values) {
      return this.delegate.mergeToMap(map, values);
   }

   public DataResult<T> mergeToPrimitive(final T prefix, final T value) {
      return this.delegate.mergeToPrimitive(prefix, value);
   }

   public DataResult<Stream<Pair<T, T>>> getMapValues(final T input) {
      return this.delegate.getMapValues(input);
   }

   public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(final T input) {
      return this.delegate.getMapEntries(input);
   }

   public T createMap(final Map<T, T> map) {
      return (T)this.delegate.createMap(map);
   }

   public T createMap(final Stream<Pair<T, T>> map) {
      return (T)this.delegate.createMap(map);
   }

   public DataResult<MapLike<T>> getMap(final T input) {
      return this.delegate.getMap(input);
   }

   public DataResult<Stream<T>> getStream(final T input) {
      return this.delegate.getStream(input);
   }

   public DataResult<Consumer<Consumer<T>>> getList(final T input) {
      return this.delegate.getList(input);
   }

   public T createList(final Stream<T> input) {
      return (T)this.delegate.createList(input);
   }

   public DataResult<ByteBuffer> getByteBuffer(final T input) {
      return this.delegate.getByteBuffer(input);
   }

   public T createByteList(final ByteBuffer input) {
      return (T)this.delegate.createByteList(input);
   }

   public DataResult<IntStream> getIntStream(final T input) {
      return this.delegate.getIntStream(input);
   }

   public T createIntList(final IntStream input) {
      return (T)this.delegate.createIntList(input);
   }

   public DataResult<LongStream> getLongStream(final T input) {
      return this.delegate.getLongStream(input);
   }

   public T createLongList(final LongStream input) {
      return (T)this.delegate.createLongList(input);
   }

   public T remove(final T input, final String key) {
      return (T)this.delegate.remove(input, key);
   }

   public boolean compressMaps() {
      return this.delegate.compressMaps();
   }

   public ListBuilder<T> listBuilder() {
      return new DelegatingOps.DelegateListBuilder(this.delegate.listBuilder());
   }

   public RecordBuilder<T> mapBuilder() {
      return new DelegatingOps.DelegateRecordBuilder(this.delegate.mapBuilder());
   }

   protected class DelegateListBuilder implements ListBuilder<T> {
      private final ListBuilder<T> original;

      protected DelegateListBuilder(final ListBuilder<T> original) {
         this.original = original;
      }

      public DynamicOps<T> ops() {
         return DelegatingOps.this;
      }

      public DataResult<T> build(final T prefix) {
         return this.original.build(prefix);
      }

      public ListBuilder<T> add(final T value) {
         this.original.add(value);
         return this;
      }

      public ListBuilder<T> add(final DataResult<T> value) {
         this.original.add(value);
         return this;
      }

      public <E> ListBuilder<T> add(final E value, final Encoder<E> encoder) {
         this.original.add(encoder.encodeStart(this.ops(), value));
         return this;
      }

      public <E> ListBuilder<T> addAll(final Iterable<E> values, final Encoder<E> encoder) {
         values.forEach(v -> this.original.add(encoder.encode(v, this.ops(), this.ops().empty())));
         return this;
      }

      public ListBuilder<T> withErrorsFrom(final DataResult<?> result) {
         this.original.withErrorsFrom(result);
         return this;
      }

      public ListBuilder<T> mapError(final UnaryOperator<String> onError) {
         this.original.mapError(onError);
         return this;
      }

      public DataResult<T> build(final DataResult<T> prefix) {
         return this.original.build(prefix);
      }
   }

   protected class DelegateRecordBuilder implements RecordBuilder<T> {
      private final RecordBuilder<T> original;

      protected DelegateRecordBuilder(final RecordBuilder<T> original) {
         this.original = original;
      }

      public DynamicOps<T> ops() {
         return DelegatingOps.this;
      }

      public RecordBuilder<T> add(final T key, final T value) {
         this.original.add(key, value);
         return this;
      }

      public RecordBuilder<T> add(final T key, final DataResult<T> value) {
         this.original.add(key, value);
         return this;
      }

      public RecordBuilder<T> add(final DataResult<T> key, final DataResult<T> value) {
         this.original.add(key, value);
         return this;
      }

      public RecordBuilder<T> add(final String key, final T value) {
         this.original.add(key, value);
         return this;
      }

      public RecordBuilder<T> add(final String key, final DataResult<T> value) {
         this.original.add(key, value);
         return this;
      }

      public <E> RecordBuilder<T> add(final String key, final E value, final Encoder<E> encoder) {
         return this.original.add(key, encoder.encodeStart(this.ops(), value));
      }

      public RecordBuilder<T> withErrorsFrom(final DataResult<?> result) {
         this.original.withErrorsFrom(result);
         return this;
      }

      public RecordBuilder<T> setLifecycle(final Lifecycle lifecycle) {
         this.original.setLifecycle(lifecycle);
         return this;
      }

      public RecordBuilder<T> mapError(final UnaryOperator<String> onError) {
         this.original.mapError(onError);
         return this;
      }

      public DataResult<T> build(final T prefix) {
         return this.original.build(prefix);
      }

      public DataResult<T> build(final DataResult<T> prefix) {
         return this.original.build(prefix);
      }
   }
}
