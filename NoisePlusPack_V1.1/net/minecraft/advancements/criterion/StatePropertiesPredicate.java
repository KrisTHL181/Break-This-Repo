package net.minecraft.advancements.criterion;

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
         p_450741_ -> p_450741_.entrySet()
            .stream()
            .map(
               p_460817_ -> new StatePropertiesPredicate.PropertyMatcher(
                  (String)p_460817_.getKey(), (StatePropertiesPredicate.ValueMatcher)p_460817_.getValue()
               )
            )
            .toList(),
         p_460358_ -> p_460358_.stream()
            .collect(Collectors.toMap(StatePropertiesPredicate.PropertyMatcher::name, StatePropertiesPredicate.PropertyMatcher::valueMatcher))
      );
   public static final Codec<StatePropertiesPredicate> CODEC = PROPERTIES_CODEC.xmap(StatePropertiesPredicate::new, StatePropertiesPredicate::properties);
   public static final StreamCodec<ByteBuf, StatePropertiesPredicate> STREAM_CODEC = StatePropertiesPredicate.PropertyMatcher.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(StatePropertiesPredicate::new, StatePropertiesPredicate::properties);

   public <S extends StateHolder<?, S>> boolean matches(StateDefinition<?, S> p_452522_, S p_457426_) {
      for (StatePropertiesPredicate.PropertyMatcher statepropertiespredicate$propertymatcher : this.properties) {
         if (!statepropertiespredicate$propertymatcher.match(p_452522_, p_457426_)) {
            return false;
         }
      }

      return true;
   }

   public boolean matches(BlockState p_450390_) {
      return this.matches(p_450390_.getBlock().getStateDefinition(), p_450390_);
   }

   public boolean matches(FluidState p_460130_) {
      return this.matches(p_460130_.getType().getStateDefinition(), p_460130_);
   }

   public Optional<String> checkState(StateDefinition<?, ?> p_452100_) {
      for (StatePropertiesPredicate.PropertyMatcher statepropertiespredicate$propertymatcher : this.properties) {
         Optional<String> optional = statepropertiespredicate$propertymatcher.checkState(p_452100_);
         if (optional.isPresent()) {
            return optional;
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

      public StatePropertiesPredicate.Builder hasProperty(Property<?> p_456470_, String p_453902_) {
         this.matchers.add(new StatePropertiesPredicate.PropertyMatcher(p_456470_.getName(), new StatePropertiesPredicate.ExactMatcher(p_453902_)));
         return this;
      }

      public StatePropertiesPredicate.Builder hasProperty(Property<Integer> p_456086_, int p_455518_) {
         return this.hasProperty(p_456086_, Integer.toString(p_455518_));
      }

      public StatePropertiesPredicate.Builder hasProperty(Property<Boolean> p_460679_, boolean p_454739_) {
         return this.hasProperty(p_460679_, Boolean.toString(p_454739_));
      }

      public <T extends Comparable<T> & StringRepresentable> StatePropertiesPredicate.Builder hasProperty(Property<T> p_454593_, T p_453103_) {
         return this.hasProperty(p_454593_, p_453103_.getSerializedName());
      }

      public Optional<StatePropertiesPredicate> build() {
         return Optional.of(new StatePropertiesPredicate(this.matchers.build()));
      }
   }

   record ExactMatcher(String value) implements StatePropertiesPredicate.ValueMatcher {
      public static final Codec<StatePropertiesPredicate.ExactMatcher> CODEC = Codec.STRING
         .xmap(StatePropertiesPredicate.ExactMatcher::new, StatePropertiesPredicate.ExactMatcher::value);
      public static final StreamCodec<ByteBuf, StatePropertiesPredicate.ExactMatcher> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
         .map(StatePropertiesPredicate.ExactMatcher::new, StatePropertiesPredicate.ExactMatcher::value);

      @Override
      public <T extends Comparable<T>> boolean match(StateHolder<?, ?> p_460310_, Property<T> p_458222_) {
         T t = p_460310_.getValue(p_458222_);
         Optional<T> optional = p_458222_.getValue(this.value);
         return optional.isPresent() && t.compareTo(optional.get()) == 0;
      }
   }

   record PropertyMatcher(String name, StatePropertiesPredicate.ValueMatcher valueMatcher) {
      public static final StreamCodec<ByteBuf, StatePropertiesPredicate.PropertyMatcher> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8,
         StatePropertiesPredicate.PropertyMatcher::name,
         StatePropertiesPredicate.ValueMatcher.STREAM_CODEC,
         StatePropertiesPredicate.PropertyMatcher::valueMatcher,
         StatePropertiesPredicate.PropertyMatcher::new
      );

      public <S extends StateHolder<?, S>> boolean match(StateDefinition<?, S> p_458162_, S p_456195_) {
         Property<?> property = p_458162_.getProperty(this.name);
         return property != null && this.valueMatcher.match(p_456195_, property);
      }

      public Optional<String> checkState(StateDefinition<?, ?> p_458375_) {
         Property<?> property = p_458375_.getProperty(this.name);
         return property != null ? Optional.empty() : Optional.of(this.name);
      }
   }

   record RangedMatcher(Optional<String> minValue, Optional<String> maxValue) implements StatePropertiesPredicate.ValueMatcher {
      public static final Codec<StatePropertiesPredicate.RangedMatcher> CODEC = RecordCodecBuilder.create(
         p_452808_ -> p_452808_.group(
               Codec.STRING.optionalFieldOf("min").forGetter(StatePropertiesPredicate.RangedMatcher::minValue),
               Codec.STRING.optionalFieldOf("max").forGetter(StatePropertiesPredicate.RangedMatcher::maxValue)
            )
            .apply(p_452808_, StatePropertiesPredicate.RangedMatcher::new)
      );
      public static final StreamCodec<ByteBuf, StatePropertiesPredicate.RangedMatcher> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
         StatePropertiesPredicate.RangedMatcher::minValue,
         ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
         StatePropertiesPredicate.RangedMatcher::maxValue,
         StatePropertiesPredicate.RangedMatcher::new
      );

      @Override
      public <T extends Comparable<T>> boolean match(StateHolder<?, ?> p_454563_, Property<T> p_450296_) {
         T t = p_454563_.getValue(p_450296_);
         if (this.minValue.isPresent()) {
            Optional<T> optional = p_450296_.getValue(this.minValue.get());
            if (optional.isEmpty() || t.compareTo(optional.get()) < 0) {
               return false;
            }
         }

         if (this.maxValue.isPresent()) {
            Optional<T> optional1 = p_450296_.getValue(this.maxValue.get());
            if (optional1.isEmpty() || t.compareTo(optional1.get()) > 0) {
               return false;
            }
         }

         return true;
      }
   }

   interface ValueMatcher {
      Codec<StatePropertiesPredicate.ValueMatcher> CODEC = Codec.either(
            StatePropertiesPredicate.ExactMatcher.CODEC, StatePropertiesPredicate.RangedMatcher.CODEC
         )
         .xmap(Either::unwrap, p_453542_ -> {
            if (p_453542_ instanceof StatePropertiesPredicate.ExactMatcher statepropertiespredicate$exactmatcher) {
               return Either.left(statepropertiespredicate$exactmatcher);
            } else if (p_453542_ instanceof StatePropertiesPredicate.RangedMatcher statepropertiespredicate$rangedmatcher) {
               return Either.right(statepropertiespredicate$rangedmatcher);
            } else {
               throw new UnsupportedOperationException();
            }
         });
      StreamCodec<ByteBuf, StatePropertiesPredicate.ValueMatcher> STREAM_CODEC = ByteBufCodecs.either(
            StatePropertiesPredicate.ExactMatcher.STREAM_CODEC, StatePropertiesPredicate.RangedMatcher.STREAM_CODEC
         )
         .map(Either::unwrap, p_453336_ -> {
            if (p_453336_ instanceof StatePropertiesPredicate.ExactMatcher statepropertiespredicate$exactmatcher) {
               return Either.left(statepropertiespredicate$exactmatcher);
            } else if (p_453336_ instanceof StatePropertiesPredicate.RangedMatcher statepropertiespredicate$rangedmatcher) {
               return Either.right(statepropertiespredicate$rangedmatcher);
            } else {
               throw new UnsupportedOperationException();
            }
         });

      <T extends Comparable<T>> boolean match(StateHolder<?, ?> var1, Property<T> var2);
   }
}
