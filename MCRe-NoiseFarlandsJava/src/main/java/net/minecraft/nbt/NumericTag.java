package net.minecraft.nbt;

import java.util.Optional;

public sealed interface NumericTag extends PrimitiveTag permits ByteTag, ShortTag, IntTag, LongTag, FloatTag, DoubleTag {
    byte byteValue();

    short shortValue();

    int intValue();

    long longValue();

    float floatValue();

    double doubleValue();

    Number box();

    @Override
    default Optional<Number> asNumber() {
        return Optional.of(this.box());
    }

    @Override
    default Optional<Byte> asByte() {
        return Optional.of(this.byteValue());
    }

    @Override
    default Optional<Short> asShort() {
        return Optional.of(this.shortValue());
    }

    @Override
    default Optional<Integer> asInt() {
        return Optional.of(this.intValue());
    }

    @Override
    default Optional<Long> asLong() {
        return Optional.of(this.longValue());
    }

    @Override
    default Optional<Float> asFloat() {
        return Optional.of(this.floatValue());
    }

    @Override
    default Optional<Double> asDouble() {
        return Optional.of(this.doubleValue());
    }

    @Override
    default Optional<Boolean> asBoolean() {
        return Optional.of(this.byteValue() != 0);
    }
}