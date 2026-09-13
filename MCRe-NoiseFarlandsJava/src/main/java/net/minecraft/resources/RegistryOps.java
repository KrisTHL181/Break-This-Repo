package net.minecraft.resources;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.util.ExtraCodecs;

public class RegistryOps<T> extends DelegatingOps<T> {
    private final RegistryOps.RegistryInfoLookup lookupProvider;

    public static <T> RegistryOps<T> create(final DynamicOps<T> parent, final HolderLookup.Provider lookupProvider) {
        return create(parent, new RegistryOps.HolderLookupAdapter(lookupProvider));
    }

    public static <T> RegistryOps<T> create(final DynamicOps<T> parent, final RegistryOps.RegistryInfoLookup lookupProvider) {
        return new RegistryOps<>(parent, lookupProvider);
    }

    public static <T> Dynamic<T> injectRegistryContext(final Dynamic<T> dynamic, final HolderLookup.Provider lookupProvider) {
        return new Dynamic<>(lookupProvider.createSerializationContext(dynamic.getOps()), dynamic.getValue());
    }

    private RegistryOps(final DynamicOps<T> parent, final RegistryOps.RegistryInfoLookup lookupProvider) {
        super(parent);
        this.lookupProvider = lookupProvider;
    }

    public <U> RegistryOps<U> withParent(final DynamicOps<U> parent) {
        return (RegistryOps<U>)(parent == this.delegate ? this : new RegistryOps<>(parent, this.lookupProvider));
    }

    public <E> Optional<HolderOwner<E>> owner(final ResourceKey<? extends Registry<? extends E>> registryKey) {
        return this.lookupProvider.lookup(registryKey).map(RegistryOps.RegistryInfo::owner);
    }

    public <E> Optional<HolderGetter<E>> getter(final ResourceKey<? extends Registry<? extends E>> registryKey) {
        return this.lookupProvider.lookup(registryKey).map(RegistryOps.RegistryInfo::getter);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            RegistryOps<?> ops = (RegistryOps<?>)obj;
            return this.delegate.equals(ops.delegate) && this.lookupProvider.equals(ops.lookupProvider);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.delegate.hashCode() * 31 + this.lookupProvider.hashCode();
    }

    public static <E, O> RecordCodecBuilder<O, HolderGetter<E>> retrieveGetter(final ResourceKey<? extends Registry<? extends E>> registryKey) {
        return ExtraCodecs.retrieveContext(
                ops -> ops instanceof RegistryOps<?> registryOps
                    ? registryOps.lookupProvider
                        .lookup(registryKey)
                        .map(r -> DataResult.success(r.getter(), r.elementsLifecycle()))
                        .orElseGet(() -> DataResult.error(() -> "Unknown registry: " + registryKey))
                    : DataResult.error(() -> "Not a registry ops")
            )
            .forGetter(e -> null);
    }

    public static <E, O> RecordCodecBuilder<O, Holder.Reference<E>> retrieveElement(final ResourceKey<E> key) {
        ResourceKey<? extends Registry<E>> registryKey = ResourceKey.createRegistryKey(key.registry());
        return ExtraCodecs.retrieveContext(
                ops -> ops instanceof RegistryOps<?> registryOps
                    ? registryOps.lookupProvider
                        .lookup(registryKey)
                        .flatMap(r -> r.getter().get(key))
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "Can't find value: " + key))
                    : DataResult.error(() -> "Not a registry ops")
            )
            .forGetter(e -> null);
    }

    private static final class HolderLookupAdapter implements RegistryOps.RegistryInfoLookup {
        private final HolderLookup.Provider lookupProvider;
        private final Map<ResourceKey<? extends Registry<?>>, Optional<? extends RegistryOps.RegistryInfo<?>>> lookups = new ConcurrentHashMap<>();

        public HolderLookupAdapter(final HolderLookup.Provider lookupProvider) {
            this.lookupProvider = lookupProvider;
        }

        @Override
        public <E> Optional<RegistryOps.RegistryInfo<E>> lookup(final ResourceKey<? extends Registry<? extends E>> registryKey) {
            return (Optional<RegistryOps.RegistryInfo<E>>)this.lookups.computeIfAbsent(registryKey, this::createLookup);
        }

        private Optional<RegistryOps.RegistryInfo<Object>> createLookup(final ResourceKey<? extends Registry<?>> key) {
            return this.lookupProvider.lookup(key).map(RegistryOps.RegistryInfo::fromRegistryLookup);
        }

        @Override
        public boolean equals(final Object obj) {
            return this == obj ? true : obj instanceof RegistryOps.HolderLookupAdapter adapter && this.lookupProvider.equals(adapter.lookupProvider);
        }

        @Override
        public int hashCode() {
            return this.lookupProvider.hashCode();
        }
    }

    public record RegistryInfo<T>(HolderOwner<T> owner, HolderGetter<T> getter, Lifecycle elementsLifecycle) {
        public static <T> RegistryOps.RegistryInfo<T> fromRegistryLookup(final HolderLookup.RegistryLookup<T> registry) {
            return new RegistryOps.RegistryInfo<>(registry, registry, registry.registryLifecycle());
        }
    }

    public interface RegistryInfoLookup {
        <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey);
    }
}