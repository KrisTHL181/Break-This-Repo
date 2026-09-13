package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public class RegistrySetBuilder {
    private final List<RegistrySetBuilder.RegistryStub<?>> entries = new ArrayList<>();

    private static <T> HolderGetter<T> wrapContextLookup(final HolderLookup.RegistryLookup<T> original) {
        return new RegistrySetBuilder.EmptyTagLookup<T>(original) {
            @Override
            public Optional<Holder.Reference<T>> get(final ResourceKey<T> id) {
                return original.get(id);
            }
        };
    }

    private static <T> HolderLookup.RegistryLookup<T> lookupFromMap(
        final ResourceKey<? extends Registry<? extends T>> key,
        final Lifecycle lifecycle,
        final HolderOwner<T> owner,
        final Map<ResourceKey<T>, Holder.Reference<T>> entries
    ) {
        return new RegistrySetBuilder.EmptyTagRegistryLookup<T>(owner) {
            @Override
            public ResourceKey<? extends Registry<? extends T>> key() {
                return key;
            }

            @Override
            public Lifecycle registryLifecycle() {
                return lifecycle;
            }

            @Override
            public Optional<Holder.Reference<T>> get(final ResourceKey<T> id) {
                return Optional.ofNullable(entries.get(id));
            }

            @Override
            public Stream<Holder.Reference<T>> listElements() {
                return entries.values().stream();
            }
        };
    }

    public <T> RegistrySetBuilder add(
        final ResourceKey<? extends Registry<T>> key, final Lifecycle lifecycle, final RegistrySetBuilder.RegistryBootstrap<T> bootstrap
    ) {
        this.entries.add(new RegistrySetBuilder.RegistryStub<>(key, lifecycle, bootstrap));
        return this;
    }

    public <T> RegistrySetBuilder add(final ResourceKey<? extends Registry<T>> key, final RegistrySetBuilder.RegistryBootstrap<T> bootstrap) {
        return this.add(key, Lifecycle.stable(), bootstrap);
    }

    private RegistrySetBuilder.BuildState createState(final RegistryAccess context) {
        RegistrySetBuilder.BuildState state = RegistrySetBuilder.BuildState.create(context, this.entries.stream().map(RegistrySetBuilder.RegistryStub::key));
        this.entries.forEach(e -> e.apply(state));
        return state;
    }

    private static HolderLookup.Provider buildProviderWithContext(
        final RegistrySetBuilder.UniversalOwner owner, final RegistryAccess context, final Stream<HolderLookup.RegistryLookup<?>> newRegistries
    ) {
        record Entry<T>(HolderLookup.RegistryLookup<T> lookup, RegistryOps.RegistryInfo<T> opsInfo) {
            public static <T> Entry<T> createForContextRegistry(final HolderLookup.RegistryLookup<T> registryLookup) {
                return new Entry<>(
                    new RegistrySetBuilder.EmptyTagLookupWrapper<>(registryLookup, registryLookup), RegistryOps.RegistryInfo.fromRegistryLookup(registryLookup)
                );
            }

            public static <T> Entry<T> createForNewRegistry(final RegistrySetBuilder.UniversalOwner owner, final HolderLookup.RegistryLookup<T> registryLookup) {
                return new Entry<>(
                    new RegistrySetBuilder.EmptyTagLookupWrapper<>(owner.cast(), registryLookup),
                    new RegistryOps.RegistryInfo<>(owner.cast(), registryLookup, registryLookup.registryLifecycle())
                );
            }
        }

        final Map<ResourceKey<? extends Registry<?>>, Entry<?>> lookups = new HashMap<>();
        context.registries().forEach(contextRegistry -> lookups.put(contextRegistry.key(), Entry.createForContextRegistry(contextRegistry.value())));
        newRegistries.forEach(newRegistry -> lookups.put(newRegistry.key(), Entry.createForNewRegistry(owner, newRegistry)));
        return new HolderLookup.Provider() {
            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
                return lookups.keySet().stream();
            }

            private <T> Optional<Entry<T>> getEntry(final ResourceKey<? extends Registry<? extends T>> key) {
                return Optional.ofNullable((Entry<T>)lookups.get(key));
            }

            @Override
            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(final ResourceKey<? extends Registry<? extends T>> key) {
                return this.getEntry(key).map(Entry::lookup);
            }

            @Override
            public <V> RegistryOps<V> createSerializationContext(final DynamicOps<V> parent) {
                return RegistryOps.create(parent, new RegistryOps.RegistryInfoLookup() {
                    @Override
                    public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(final ResourceKey<? extends Registry<? extends T>> registryKey) {
                        return getEntry(registryKey).map(Entry::opsInfo);
                    }
                });
            }
        };
    }

    public HolderLookup.Provider build(final RegistryAccess context) {
        RegistrySetBuilder.BuildState state = this.createState(context);
        Stream<HolderLookup.RegistryLookup<?>> newRegistries = this.entries
            .stream()
            .map(stub -> stub.collectRegisteredValues(state).buildAsLookup(state.owner));
        HolderLookup.Provider result = buildProviderWithContext(state.owner, context, newRegistries);
        state.reportNotCollectedHolders();
        state.reportUnclaimedRegisteredValues();
        state.throwOnError();
        return result;
    }

    private HolderLookup.Provider createLazyFullPatchedRegistries(
        final RegistryAccess context,
        final HolderLookup.Provider fallbackProvider,
        final Cloner.Factory clonerFactory,
        final Map<ResourceKey<? extends Registry<?>>, RegistrySetBuilder.RegistryContents<?>> newRegistries,
        final HolderLookup.Provider patchOnlyRegistries
    ) {
        RegistrySetBuilder.UniversalOwner fullPatchedOwner = new RegistrySetBuilder.UniversalOwner();
        MutableObject<HolderLookup.Provider> resultReference = new MutableObject<>();
        List<HolderLookup.RegistryLookup<?>> lazyFullRegistries = newRegistries.keySet()
            .stream()
            .map(
                registryKey -> this.createLazyFullPatchedRegistries(
                    fullPatchedOwner,
                    clonerFactory,
                    (ResourceKey<? extends Registry<? extends Object>>)registryKey,
                    patchOnlyRegistries,
                    fallbackProvider,
                    resultReference
                )
            )
            .collect(Collectors.toUnmodifiableList());
        HolderLookup.Provider result = buildProviderWithContext(fullPatchedOwner, context, lazyFullRegistries.stream());
        resultReference.setValue(result);
        return result;
    }

    private <T> HolderLookup.RegistryLookup<T> createLazyFullPatchedRegistries(
        final HolderOwner<T> owner,
        final Cloner.Factory clonerFactory,
        final ResourceKey<? extends Registry<? extends T>> registryKey,
        final HolderLookup.Provider patchProvider,
        final HolderLookup.Provider fallbackProvider,
        final MutableObject<HolderLookup.Provider> targetProvider
    ) {
        Cloner<T> cloner = clonerFactory.cloner(registryKey);
        if (cloner == null) {
            throw new NullPointerException("No cloner for " + registryKey.identifier());
        }

        Map<ResourceKey<T>, Holder.Reference<T>> entries = new HashMap<>();
        HolderLookup.RegistryLookup<T> patchContents = patchProvider.lookupOrThrow(registryKey);
        patchContents.listElements().forEach(elementHolder -> {
            ResourceKey<T> elementKey = elementHolder.key();
            RegistrySetBuilder.LazyHolder<T> holder = new RegistrySetBuilder.LazyHolder<>(owner, elementKey);
            holder.supplier = () -> cloner.clone((T)elementHolder.value(), patchProvider, targetProvider.get());
            entries.put(elementKey, holder);
        });
        HolderLookup.RegistryLookup<T> fallbackContents = fallbackProvider.lookupOrThrow(registryKey);
        fallbackContents.listElements().forEach(elementHolder -> {
            ResourceKey<T> elementKey = elementHolder.key();
            entries.computeIfAbsent(elementKey, key -> {
                RegistrySetBuilder.LazyHolder<T> holder = new RegistrySetBuilder.LazyHolder<>(owner, elementKey);
                holder.supplier = () -> cloner.clone((T)elementHolder.value(), fallbackProvider, targetProvider.get());
                return holder;
            });
        });
        Lifecycle lifecycle = patchContents.registryLifecycle().add(fallbackContents.registryLifecycle());
        return lookupFromMap(registryKey, lifecycle, owner, entries);
    }

    public RegistrySetBuilder.PatchedRegistries buildPatch(
        final RegistryAccess context, final HolderLookup.Provider fallbackProvider, final Cloner.Factory clonerFactory
    ) {
        RegistrySetBuilder.BuildState state = this.createState(context);
        Map<ResourceKey<? extends Registry<?>>, RegistrySetBuilder.RegistryContents<?>> newRegistries = new HashMap<>();
        this.entries
            .stream()
            .map(stub -> stub.collectRegisteredValues(state))
            .forEach(e -> newRegistries.put(e.key, (RegistrySetBuilder.RegistryContents<?>)e));
        Set<ResourceKey<? extends Registry<?>>> contextRegistries = context.listRegistryKeys().collect(Collectors.toUnmodifiableSet());
        fallbackProvider.listRegistryKeys()
            .filter(k -> !contextRegistries.contains(k))
            .forEach(
                resourceKey -> newRegistries.putIfAbsent(
                    (ResourceKey<? extends Registry<?>>)resourceKey,
                    new RegistrySetBuilder.RegistryContents<>((ResourceKey<? extends Registry<?>>)resourceKey, Lifecycle.stable(), Map.of())
                )
            );
        Stream<HolderLookup.RegistryLookup<?>> dynamicRegistries = newRegistries.values()
            .stream()
            .map(registryContents -> registryContents.buildAsLookup(state.owner));
        HolderLookup.Provider patchOnlyRegistries = buildProviderWithContext(state.owner, context, dynamicRegistries);
        state.reportUnclaimedRegisteredValues();
        state.throwOnError();
        HolderLookup.Provider fullPatchedRegistries = this.createLazyFullPatchedRegistries(
            context, fallbackProvider, clonerFactory, newRegistries, patchOnlyRegistries
        );
        return new RegistrySetBuilder.PatchedRegistries(fullPatchedRegistries, patchOnlyRegistries);
    }

    private record BuildState(
        RegistrySetBuilder.UniversalOwner owner,
        RegistrySetBuilder.UniversalLookup lookup,
        Map<Identifier, HolderGetter<?>> registries,
        Map<ResourceKey<?>, RegistrySetBuilder.RegisteredValue<?>> registeredValues,
        List<RuntimeException> errors
    ) {
        public static RegistrySetBuilder.BuildState create(final RegistryAccess context, final Stream<ResourceKey<? extends Registry<?>>> newRegistries) {
            RegistrySetBuilder.UniversalOwner owner = new RegistrySetBuilder.UniversalOwner();
            List<RuntimeException> errors = new ArrayList<>();
            RegistrySetBuilder.UniversalLookup lookup = new RegistrySetBuilder.UniversalLookup(owner);
            Builder<Identifier, HolderGetter<?>> registries = ImmutableMap.builder();
            context.registries()
                .forEach(contextRegistry -> registries.put(contextRegistry.key().identifier(), RegistrySetBuilder.wrapContextLookup(contextRegistry.value())));
            newRegistries.forEach(newRegistry -> registries.put(newRegistry.identifier(), lookup));
            return new RegistrySetBuilder.BuildState(owner, lookup, registries.build(), new HashMap<>(), errors);
        }

        public <T> BootstrapContext<T> bootstrapContext() {
            return new BootstrapContext<T>() {
                @Override
                public Holder.Reference<T> register(final ResourceKey<T> key, final T value, final Lifecycle lifecycle) {
                    RegistrySetBuilder.RegisteredValue<?> previousValue = BuildState.this.registeredValues
                        .put(key, new RegistrySetBuilder.RegisteredValue(value, lifecycle));
                    if (previousValue != null) {
                        BuildState.this.errors
                            .add(new IllegalStateException("Duplicate registration for " + key + ", new=" + value + ", old=" + previousValue.value));
                    }

                    return BuildState.this.lookup.getOrCreate(key);
                }

                @Override
                public <S> HolderGetter<S> lookup(final ResourceKey<? extends Registry<? extends S>> key) {
                    return (HolderGetter<S>)BuildState.this.registries.getOrDefault(key.identifier(), BuildState.this.lookup);
                }
            };
        }

        public void reportUnclaimedRegisteredValues() {
            this.registeredValues
                .forEach((key, registeredValue) -> this.errors.add(new IllegalStateException("Orpaned value " + registeredValue.value + " for key " + key)));
        }

        public void reportNotCollectedHolders() {
            for (ResourceKey<Object> key : this.lookup.holders.keySet()) {
                this.errors.add(new IllegalStateException("Unreferenced key: " + key));
            }
        }

        public void throwOnError() {
            if (!this.errors.isEmpty()) {
                IllegalStateException result = new IllegalStateException("Errors during registry creation");

                for (RuntimeException error : this.errors) {
                    result.addSuppressed(error);
                }

                throw result;
            }
        }
    }

    private abstract static class EmptyTagLookup<T> implements HolderGetter<T> {
        protected final HolderOwner<T> owner;

        protected EmptyTagLookup(final HolderOwner<T> owner) {
            this.owner = owner;
        }

        @Override
        public Optional<HolderSet.Named<T>> get(final TagKey<T> id) {
            return Optional.of(HolderSet.emptyNamed(this.owner, id));
        }
    }

    private static class EmptyTagLookupWrapper<T> extends RegistrySetBuilder.EmptyTagRegistryLookup<T> implements HolderLookup.RegistryLookup.Delegate<T> {
        private final HolderLookup.RegistryLookup<T> parent;

        private EmptyTagLookupWrapper(final HolderOwner<T> owner, final HolderLookup.RegistryLookup<T> parent) {
            super(owner);
            this.parent = parent;
        }

        @Override
        public HolderLookup.RegistryLookup<T> parent() {
            return this.parent;
        }
    }

    private abstract static class EmptyTagRegistryLookup<T> extends RegistrySetBuilder.EmptyTagLookup<T> implements HolderLookup.RegistryLookup<T> {
        protected EmptyTagRegistryLookup(final HolderOwner<T> owner) {
            super(owner);
        }

        @Override
        public Stream<HolderSet.Named<T>> listTags() {
            throw new UnsupportedOperationException("Tags are not available in datagen");
        }
    }

    private static class LazyHolder<T> extends Holder.Reference<T> {
        private @Nullable Supplier<T> supplier;

        protected LazyHolder(final HolderOwner<T> owner, final @Nullable ResourceKey<T> key) {
            super(Holder.Reference.Type.STAND_ALONE, owner, key, null);
        }

        @Override
        protected void bindValue(final T value) {
            super.bindValue(value);
            this.supplier = null;
        }

        @Override
        public T value() {
            if (this.supplier != null) {
                this.bindValue(this.supplier.get());
            }

            return super.value();
        }
    }

    public record PatchedRegistries(HolderLookup.Provider full, HolderLookup.Provider patches) {
    }

    private record RegisteredValue<T>(T value, Lifecycle lifecycle) {
    }

    @FunctionalInterface
    public interface RegistryBootstrap<T> {
        void run(BootstrapContext<T> registry);
    }

    private record RegistryContents<T>(
        ResourceKey<? extends Registry<? extends T>> key, Lifecycle lifecycle, Map<ResourceKey<T>, RegistrySetBuilder.ValueAndHolder<T>> values
    ) {
        public HolderLookup.RegistryLookup<T> buildAsLookup(final RegistrySetBuilder.UniversalOwner owner) {
            Map<ResourceKey<T>, Holder.Reference<T>> entries = this.values
                .entrySet()
                .stream()
                .collect(Collectors.toUnmodifiableMap(java.util.Map.Entry::getKey, e -> {
                    RegistrySetBuilder.ValueAndHolder<T> entry = e.getValue();
                    Holder.Reference<T> holder = entry.holder().orElseGet(() -> Holder.Reference.createStandAlone(owner.cast(), e.getKey()));
                    holder.bindValue(entry.value().value());
                    return holder;
                }));
            return RegistrySetBuilder.lookupFromMap(this.key, this.lifecycle, owner.cast(), entries);
        }
    }

    private record RegistryStub<T>(ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle, RegistrySetBuilder.RegistryBootstrap<T> bootstrap) {
        private void apply(final RegistrySetBuilder.BuildState state) {
            this.bootstrap.run(state.bootstrapContext());
        }

        public RegistrySetBuilder.RegistryContents<T> collectRegisteredValues(final RegistrySetBuilder.BuildState state) {
            Map<ResourceKey<T>, RegistrySetBuilder.ValueAndHolder<T>> result = new HashMap<>();
            Iterator<java.util.Map.Entry<ResourceKey<?>, RegistrySetBuilder.RegisteredValue<?>>> iterator = state.registeredValues.entrySet().iterator();

            while (iterator.hasNext()) {
                java.util.Map.Entry<ResourceKey<?>, RegistrySetBuilder.RegisteredValue<?>> entry = iterator.next();
                ResourceKey<?> key = entry.getKey();
                if (key.isFor(this.key)) {
                    ResourceKey<T> castKey = (ResourceKey<T>)key;
                    RegistrySetBuilder.RegisteredValue<T> value = (RegistrySetBuilder.RegisteredValue<T>)entry.getValue();
                    Holder.Reference<T> holder = (Holder.Reference<T>)state.lookup.holders.remove(key);
                    result.put(castKey, new RegistrySetBuilder.ValueAndHolder<>(value, Optional.ofNullable(holder)));
                    iterator.remove();
                }
            }

            return new RegistrySetBuilder.RegistryContents<>(this.key, this.lifecycle, result);
        }
    }

    private static class UniversalLookup extends RegistrySetBuilder.EmptyTagLookup<Object> {
        private final Map<ResourceKey<Object>, Holder.Reference<Object>> holders = new HashMap<>();

        public UniversalLookup(final HolderOwner<Object> owner) {
            super(owner);
        }

        @Override
        public Optional<Holder.Reference<Object>> get(final ResourceKey<Object> id) {
            return Optional.of(this.getOrCreate(id));
        }

        private <T> Holder.Reference<T> getOrCreate(final ResourceKey<T> id) {
            return (Holder.Reference<T>)((Map)this.holders).computeIfAbsent(id, k -> Holder.Reference.createStandAlone(this.owner, (ResourceKey<Object>)k));
        }
    }

    private static class UniversalOwner implements HolderOwner<Object> {
        public <T> HolderOwner<T> cast() {
            return (HolderOwner<T>)this;
        }
    }

    private record ValueAndHolder<T>(RegistrySetBuilder.RegisteredValue<T> value, Optional<Holder.Reference<T>> holder) {
    }
}
