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

    @Override
    public T empty() {
        return this.delegate.empty();
    }

    @Override
    public T emptyMap() {
        return this.delegate.emptyMap();
    }

    @Override
    public T emptyList() {
        return this.delegate.emptyList();
    }

    @Override
    public <U> U convertTo(final DynamicOps<U> outOps, final T input) {
        return (U)(Objects.equals(outOps, this.delegate) ? input : this.delegate.convertTo(outOps, input));
    }

    @Override
    public DataResult<Number> getNumberValue(final T input) {
        return this.delegate.getNumberValue(input);
    }

    @Override
    public T createNumeric(final Number i) {
        return this.delegate.createNumeric(i);
    }

    @Override
    public T createByte(final byte value) {
        return this.delegate.createByte(value);
    }

    @Override
    public T createShort(final short value) {
        return this.delegate.createShort(value);
    }

    @Override
    public T createInt(final int value) {
        return this.delegate.createInt(value);
    }

    @Override
    public T createLong(final long value) {
        return this.delegate.createLong(value);
    }

    @Override
    public T createFloat(final float value) {
        return this.delegate.createFloat(value);
    }

    @Override
    public T createDouble(final double value) {
        return this.delegate.createDouble(value);
    }

    @Override
    public DataResult<Boolean> getBooleanValue(final T input) {
        return this.delegate.getBooleanValue(input);
    }

    @Override
    public T createBoolean(final boolean value) {
        return this.delegate.createBoolean(value);
    }

    @Override
    public DataResult<String> getStringValue(final T input) {
        return this.delegate.getStringValue(input);
    }

    @Override
    public T createString(final String value) {
        return this.delegate.createString(value);
    }

    @Override
    public DataResult<T> mergeToList(final T list, final T value) {
        return this.delegate.mergeToList(list, value);
    }

    @Override
    public DataResult<T> mergeToList(final T list, final List<T> values) {
        return this.delegate.mergeToList(list, values);
    }

    @Override
    public DataResult<T> mergeToMap(final T map, final T key, final T value) {
        return this.delegate.mergeToMap(map, key, value);
    }

    @Override
    public DataResult<T> mergeToMap(final T map, final MapLike<T> values) {
        return this.delegate.mergeToMap(map, values);
    }

    @Override
    public DataResult<T> mergeToMap(final T map, final Map<T, T> values) {
        return this.delegate.mergeToMap(map, values);
    }

    @Override
    public DataResult<T> mergeToPrimitive(final T prefix, final T value) {
        return this.delegate.mergeToPrimitive(prefix, value);
    }

    @Override
    public DataResult<Stream<Pair<T, T>>> getMapValues(final T input) {
        return this.delegate.getMapValues(input);
    }

    @Override
    public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(final T input) {
        return this.delegate.getMapEntries(input);
    }

    @Override
    public T createMap(final Map<T, T> map) {
        return this.delegate.createMap(map);
    }

    @Override
    public T createMap(final Stream<Pair<T, T>> map) {
        return this.delegate.createMap(map);
    }

    @Override
    public DataResult<MapLike<T>> getMap(final T input) {
        return this.delegate.getMap(input);
    }

    @Override
    public DataResult<Stream<T>> getStream(final T input) {
        return this.delegate.getStream(input);
    }

    @Override
    public DataResult<Consumer<Consumer<T>>> getList(final T input) {
        return this.delegate.getList(input);
    }

    @Override
    public T createList(final Stream<T> input) {
        return this.delegate.createList(input);
    }

    @Override
    public DataResult<ByteBuffer> getByteBuffer(final T input) {
        return this.delegate.getByteBuffer(input);
    }

    @Override
    public T createByteList(final ByteBuffer input) {
        return this.delegate.createByteList(input);
    }

    @Override
    public DataResult<IntStream> getIntStream(final T input) {
        return this.delegate.getIntStream(input);
    }

    @Override
    public T createIntList(final IntStream input) {
        return this.delegate.createIntList(input);
    }

    @Override
    public DataResult<LongStream> getLongStream(final T input) {
        return this.delegate.getLongStream(input);
    }

    @Override
    public T createLongList(final LongStream input) {
        return this.delegate.createLongList(input);
    }

    @Override
    public T remove(final T input, final String key) {
        return this.delegate.remove(input, key);
    }

    @Override
    public boolean compressMaps() {
        return this.delegate.compressMaps();
    }

    @Override
    public ListBuilder<T> listBuilder() {
        return new DelegatingOps.DelegateListBuilder(this.delegate.listBuilder());
    }

    @Override
    public RecordBuilder<T> mapBuilder() {
        return new DelegatingOps.DelegateRecordBuilder(this.delegate.mapBuilder());
    }

    protected class DelegateListBuilder implements ListBuilder<T> {
        private final ListBuilder<T> original;

        protected DelegateListBuilder(final ListBuilder<T> original) {
            this.original = original;
        }

        @Override
        public DynamicOps<T> ops() {
            return DelegatingOps.this;
        }

        @Override
        public DataResult<T> build(final T prefix) {
            return this.original.build(prefix);
        }

        @Override
        public ListBuilder<T> add(final T value) {
            this.original.add(value);
            return this;
        }

        @Override
        public ListBuilder<T> add(final DataResult<T> value) {
            this.original.add(value);
            return this;
        }

        @Override
        public <E> ListBuilder<T> add(final E value, final Encoder<E> encoder) {
            this.original.add(encoder.encodeStart(this.ops(), value));
            return this;
        }

        @Override
        public <E> ListBuilder<T> addAll(final Iterable<E> values, final Encoder<E> encoder) {
            values.forEach(v -> this.original.add(encoder.encode((E)v, this.ops(), (T)this.ops().empty())));
            return this;
        }

        @Override
        public ListBuilder<T> withErrorsFrom(final DataResult<?> result) {
            this.original.withErrorsFrom(result);
            return this;
        }

        @Override
        public ListBuilder<T> mapError(final UnaryOperator<String> onError) {
            this.original.mapError(onError);
            return this;
        }

        @Override
        public DataResult<T> build(final DataResult<T> prefix) {
            return this.original.build(prefix);
        }
    }

    protected class DelegateRecordBuilder implements RecordBuilder<T> {
        private final RecordBuilder<T> original;

        protected DelegateRecordBuilder(final RecordBuilder<T> original) {
            this.original = original;
        }

        @Override
        public DynamicOps<T> ops() {
            return DelegatingOps.this;
        }

        @Override
        public RecordBuilder<T> add(final T key, final T value) {
            this.original.add(key, value);
            return this;
        }

        @Override
        public RecordBuilder<T> add(final T key, final DataResult<T> value) {
            this.original.add(key, value);
            return this;
        }

        @Override
        public RecordBuilder<T> add(final DataResult<T> key, final DataResult<T> value) {
            this.original.add(key, value);
            return this;
        }

        @Override
        public RecordBuilder<T> add(final String key, final T value) {
            this.original.add(key, value);
            return this;
        }

        @Override
        public RecordBuilder<T> add(final String key, final DataResult<T> value) {
            this.original.add(key, value);
            return this;
        }

        @Override
        public <E> RecordBuilder<T> add(final String key, final E value, final Encoder<E> encoder) {
            return this.original.add(key, encoder.encodeStart(this.ops(), value));
        }

        @Override
        public RecordBuilder<T> withErrorsFrom(final DataResult<?> result) {
            this.original.withErrorsFrom(result);
            return this;
        }

        @Override
        public RecordBuilder<T> setLifecycle(final Lifecycle lifecycle) {
            this.original.setLifecycle(lifecycle);
            return this;
        }

        @Override
        public RecordBuilder<T> mapError(final UnaryOperator<String> onError) {
            this.original.mapError(onError);
            return this;
        }

        @Override
        public DataResult<T> build(final T prefix) {
            return this.original.build(prefix);
        }

        @Override
        public DataResult<T> build(final DataResult<T> prefix) {
            return this.original.build(prefix);
        }
    }
}