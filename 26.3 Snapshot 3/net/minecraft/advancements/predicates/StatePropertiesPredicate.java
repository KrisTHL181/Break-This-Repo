package net.minecraft.advancements.predicates;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;

public record StatePropertiesPredicate(List<StatePropertiesPredicate.PropertyMatcher> properties) {
   private static final Codec<List<StatePropertiesPredicate.PropertyMatcher>> PROPERTIES_CODEC = Codec.unboundedMap(
         Codec.STRING, StatePropertiesPredicate.ValueMatcher.CODEC
      )
      .xmap(
         map -> map.entrySet()
            .stream()
            .map(entry -> new StatePropertiesPredicate.PropertyMatcher((String)entry.getKey(), (StatePropertiesPredicate.ValueMatcher)entry.getValue()))
            .toList(),
         properties -> properties.stream()
            .collect(Collectors.toMap(StatePropertiesPredicate.PropertyMatcher::name, StatePropertiesPredicate.PropertyMatcher::valueMatcher))
      );
   public static final Codec<StatePropertiesPredicate> CODEC = PROPERTIES_CODEC.xmap(StatePropertiesPredicate::new, StatePropertiesPredicate::properties);
   public static final StreamCodec<ByteBuf, StatePropertiesPredicate> STREAM_CODEC = StatePropertiesPredicate.PropertyMatcher.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(StatePropertiesPredicate::new, StatePropertiesPredicate::properties);

   public <S extends StateHolder<?, S>> boolean matches(final StateDefinition<?, S> definition, final S state) {
      for (StatePropertiesPredicate.PropertyMatcher matcher : this.properties) {
         if (!matcher.match(definition, state)) {
            return false;
         }
      }

      return true;
   }

   public boolean matches(final BlockState state) {
      return this.matches(state.getBlock().getStateDefinition(), state);
   }

   public boolean matches(final FluidState state) {
      return this.matches(state.getType().getStateDefinition(), state);
   }

   public Optional<String> checkState(final StateDefinition<?, ?> states) {
      for (StatePropertiesPredicate.PropertyMatcher property : this.properties) {
         Optional<String> unknownProperty = property.checkState(states);
         if (unknownProperty.isPresent()) {
            return unknownProperty;
         }
      }

      return Optional.empty();
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<StatePropertiesPredicate.PropertyMatcher> matchers = ImmutableList.builder();

      private Builder() {
      }

      public static StatePropertiesPredicate.Builder properties() {
         return new StatePropertiesPredicate.Builder();
      }

      public StatePropertiesPredicate.Builder hasProperty(final Property<?> property, final String value) {
         this.matchers.add(new StatePropertiesPredicate.PropertyMatcher(property.getName(), new StatePropertiesPredicate.ExactMatcher(value)));
         return this;
      }

      public StatePropertiesPredicate.Builder hasProperty(final Property<Integer> property, final int value) {
         return this.hasProperty(property, Integer.toString(value));
      }

      public StatePropertiesPredicate.Builder hasProperty(final Property<Boolean> property, final boolean value) {
         return this.hasProperty(property, Boolean.toString(value));
      }

      public <T extends Comparable<T> & StringRepresentable> StatePropertiesPredicate.Builder hasProperty(final Property<T> property, final T value) {
         return this.hasProperty(property, value.getSerializedName());
      }

      public Optional<StatePropertiesPredicate> build() {
         return Optional.of(new StatePropertiesPredicate(this.matchers.build()));
      }
   }

   private record ExactMatcher(String value) implements StatePropertiesPredicate.ValueMatcher {
      public static final Codec<StatePropertiesPredicate.ExactMatcher> CODEC = Codec.STRING
         .xmap(StatePropertiesPredicate.ExactMatcher::new, StatePropertiesPredicate.ExactMatcher::value);
      public static final StreamCodec<ByteBuf, StatePropertiesPredicate.ExactMatcher> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
         .map(StatePropertiesPredicate.ExactMatcher::new, StatePropertiesPredicate.ExactMatcher::value);

