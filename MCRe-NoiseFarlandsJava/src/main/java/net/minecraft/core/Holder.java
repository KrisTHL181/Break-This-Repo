package net.minecraft.core;

import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jspecify.annotations.Nullable;

public sealed interface Holder<T> permits Holder.Direct, Holder.Reference {
    T value();

    boolean isBound();

    boolean areComponentsBound();

    boolean is(Identifier key);

    boolean is(ResourceKey<T> key);

    boolean is(Predicate<ResourceKey<T>> predicate);

    boolean is(TagKey<T> tag);

    @Deprecated
    boolean is(Holder<T> holder);

    Stream<TagKey<T>> tags();

    DataComponentMap components();

    Either<ResourceKey<T>, T> unwrap();

    Optional<ResourceKey<T>> unwrapKey();

    Holder.Kind kind();

    boolean canSerializeIn(HolderOwner<T> registry);

    default String getRegisteredName() {
        return this.unwrapKey().map(key -> key.identifier().toString()).orElse("[unregistered]");
    }

    static <T> Holder<T> direct(final T value) {
        return new Holder.Direct<>(value, DataComponentMap.EMPTY);
    }

    static <T> Holder<T> direct(final T value, final DataComponentMap components) {
        return new Holder.Direct<>(value, components);
    }

    record Direct<T>(T value, DataComponentMap components) implements Holder<T> {
        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public boolean areComponentsBound() {
            return true;
        }

        @Override
        public boolean is(final Identifier key) {
            return false;
        }

        @Override
        public boolean is(final ResourceKey<T> key) {
            return false;
        }

        @Override
        public boolean is(final TagKey<T> tag) {
            return false;
        }

        @Override
        public boolean is(final Holder<T> holder) {
            return this.value.equals(holder.value());
        }

        @Override
        public boolean is(final Predicate<ResourceKey<T>> predicate) {
            return false;
        }

        @Override
        public Either<ResourceKey<T>, T> unwrap() {
            return Either.right(this.value);
        }

        @Override
        public Optional<ResourceKey<T>> unwrapKey() {
            return Optional.empty();
        }

        @Override
        public Holder.Kind kind() {
            return Holder.Kind.DIRECT;
        }

        @Override
        public String toString() {
            return "Direct{" + this.value + "}";
        }

        @Override
        public boolean canSerializeIn(final HolderOwner<T> registry) {
            return true;
        }

        @Override
        public Stream<TagKey<T>> tags() {
            return Stream.of();
        }
    }

    enum Kind {
        REFERENCE,
        DIRECT;
    }

    non-sealed class Reference<T> implements Holder<T> {
        private final HolderOwner<T> owner;
        private @Nullable Set<TagKey<T>> tags;
        private @Nullable DataComponentMap components;
        private final Holder.Reference.Type type;
        private @Nullable ResourceKey<T> key;
        private @Nullable T value;

        protected Reference(final Holder.Reference.Type type, final HolderOwner<T> owner, final @Nullable ResourceKey<T> key, final @Nullable T value) {
            this.owner = owner;
            this.type = type;
            this.key = key;
            this.value = value;
        }

        public static <T> Holder.Reference<T> createStandAlone(final HolderOwner<T> owner, final ResourceKey<T> key) {
            return new Holder.Reference<>(Holder.Reference.Type.STAND_ALONE, owner, key, null);
        }

        @Deprecated
        public static <T> Holder.Reference<T> createIntrusive(final HolderOwner<T> owner, final @Nullable T value) {
            return new Holder.Reference<>(Holder.Reference.Type.INTRUSIVE, owner, null, value);
        }

        public ResourceKey<T> key() {
            if (this.key == null) {
                throw new IllegalStateException("Trying to access unbound value '" + this.value + "' from registry " + this.owner);
            } else {
                return this.key;
            }
        }

        @Override
        public T value() {
            if (this.value == null) {
                throw new IllegalStateException("Trying to access unbound value '" + this.key + "' from registry " + this.owner);
            } else {
                return this.value;
            }
        }

        @Override
        public boolean is(final Identifier key) {
            return this.key().identifier().equals(key);
        }

        @Override
        public boolean is(final ResourceKey<T> key) {
            return this.key() == key;
        }

        private Set<TagKey<T>> boundTags() {
            if (this.tags == null) {
                throw new IllegalStateException("Tags not bound");
            } else {
                return this.tags;
            }
        }

        @Override
        public boolean is(final TagKey<T> tag) {
            return this.boundTags().contains(tag);
        }

        @Override
        public boolean is(final Holder<T> holder) {
            return holder.is(this.key());
        }

        @Override
        public boolean is(final Predicate<ResourceKey<T>> predicate) {
            return predicate.test(this.key());
        }

        @Override
        public boolean canSerializeIn(final HolderOwner<T> context) {
            return this.owner.canSerializeIn(context);
        }

        @Override
        public Either<ResourceKey<T>, T> unwrap() {
            return Either.left(this.key());
        }

        @Override
        public Optional<ResourceKey<T>> unwrapKey() {
            return Optional.of(this.key());
        }

        @Override
        public Holder.Kind kind() {
            return Holder.Kind.REFERENCE;
        }

        @Override
        public boolean isBound() {
            return this.key != null && this.value != null;
        }

        @Override
        public boolean areComponentsBound() {
            return this.components != null;
        }

        void bindKey(final ResourceKey<T> key) {
            if (this.key != null && key != this.key) {
                throw new IllegalStateException("Can't change holder key: existing=" + this.key + ", new=" + key);
            }

            this.key = key;
        }

        protected void bindValue(final T value) {
            if (this.type == Holder.Reference.Type.INTRUSIVE && this.value != value) {
                throw new IllegalStateException("Can't change holder " + this.key + " value: existing=" + this.value + ", new=" + value);
            }

            this.value = value;
        }

        void bindTags(final Collection<TagKey<T>> tags) {
            this.tags = Set.copyOf(tags);
        }

        public void bindComponents(final DataComponentMap components) {
            this.components = components;
        }

        @Override
        public Stream<TagKey<T>> tags() {
            return this.boundTags().stream();
        }

        @Override
        public DataComponentMap components() {
            return Objects.requireNonNull(this.components, "Components not bound yet");
        }

        @Override
        public String toString() {
            return "Reference{" + this.key + "=" + this.value + "}";
        }

        protected enum Type {
            STAND_ALONE,
            INTRUSIVE;
        }
    }
}