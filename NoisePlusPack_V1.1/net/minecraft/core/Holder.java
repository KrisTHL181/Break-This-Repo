package net.minecraft.core;

import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jspecify.annotations.Nullable;

public interface Holder<T> {
   T value();

   boolean isBound();

   boolean is(Identifier var1);

   boolean is(ResourceKey<T> var1);

   boolean is(Predicate<ResourceKey<T>> var1);

   boolean is(TagKey<T> var1);

   @Deprecated
   boolean is(Holder<T> var1);

   Stream<TagKey<T>> tags();

   Either<ResourceKey<T>, T> unwrap();

   Optional<ResourceKey<T>> unwrapKey();

   Holder.Kind kind();

   boolean canSerializeIn(HolderOwner<T> var1);

   default String getRegisteredName() {
      return this.unwrapKey().map(p_448557_ -> p_448557_.identifier().toString()).orElse("[unregistered]");
   }

   static <T> Holder<T> direct(T p_205710_) {
      return new Holder.Direct<>(p_205710_);
   }

   record Direct<T>(T value) implements Holder<T> {
      @Override
      public boolean isBound() {
         return true;
      }

      @Override
      public boolean is(Identifier p_451249_) {
         return false;
      }

      @Override
      public boolean is(ResourceKey<T> p_205725_) {
         return false;
      }

      @Override
      public boolean is(TagKey<T> p_205719_) {
         return false;
      }

      @Override
      public boolean is(Holder<T> p_329830_) {
         return this.value.equals(p_329830_.value());
      }

      @Override
      public boolean is(Predicate<ResourceKey<T>> p_205723_) {
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
      public boolean canSerializeIn(HolderOwner<T> p_256328_) {
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

   class Reference<T> implements Holder<T> {
      private final HolderOwner<T> owner;
      private @Nullable Set<TagKey<T>> tags;
      private final Holder.Reference.Type type;
      private @Nullable ResourceKey<T> key;
      private @Nullable T value;

      protected Reference(Holder.Reference.Type p_256425_, HolderOwner<T> p_256562_, @Nullable ResourceKey<T> p_256636_, @Nullable T p_255889_) {
         this.owner = p_256562_;
         this.type = p_256425_;
         this.key = p_256636_;
         this.value = p_255889_;
      }

      public static <T> Holder.Reference<T> createStandAlone(HolderOwner<T> p_255955_, ResourceKey<T> p_255958_) {
         return new Holder.Reference<>(Holder.Reference.Type.STAND_ALONE, p_255955_, p_255958_, null);
      }

      @Deprecated
      public static <T> Holder.Reference<T> createIntrusive(HolderOwner<T> p_256106_, @Nullable T p_255948_) {
         return new Holder.Reference<>(Holder.Reference.Type.INTRUSIVE, p_256106_, null, p_255948_);
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
      public boolean is(Identifier p_459053_) {
         return this.key().identifier().equals(p_459053_);
      }

      @Override
      public boolean is(ResourceKey<T> p_205774_) {
         return this.key() == p_205774_;
      }

      private Set<TagKey<T>> boundTags() {
         if (this.tags == null) {
            throw new IllegalStateException("Tags not bound");
         } else {
            return this.tags;
         }
      }

      @Override
      public boolean is(TagKey<T> p_205760_) {
         return this.boundTags().contains(p_205760_);
      }

      @Override
      public boolean is(Holder<T> p_335729_) {
         return p_335729_.is(this.key());
      }

      @Override
      public boolean is(Predicate<ResourceKey<T>> p_205772_) {
         return p_205772_.test(this.key());
      }

      @Override
      public boolean canSerializeIn(HolderOwner<T> p_256521_) {
         return this.owner.canSerializeIn(p_256521_);
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

      void bindKey(ResourceKey<T> p_251943_) {
         if (this.key != null && p_251943_ != this.key) {
            throw new IllegalStateException("Can't change holder key: existing=" + this.key + ", new=" + p_251943_);
         }

         this.key = p_251943_;
      }

      protected void bindValue(T p_249418_) {
         if (this.type == Holder.Reference.Type.INTRUSIVE && this.value != p_249418_) {
            throw new IllegalStateException("Can't change holder " + this.key + " value: existing=" + this.value + ", new=" + p_249418_);
         }

         this.value = p_249418_;
      }

      void bindTags(Collection<TagKey<T>> p_205770_) {
         this.tags = Set.copyOf(p_205770_);
      }

      @Override
      public Stream<TagKey<T>> tags() {
         return this.boundTags().stream();
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
