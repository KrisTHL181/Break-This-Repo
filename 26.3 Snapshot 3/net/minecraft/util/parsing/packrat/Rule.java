package net.minecraft.util.parsing.packrat;

import org.jspecify.annotations.Nullable;

public interface Rule<S, T> {
   @Nullable T parse(ParseState<S> state);

   static <S, T> Rule<S, T> fromTerm(final Term<S> child, final Rule.RuleAction<S, T> action) {
      return new Rule.WrappedTerm<>(action, child);
   }

   static <S, T> Rule<S, T> fromTerm(final Term<S> child, final Rule.SimpleRuleAction<S, T> action) {
      return new Rule.WrappedTerm<>(action, child);
   }

   @FunctionalInterface
   interface RuleAction<S, T> {
      @Nullable T run(ParseState<S> state);
   }

   @FunctionalInterface
   interface SimpleRuleAction<S, T> extends Rule.RuleAction<S, T> {
      T run(Scope ruleScope);

      @Override
      default T run(final ParseState<S> state) {
         return this.run(state.scope());
      }
   }

   record WrappedTerm<S, T>(Rule.RuleAction<S, T> action, Term<S> child) implements Rule<S, T> {
      @Override
      public @Nullable T parse(final ParseState<S> state) {
         Scope scope = state.scope();
         scope.pushFrame();

         try {
            return this.child.parse(state, scope, Control.UNBOUND) ? this.action.run(state) : null;
         } finally {
            scope.popFrame();
         }
      }
   }
}
