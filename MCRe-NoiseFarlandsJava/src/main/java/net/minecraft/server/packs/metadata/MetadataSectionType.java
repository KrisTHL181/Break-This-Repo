package net.minecraft.server.packs.metadata;

import com.mojang.serialization.Codec;
import java.util.Optional;

public record MetadataSectionType<T>(String name, Codec<T> codec) {
    public MetadataSectionType.WithValue<T> withValue(final T value) {
        return new MetadataSectionType.WithValue<>(this, value);
    }

    public record WithValue<T>(MetadataSectionType<T> type, T value) {
        public <U> Optional<U> unwrapToType(final MetadataSectionType<U> type) {
            return type == this.type ? Optional.of((U)this.value) : Optional.empty();
        }
    }
}