      @Override
      public <T extends Comparable<T>> boolean match(final StateHolder<?, ?> state, final Property<T> property) {
         T actualValue = state.getValue(property);
         Optional<T> typedExpected = property.getValue(this.value);
         return typedExpected.isPresent() && actualValue.compareTo(typedExpected.get()) == 0;
      }
   }

   private record PropertyMatcher(String name, StatePropertiesPredicate.ValueMatcher valueMatcher) {
      public static final StreamCodec<ByteBuf, StatePropertiesPredicate.PropertyMatcher> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8,
         StatePropertiesPredicate.PropertyMatcher::name,
         StatePropertiesPredicate.ValueMatcher.STREAM_CODEC,
         StatePropertiesPredicate.PropertyMatcher::valueMatcher,
         StatePropertiesPredicate.PropertyMatcher::new
      );

      public <S extends StateHolder<?, S>> boolean match(final StateDefinition<?, S> definition, final S state) {
         Property<?> property = definition.getProperty(this.name);
         return property != null && this.valueMatcher.match(state, property);
      }

      public Optional<String> checkState(final StateDefinition<?, ?> states) {
         Property<?> property = states.getProperty(this.name);
         return property != null ? Optional.empty() : Optional.of(this.name);
      }
   }

   private record RangedMatcher(Optional<String> minValue, Optional<String> maxValue) implements StatePropertiesPredicate.ValueMatcher {
      public static final Codec<StatePropertiesPredicate.RangedMatcher> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.STRING.optionalFieldOf("min").forGetter(StatePropertiesPredicate.RangedMatcher::minValue),
               Codec.STRING.optionalFieldOf("max").forGetter(StatePropertiesPredicate.RangedMatcher::maxValue)
            )
            .apply(i, StatePropertiesPredicate.RangedMatcher::new)
      );
      public static final StreamCodec<ByteBuf, StatePropertiesPredicate.RangedMatcher> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
         StatePropertiesPredicate.RangedMatcher::minValue,
         ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
         StatePropertiesPredicate.RangedMatcher::maxValue,
         StatePropertiesPredicate.RangedMatcher::new
      );

      @Override
      public <T extends Comparable<T>> boolean match(final StateHolder<?, ?> state, final Property<T> property) {
         T value = state.getValue(property);
         if (this.minValue.isPresent()) {
            Optional<T> typedMinValue = property.getValue(this.minValue.get());
            if (typedMinValue.isEmpty() || value.compareTo(typedMinValue.get()) < 0) {
               return false;
            }
         }

         if (this.maxValue.isPresent()) {
            Optional<T> typedMaxValue = property.getValue(this.maxValue.get());
            if (typedMaxValue.isEmpty() || value.compareTo(typedMaxValue.get()) > 0) {
               return false;
            }
         }

         return true;
      }
   }

   private interface ValueMatcher {
      Codec<StatePropertiesPredicate.ValueMatcher> CODEC = Codec.either(
            StatePropertiesPredicate.ExactMatcher.CODEC, StatePropertiesPredicate.RangedMatcher.CODEC
         )
         .xmap(Either::unwrap, matcher -> {
            if (matcher instanceof StatePropertiesPredicate.ExactMatcher exact) {
               return Either.left(exact);
            } else if (matcher instanceof StatePropertiesPredicate.RangedMatcher ranged) {
               return Either.right(ranged);
            } else {
               throw new UnsupportedOperationException();
            }
         });
      StreamCodec<ByteBuf, StatePropertiesPredicate.ValueMatcher> STREAM_CODEC = ByteBufCodecs.either(
            StatePropertiesPredicate.ExactMatcher.STREAM_CODEC, StatePropertiesPredicate.RangedMatcher.STREAM_CODEC
         )
         .map(Either::unwrap, matcher -> {
            if (matcher instanceof StatePropertiesPredicate.ExactMatcher exact) {
               return Either.left(exact);
            } else if (matcher instanceof StatePropertiesPredicate.RangedMatcher ranged) {
               return Either.right(ranged);
            } else {
               throw new UnsupportedOperationException();
            }
         });

      <T extends Comparable<T>> boolean match(StateHolder<?, ?> state, Property<T> property);
   }
}
