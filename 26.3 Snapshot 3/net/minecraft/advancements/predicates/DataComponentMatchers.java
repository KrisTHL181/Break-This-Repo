package net.minecraft.advancements.predicates;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record DataComponentMatchers(DataComponentExactPredicate exact, Map<DataComponentPredicate.Type<?>, DataComponentPredicate> partial)
   implements Predicate<DataComponentGetter> {
   public static final DataComponentMatchers ANY = new DataComponentMatchers(DataComponentExactPredicate.EMPTY, Map.of());
   public static final MapCodec<DataComponentMatchers> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            DataComponentExactPredicate.CODEC.optionalFieldOf("components", DataComponentExactPredicate.EMPTY).forGetter(DataComponentMatchers::exact),
            DataComponentPredicate.CODEC.optionalFieldOf("predicates", Map.of()).forGetter(DataComponentMatchers::partial)
         )
         .apply(i, DataComponentMatchers::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentMatchers> STREAM_CODEC = StreamCodec.composite(
      DataComponentExactPredicate.STREAM_CODEC,
      DataComponentMatchers::exact,
      DataComponentPredicate.STREAM_CODEC,
      DataComponentMatchers::partial,
      DataComponentMatchers::new
   );

   public boolean test(final DataComponentGetter values) {
      if (!this.exact.test(values)) {
         return false;
      }

      for (DataComponentPredicate predicate : this.partial.values()) {
         if (!predicate.matches(values)) {
            return false;
         }
      }

      return true;
   }

   public boolean isEmpty() {
      return this.exact.isEmpty() && this.partial.isEmpty();
   }

   public static class Builder {
      private DataComponentExactPredicate exact = DataComponentExactPredicate.EMPTY;
      private final com.google.common.collect.ImmutableMap.Builder<DataComponentPredicate.Type<?>, DataComponentPredicate> partial = ImmutableMap.builder();

      private Builder() {
      }

      public static DataComponentMatchers.Builder components() {
         return new DataComponentMatchers.Builder();
      }

      public <T extends DataComponentType<?>> DataComponentMatchers.Builder any(final DataComponentType<?> type) {
         DataComponentPredicate.AnyValueType predicateType = DataComponentPredicate.AnyValueType.create(type);
         this.partial.put(predicateType, predicateType.predicate());
         return this;
      }

      public <T extends DataComponentPredicate> DataComponentMatchers.Builder partial(final DataComponentPredicate.Type<T> type, final T predicate) {
         this.partial.put(type, predicate);
         return this;
      }

      public DataComponentMatchers.Builder exact(final DataComponentExactPredicate exact) {
         this.exact = exact;
         return this;
      }

      public DataComponentMatchers build() {
         return new DataComponentMatchers(this.exact, this.partial.buildOrThrow());
      }
   }
}
