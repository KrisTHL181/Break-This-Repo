package net.minecraft.util.parsing.packrat;

import org.jspecify.annotations.Nullable;

public interface Rule<S, T> {
   @Nullable T parse(ParseState<S> var1);

   static <S, T> Rule<S, T> fromTerm(Term<S> p_334127_, Rule.RuleAction<S, T> p_334890_) {
      return new Rule.WrappedTerm<>(p_334890_, p_334127_);
   }

   static <S, T> Rule<S, T> fromTerm(Term<S> p_336211_, Rule.SimpleRuleAction<S, T> p_332994_) {
      return new Rule.WrappedTerm<>(p_332994_, p_336211_);
   }

   @FunctionalInterface
   interface RuleAction<S, T> {
      @Nullable T run(ParseState<S> var1);
   }

   @FunctionalInterface
   interface SimpleRuleAction<S, T> extends Rule.RuleAction<S, T> {
      T run(Scope var1);

      @Override
      default T run(ParseState<S> p_392774_) {
         return this.run(p_392774_.scope());
      }
   }

   record WrappedTerm<S, T>(Rule.RuleAction<S, T> action, Term<S> child) implements Rule<S, T> {
      @Override
      public @Nullable T parse(ParseState<S> p_328860_) {
         Scope scope = p_328860_.scope();
         scope.pushFrame();

         Object object;
         try {
            if (!this.child.parse(p_328860_, scope, Control.UNBOUND)) {
               return null;
            }

            object = this.action.run(p_328860_);
         } finally {
            scope.popFrame();
         }

         return (T)object;
      }
   }
}
