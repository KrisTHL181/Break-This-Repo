package net.minecraft;

import java.util.Objects;

@FunctionalInterface
public interface CharPredicate {
   boolean test(char value);

   default CharPredicate and(final CharPredicate other) {
      Objects.requireNonNull(other);
      return value -> this.test(value) && other.test(value);
   }

   default CharPredicate negate() {
      return value -> !this.test(value);
   }

   default CharPredicate or(final CharPredicate other) {
      Objects.requireNonNull(other);
      return value -> this.test(value) || other.test(value);
   }
}
