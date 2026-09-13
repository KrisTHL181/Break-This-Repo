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
         p_397402_ -> new DataComponentExactPredicate(p_397402_.entrySet().stream().map(TypedDataComponent::fromEntryUnchecked).collect(Collectors.toList())),
         p_397856_ -> p_397856_.expectedComponents
            .stream()
            .filter(p_397260_ -> !p_397260_.type().isTransient())
            .collect(Collectors.toMap(TypedDataComponent::type, TypedDataComponent::value))
      );
   public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentExactPredicate> STREAM_CODEC = TypedDataComponent.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(DataComponentExactPredicate::new, p_397055_ -> p_397055_.expectedComponents);
   public static final DataComponentExactPredicate EMPTY = new DataComponentExactPredicate(List.of());
   private final List<TypedDataComponent<?>> expectedComponents;

   DataComponentExactPredicate(List<TypedDataComponent<?>> p_392485_) {
      this.expectedComponents = p_392485_;
   }

   public static DataComponentExactPredicate.Builder builder() {
      return new DataComponentExactPredicate.Builder();
   }

   public static <T> DataComponentExactPredicate expect(DataComponentType<T> p_394016_, T p_397299_) {
      return new DataComponentExactPredicate(List.of(new TypedDataComponent<>(p_394016_, p_397299_)));
   }

   public static DataComponentExactPredicate allOf(DataComponentMap p_392635_) {
      return new DataComponentExactPredicate(ImmutableList.copyOf(p_392635_));
   }

   public static DataComponentExactPredicate someOf(DataComponentMap p_393446_, DataComponentType<?>... p_394636_) {
      DataComponentExactPredicate.Builder datacomponentexactpredicate$builder = new DataComponentExactPredicate.Builder();

      for (DataComponentType<?> datacomponenttype : p_394636_) {
         TypedDataComponent<?> typeddatacomponent = p_393446_.getTyped(datacomponenttype);
         if (typeddatacomponent != null) {
            datacomponentexactpredicate$builder.expect(typeddatacomponent);
         }
      }

      return datacomponentexactpredicate$builder.build();
   }

   public boolean isEmpty() {
      return this.expectedComponents.isEmpty();
   }

   @Override
   public boolean equals(Object p_393364_) {
      return p_393364_ instanceof DataComponentExactPredicate datacomponentexactpredicate
         && this.expectedComponents.equals(datacomponentexactpredicate.expectedComponents);
   }

   @Override
   public int hashCode() {
      return this.expectedComponents.hashCode();
   }

   @Override
   public String toString() {
      return this.expectedComponents.toString();
   }

   public boolean test(DataComponentGetter p_393043_) {
      for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
         Object object = p_393043_.get(typeddatacomponent.type());
         if (!Objects.equals(typeddatacomponent.value(), object)) {
            return false;
         }
      }

      return true;
   }

   public boolean alwaysMatches() {
      return this.expectedComponents.isEmpty();
   }

   public DataComponentPatch asPatch() {
      DataComponentPatch.Builder datacomponentpatch$builder = DataComponentPatch.builder();

      for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
         datacomponentpatch$builder.set(typeddatacomponent);
      }

      return datacomponentpatch$builder.build();
   }

   public static class Builder {
      private final List<TypedDataComponent<?>> expectedComponents = new ArrayList<>();

      Builder() {
      }

      public <T> DataComponentExactPredicate.Builder expect(TypedDataComponent<T> p_395145_) {
         return this.expect(p_395145_.type(), p_395145_.value());
      }

      public <T> DataComponentExactPredicate.Builder expect(DataComponentType<? super T> p_396654_, T p_395347_) {
         for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
            if (typeddatacomponent.type() == p_396654_) {
               throw new IllegalArgumentException("Predicate already has component of type: '" + p_396654_ + "'");
            }
         }

         this.expectedComponents.add(new TypedDataComponent<>(p_396654_, p_395347_));
         return this;
      }

      public DataComponentExactPredicate build() {
         return new DataComponentExactPredicate(List.copyOf(this.expectedComponents));
      }
   }
}
