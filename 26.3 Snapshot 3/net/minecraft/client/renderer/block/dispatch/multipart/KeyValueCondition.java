package net.minecraft.client.renderer.block.dispatch.multipart;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;

public record KeyValueCondition(Map<String, KeyValueCondition.Terms> tests) implements Condition {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<KeyValueCondition> CODEC = ExtraCodecs.nonEmptyMap(Codec.unboundedMap(Codec.STRING, KeyValueCondition.Terms.CODEC))
      .xmap(KeyValueCondition::new, KeyValueCondition::tests);

   @Override
   public <O, S extends StateHolder<O, S>> Predicate<S> instantiate(final StateDefinition<O, S> definition) {
      List<Predicate<S>> predicates = new ArrayList<>(this.tests.size());
      this.tests.forEach((key, valueTest) -> predicates.add(instantiate(definition, key, valueTest)));
      return Util.allOf(predicates);
   }

   private static <O, S extends StateHolder<O, S>> Predicate<S> instantiate(
      final StateDefinition<O, S> definition, final String key, final KeyValueCondition.Terms valueTest
   ) {
      Property<?> property = definition.getProperty(key);
      if (property == null) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "Unknown property '%s' on '%s'", key, definition.getOwner()));
      } else {
         return valueTest.instantiate(definition.getOwner(), property);
      }
   }

   public record Term(String value, boolean negated) {
      private static final String NEGATE = "!";

      public Term {
         if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty term");
         }
      }

      public static KeyValueCondition.Term parse(final String value) {
         return value.startsWith("!") ? new KeyValueCondition.Term(value.substring(1), true) : new KeyValueCondition.Term(value, false);
      }

      @Override
      public String toString() {
         return this.negated ? "!" + this.value : this.value;
      }
   }

   public record Terms(List<KeyValueCondition.Term> entries) {
      private static final char SEPARATOR = '|';
      private static final Joiner JOINER = Joiner.on('|');
      private static final Splitter SPLITTER = Splitter.on('|');
      private static final Codec<String> LEGACY_REPRESENTATION_CODEC = Codec.either(Codec.INT, Codec.BOOL)
         .flatComapMap(either -> (String)either.map(String::valueOf, String::valueOf), o -> DataResult.error(() -> "This codec can't be used for encoding"));
      public static final Codec<KeyValueCondition.Terms> CODEC = Codec.withAlternative(Codec.STRING, LEGACY_REPRESENTATION_CODEC)
         .comapFlatMap(KeyValueCondition.Terms::parse, KeyValueCondition.Terms::toString);

      public Terms {
         if (entries.isEmpty()) {
            throw new IllegalArgumentException("Empty value for property");
         }
      }

      public static DataResult<KeyValueCondition.Terms> parse(final String value) {
         List<KeyValueCondition.Term> terms = SPLITTER.splitToStream(value).map(KeyValueCondition.Term::parse).toList();
         if (terms.isEmpty()) {
            return DataResult.error(() -> "Empty value for property");
         }

         for (KeyValueCondition.Term entry : terms) {
            if (entry.value.isEmpty()) {
               return DataResult.error(() -> "Empty term in value '" + value + "'");
            }
         }

         return DataResult.success(new KeyValueCondition.Terms(terms));
      }

      @Override
      public String toString() {
         return JOINER.join(this.entries);
      }

      public <O, S extends StateHolder<O, S>, T extends Comparable<T>> Predicate<S> instantiate(final O owner, final Property<T> property) {
         Predicate<T> allowedValueTest = Util.anyOf(Lists.transform(this.entries, t -> this.instantiate(owner, property, t)));
         List<T> allowedValues = new ArrayList<>(property.getPossibleValues());
         int allValuesCount = allowedValues.size();
         allowedValues.removeIf(allowedValueTest.negate());
         int allowedValuesCount = allowedValues.size();
         if (allowedValuesCount == 0) {
            KeyValueCondition.LOGGER.warn("Condition {} for property {} on {} is always false", new Object[]{this, property.getName(), owner});
            return blockState -> false;
         }

         int rejectedValuesCount = allValuesCount - allowedValuesCount;
         if (rejectedValuesCount == 0) {
            KeyValueCondition.LOGGER.warn("Condition {} for property {} on {} is always true", new Object[]{this, property.getName(), owner});
            return blockState -> true;
         }

         boolean negate;
         List<T> valuesToMatch;
         if (allowedValuesCount <= rejectedValuesCount) {
            negate = false;
            valuesToMatch = allowedValues;
         } else {
            negate = true;
            List<T> rejectedValues = new ArrayList<>(property.getPossibleValues());
            rejectedValues.removeIf(allowedValueTest);
            valuesToMatch = rejectedValues;
         }

         if (valuesToMatch.size() == 1) {
            T expectedValue = (T)valuesToMatch.getFirst();
            return state -> {
               T value = state.getValue(property);
               return expectedValue.equals(value) ^ negate;
            };
         } else {
            return state -> {
               T value = state.getValue(property);
               return valuesToMatch.contains(value) ^ negate;
            };
         }
      }

      private <T extends Comparable<T>> T getValueOrThrow(final Object owner, final Property<T> property, final String input) {
         Optional<T> value = property.getValue(input);
         if (value.isEmpty()) {
            throw new RuntimeException(String.format(Locale.ROOT, "Unknown value '%s' for property '%s' on '%s' in '%s'", input, property, owner, this));
         } else {
            return value.get();
         }
      }

      private <T extends Comparable<T>> Predicate<T> instantiate(final Object owner, final Property<T> property, final KeyValueCondition.Term term) {
         T parsedValue = this.getValueOrThrow(owner, property, term.value);
         return term.negated ? value -> !value.equals(parsedValue) : value -> value.equals(parsedValue);
      }
   }
}
