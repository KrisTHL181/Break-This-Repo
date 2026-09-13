package net.minecraft.core.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Map.Entry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record TypedDataComponent<T>(DataComponentType<T> type, T value) {
    public static final StreamCodec<RegistryFriendlyByteBuf, TypedDataComponent<?>> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, TypedDataComponent<?>>() {
        public TypedDataComponent<?> decode(final RegistryFriendlyByteBuf input) {
            DataComponentType<?> type = DataComponentType.STREAM_CODEC.decode(input);
            return decodeTyped(input, (DataComponentType)type);
        }

        private static <T> TypedDataComponent<T> decodeTyped(final RegistryFriendlyByteBuf input, final DataComponentType<T> type) {
            return new TypedDataComponent<>(type, type.streamCodec().decode(input));
        }

        public void encode(final RegistryFriendlyByteBuf output, final TypedDataComponent<?> value) {
            encodeCap(output, (TypedDataComponent)value);
        }

        private static <T> void encodeCap(final RegistryFriendlyByteBuf output, final TypedDataComponent<T> component) {
            DataComponentType.STREAM_CODEC.encode(output, component.type());
            component.type().streamCodec().encode(output, component.value());
        }
    };

    static TypedDataComponent<?> fromEntryUnchecked(final Entry<DataComponentType<?>, Object> entry) {
        return createUnchecked(entry.getKey(), entry.getValue());
    }

    public static <T> TypedDataComponent<T> createUnchecked(final DataComponentType<T> type, final Object value) {
        return new TypedDataComponent<>(type, (T)value);
    }

    public void applyTo(final PatchedDataComponentMap components) {
        components.set(this.type, this.value);
    }

    public <D> DataResult<D> encodeValue(final DynamicOps<D> ops) {
        Codec<T> codec = this.type.codec();
        return codec == null ? DataResult.error(() -> "Component of type " + this.type + " is not encodable") : codec.encodeStart(ops, this.value);
    }

    @Override
    public String toString() {
        return this.type + "=>" + this.value;
    }
}
