package net.minecraft.util;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import java.util.function.UnaryOperator;

public abstract class AbstractListBuilder<T, B> implements ListBuilder<T> {
    private final DynamicOps<T> ops;
    protected DataResult<B> builder = DataResult.success(this.initBuilder(), Lifecycle.stable());

    protected AbstractListBuilder(final DynamicOps<T> ops) {
        this.ops = ops;
    }

    @Override
    public DynamicOps<T> ops() {
        return this.ops;
    }

    protected abstract B initBuilder();

    protected abstract B append(B builder, T value);

    protected abstract DataResult<T> build(B builder, T prefix);

    @Override
    public ListBuilder<T> add(final T value) {
        this.builder = this.builder.map(b -> this.append((B)b, value));
        return this;
    }

    @Override
    public ListBuilder<T> add(final DataResult<T> value) {
        this.builder = this.builder.apply2stable(this::append, value);
        return this;
    }

    @Override
    public ListBuilder<T> withErrorsFrom(final DataResult<?> result) {
        this.builder = this.builder.flatMap(r -> result.map(v -> r));
        return this;
    }

    @Override
    public ListBuilder<T> mapError(final UnaryOperator<String> onError) {
        this.builder = this.builder.mapError(onError);
        return this;
    }

    @Override
    public DataResult<T> build(final T prefix) {
        DataResult<T> result = this.builder.flatMap(b -> this.build((B)b, prefix));
        this.builder = DataResult.success(this.initBuilder(), Lifecycle.stable());
        return result;
    }
}
