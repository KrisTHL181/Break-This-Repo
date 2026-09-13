package net.minecraft.core.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class DataComponentExactPredicate implements Predicate<DataComponentGetter> {
   public static final Codec<DataComponentExactPredicate> CODEC = DataComponentType.VALUE_MAP_CODEC
      .xmap(
         map -> new DataComponentExactPredicate(map.entrySet().stream().map(TypedDataComponent::fromEntryUnchecked).collect(Collectors.toList())),
         predicate -> predicate.expectedComponents
            .stream()
            .filter(e -> !e.type().isTransient())
            .collect(Collectors.toMap(TypedDataComponent::type, TypedDataComponent::value))
      );
   public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentExactPredicate> STREAM_CODEC = TypedDataComponent.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(DataComponentExactPredicate::new, predicate -> predicate.expectedComponents);
   public static final DataComponentExactPredicate EMPTY = new DataComponentExactPredicate(List.of());
   private final List<TypedDataComponent<?>> expectedComponents;

   private DataComponentExactPredicate(final List<TypedDataComponent<?>> expectedComponents) {
      this.expectedComponents = expectedComponents;
   }

   public static DataComponentExactPredicate.Builder builder() {
      return new DataComponentExactPredicate.Builder();
   }

   public static <T> DataComponentExactPredicate expect(final DataComponentType<T> type, final T value) {
      return new DataComponentExactPredicate(List.of(new TypedDataComponent<>(type, value)));
   }

   public static DataComponentExactPredicate allOf(final DataComponentMap components) {
      return new DataComponentExactPredicate(ImmutableList.copyOf(components));
   }

   public static DataComponentExactPredicate someOf(final DataComponentMap components, final DataComponentType<?>... types) {
      DataComponentExactPredicate.Builder result = new DataComponentExactPredicate.Builder();

      for (DataComponentType<?> type : types) {
         TypedDataComponent<?> value = components.getTyped(type);
         if (value != null) {
            result.expect(value);
         }
      }

      return result.build();
   }

   public boolean isEmpty() {
      return this.expectedComponents.isEmpty();
   }

   @Override
   public boolean equals(final Object obj) {
      return obj instanceof DataComponentExactPredicate predicate && this.expectedComponents.equals(predicate.expectedComponents);
   }

   @Override
   public int hashCode() {
      return this.expectedComponents.hashCode();
   }

   @Override
   public String toString() {
      return this.expectedComponents.toString();
   }

   public boolean test(final DataComponentGetter actualComponents) {
      for (TypedDataComponent<?> expected : this.expectedComponents) {
         Object actual = actualComponents.get(expected.type());
         if (!Objects.equals(expected.value(), actual)) {
            return false;
         }
      }

      return true;
   }

   public boolean alwaysMatches() {
      return this.expectedComponents.isEmpty();
   }

   public DataComponentPatch asPatch() {
      return DataComponentPatch.builder().set(this.expectedComponents).build();
   }

   public static class Builder {
      private final List<TypedDataComponent<?>> expectedComponents = new ArrayList<>();

      private Builder() {
      }

      public <T> DataComponentExactPredicate.Builder expect(final TypedDataComponent<T> value) {
         return this.expect(value.type(), value.value());
      }

      public <T> DataComponentExactPredicate.Builder expect(final DataComponentType<? super T> type, final T value) {
         for (TypedDataComponent<?> component : this.expectedComponents) {
            if (component.type() == type) {
               throw new IllegalArgumentException("Predicate already has component of type: '" + type + "'");
            }
         }

         this.expectedComponents.add(new TypedDataComponent<>(type, value));
         return this;
      }

      public DataComponentExactPredicate build() {
         return new DataComponentExactPredicate(List.copyOf(this.expectedComponents));
      }
   }
}
