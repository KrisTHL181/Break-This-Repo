package net.minecraft.world.attribute;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import org.jspecify.annotations.Nullable;

public final class EnvironmentAttributeMap {
    public static final EnvironmentAttributeMap EMPTY = new EnvironmentAttributeMap(Map.of());
    public static final Codec<EnvironmentAttributeMap> CODEC = Codec.lazyInitialized(
        () -> Codec.dispatchedMap(EnvironmentAttributes.CODEC, Util.memoize(EnvironmentAttributeMap.Entry::createCodec))
            .xmap((java.util.function.Function<Map, EnvironmentAttributeMap>)EnvironmentAttributeMap::new, v -> (Map)((EnvironmentAttributeMap)v).entries)
    );
    public static final Codec<EnvironmentAttributeMap> NETWORK_CODEC = CODEC.xmap(
        EnvironmentAttributeMap::filterSyncable, EnvironmentAttributeMap::filterSyncable
    );
    public static final Codec<EnvironmentAttributeMap> CODEC_ONLY_POSITIONAL = CODEC.validate(
        map -> {
            List<EnvironmentAttribute<?>> illegalAttributes = map.keySet().stream().filter(attribute -> !attribute.isPositional()).toList();
            return !illegalAttributes.isEmpty()
                ? DataResult.error(() -> "The following attributes cannot be positional: " + illegalAttributes)
                : DataResult.success(map);
        }
    );
    private final Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entries;

    private static EnvironmentAttributeMap filterSyncable(final EnvironmentAttributeMap attributes) {
        return new EnvironmentAttributeMap(Map.copyOf(Maps.filterKeys(attributes.entries, EnvironmentAttribute::isSyncable)));
    }

    private EnvironmentAttributeMap(final Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entries) {
        this.entries = entries;
    }

    public static EnvironmentAttributeMap.Builder builder() {
        return new EnvironmentAttributeMap.Builder();
    }

    public <Value> EnvironmentAttributeMap.@Nullable Entry<Value, ?> get(final EnvironmentAttribute<Value> attribute) {
        return (EnvironmentAttributeMap.Entry<Value, ?>)this.entries.get(attribute);
    }

    public <Value> Value applyModifier(final EnvironmentAttribute<Value> attribute, final Value baseValue) {
        EnvironmentAttributeMap.Entry<Value, ?> entry = this.get(attribute);
        return entry != null ? entry.applyModifier(baseValue) : baseValue;
    }

    public boolean contains(final EnvironmentAttribute<?> attribute) {
        return this.entries.containsKey(attribute);
    }

    public Set<EnvironmentAttribute<?>> keySet() {
        return this.entries.keySet();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj == this ? true : obj instanceof EnvironmentAttributeMap attributes && this.entries.equals(attributes.entries);
    }

    @Override
    public int hashCode() {
        return this.entries.hashCode();
    }

    @Override
    public String toString() {
        return this.entries.toString();
    }

    public static class Builder {
        private final Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entries = new HashMap<>();

        private Builder() {
        }

        public EnvironmentAttributeMap.Builder putAll(final EnvironmentAttributeMap map) {
            this.entries.putAll(map.entries);
            return this;
        }

        public <Value, Parameter> EnvironmentAttributeMap.Builder modify(
            final EnvironmentAttribute<Value> attribute, final AttributeModifier<Value, Parameter> modifier, final Parameter value
        ) {
            attribute.type().checkAllowedModifier(modifier);
            this.entries.put(attribute, new EnvironmentAttributeMap.Entry<>(value, modifier));
            return this;
        }

        public <Value> EnvironmentAttributeMap.Builder set(final EnvironmentAttribute<Value> attribute, final Value value) {
            return this.modify(attribute, AttributeModifier.override(), value);
        }

        public EnvironmentAttributeMap build() {
            return this.entries.isEmpty() ? EnvironmentAttributeMap.EMPTY : new EnvironmentAttributeMap(Map.copyOf(this.entries));
        }
    }

    public record Entry<Value, Argument>(Argument argument, AttributeModifier<Value, Argument> modifier) {
        private static <Value> Codec<EnvironmentAttributeMap.Entry<Value, ?>> createCodec(final EnvironmentAttribute<Value> attribute) {
            Codec<EnvironmentAttributeMap.Entry<Value, ?>> fullCodec = attribute.type()
                .modifierCodec()
                .dispatch(
                    "modifier",
                    EnvironmentAttributeMap.Entry::modifier,
                    Util.memoize(modifier -> createFullCodec(attribute, (AttributeModifier<Value, ?>)modifier))
                );
            return Codec.either(attribute.valueCodec(), fullCodec)
                .xmap(
                    either -> either.map(value -> new EnvironmentAttributeMap.Entry<>(value, AttributeModifier.override()), e -> e),
                    entry -> entry.modifier == AttributeModifier.override()
                        ? Either.left((Value)entry.argument())
                        : Either.right((EnvironmentAttributeMap.Entry<Value, ?>)entry)
                );
        }

        private static <Value, Argument> MapCodec<EnvironmentAttributeMap.Entry<Value, Argument>> createFullCodec(
            final EnvironmentAttribute<Value> attribute, final AttributeModifier<Value, Argument> modifier
        ) {
            return RecordCodecBuilder.mapCodec(
                i -> i.group(modifier.argumentCodec(attribute).fieldOf("argument").forGetter(EnvironmentAttributeMap.Entry::argument))
                    .apply(i, value -> new EnvironmentAttributeMap.Entry<>(value, modifier))
            );
        }

        public Value applyModifier(final Value subject) {
            return this.modifier.apply(subject, this.argument);
        }
    }
}
