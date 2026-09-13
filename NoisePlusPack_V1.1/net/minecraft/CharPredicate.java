package net.minecraft;

import java.util.Objects;

@FunctionalInterface
public interface CharPredicate {
   boolean test(char var1);

   default CharPredicate and(CharPredicate p_178287_) {
      Objects.requireNonNull(p_178287_);
      return p_178295_ -> this.test(p_178295_) && p_178287_.test(p_178295_);
   }

   default CharPredicate negate() {
      return p_178285_ -> !this.test(p_178285_);
   }

   default CharPredicate or(CharPredicate p_178292_) {
      Objects.requireNonNull(p_178292_);
      return p_178290_ -> this.test(p_178290_) || p_178292_.test(p_178290_);
   }
}
