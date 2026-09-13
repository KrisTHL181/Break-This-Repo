package com.mojang.datafixers;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.functions.PointFreeRule;
import com.mojang.datafixers.types.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface TypeRewriteRule {
   <A> Optional<RewriteResult<A, ?>> rewrite(Type<A> var1);

   static TypeRewriteRule nop() {
      return TypeRewriteRule.Nop.INSTANCE;
   }

   static TypeRewriteRule seq(List<TypeRewriteRule> rules) {
      return new TypeRewriteRule.Seq(rules);
   }

   static TypeRewriteRule seq(TypeRewriteRule first, TypeRewriteRule second) {
      if (Objects.equals(first, nop())) {
         return second;
      } else {
         return Objects.equals(second, nop()) ? first : seq(ImmutableList.of(first, second));
      }
   }

   static TypeRewriteRule seq(TypeRewriteRule firstRule, TypeRewriteRule... rules) {
      if (rules.length == 0) {
         return firstRule;
      }

      int lastRule = rules.length - 1;
      TypeRewriteRule tail = rules[lastRule];

      while (lastRule > 0) {
         tail = seq(rules[--lastRule], tail);
      }

      return seq(firstRule, tail);
   }

   static TypeRewriteRule orElse(TypeRewriteRule first, TypeRewriteRule second) {
      return orElse(first, () -> second);
   }

   static TypeRewriteRule orElse(TypeRewriteRule first, Supplier<TypeRewriteRule> second) {
      return new TypeRewriteRule.OrElse(first, second);
   }

   static TypeRewriteRule all(TypeRewriteRule rule, boolean recurse, boolean checkIndex) {
      return new TypeRewriteRule.All(rule, recurse, checkIndex);
   }

   static TypeRewriteRule one(TypeRewriteRule rule) {
      return new TypeRewriteRule.One(rule);
   }

   static TypeRewriteRule once(TypeRewriteRule rule) {
      return orElse(rule, () -> one(once(rule)));
   }

   static TypeRewriteRule checkOnce(TypeRewriteRule rule, Consumer<Type<?>> onFail) {
      return rule;
   }

   static TypeRewriteRule everywhere(TypeRewriteRule rule, PointFreeRule optimizationRule, boolean recurse, boolean checkIndex) {
      return new TypeRewriteRule.Everywhere(rule, optimizationRule, recurse, checkIndex);
   }

   static <B> TypeRewriteRule ifSame(Type<B> targetType, RewriteResult<B, ?> value) {
      return new TypeRewriteRule.IfSame<>(targetType, value);
   }

   class All implements TypeRewriteRule {
      private final TypeRewriteRule rule;
      private final boolean recurse;
      private final boolean checkIndex;
      private final int hashCode;

      public All(TypeRewriteRule rule, boolean recurse, boolean checkIndex) {
         this.rule = rule;
         this.recurse = recurse;
         this.checkIndex = checkIndex;
         this.hashCode = Objects.hash(rule, recurse, checkIndex);
      }

      @Override
      public <A> Optional<RewriteResult<A, ?>> rewrite(Type<A> type) {
         return Optional.of(type.all(this.rule, this.recurse, this.checkIndex));
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else {
            return !(obj instanceof TypeRewriteRule.All that)
               ? false
               : Objects.equals(this.rule, that.rule) && this.recurse == that.recurse && this.checkIndex == that.checkIndex;
         }
      }

      @Override
      public int hashCode() {
         return this.hashCode;
      }
   }

   record CheckOnce(TypeRewriteRule rule, Consumer<Type<?>> onFail) implements TypeRewriteRule {
      @Override
      public <A> Optional<RewriteResult<A, ?>> rewrite(Type<A> type) {
         Optional<RewriteResult<A, ?>> result = this.rule.rewrite(type);
         if (!result.isPresent() || result.get().view().isNop()) {
            this.onFail.accept(type);
         }

         return result;
      }
   }

   class Everywhere implements TypeRewriteRule {
      protected final TypeRewriteRule rule;
      protected final PointFreeRule optimizationRule;
      protected final boolean recurse;
      private final boolean checkIndex;
      private final int hashCode;

      public Everywhere(TypeRewriteRule rule, PointFreeRule optimizationRule, boolean recurse, boolean checkIndex) {
         this.rule = rule;
         this.optimizationRule = optimizationRule;
         this.recurse = recurse;
         this.checkIndex = checkIndex;
         this.hashCode = Objects.hash(rule, optimizationRule, recurse, checkIndex);
      }

      @Override
      public <A> Optional<RewriteResult<A, ?>> rewrite(Type<A> type) {
         return type.everywhere(this.rule, this.optimizationRule, this.recurse, this.checkIndex);
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else {
            return !(obj instanceof TypeRewriteRule.Everywhere that)
               ? false
               : Objects.equals(this.rule, that.rule)
                  && Objects.equals(this.optimizationRule, that.optimizationRule)
                  && this.recurse == that.recurse
                  && this.checkIndex == that.checkIndex;
         }
      }

      @Override
      public int hashCode() {
         return this.hashCode;
      }
   }

   class IfSame<B> implements TypeRewriteRule {
      private final Type<B> targetType;
      private final RewriteResult<B, ?> value;
      private final int hashCode;

      public IfSame(Type<B> targetType, RewriteResult<B, ?> value) {
         this.targetType = targetType;
         this.value = value;
         this.hashCode = Objects.hash(targetType, value);
      }

      @Override
      public <A> Optional<RewriteResult<A, ?>> rewrite(Type<A> type) {
         return type.ifSame(this.targetType, this.value);
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else {
            return !(obj instanceof TypeRewriteRule.IfSame<?> that)
               ? false
               : Objects.equals(this.targetType, that.targetType) && Objects.equals(this.value, that.value);
         }
      }

      @Override
      public int hashCode() {
         return this.hashCode;
      }
   }

   enum Nop implements TypeRewriteRule, Supplier<TypeRewriteRule> {
      INSTANCE;

      @Override
      public <A> Optional<RewriteResult<A, ?>> rewrite(Type<A> type) {
         return Optional.of(RewriteResult.nop(type));
      }

      public TypeRewriteRule get() {
         return this;
      }
   }

   record One(TypeRewriteRule rule) implements TypeRewriteRule {
      @Override
      public <A> Optional<RewriteResult<A, ?>> rewrite(Type<A> type) {
         return type.one(this.rule);
      }
   }

   final class OrElse implements TypeRewriteRule {
      protected final TypeRewriteRule first;
      protected final Supplier<TypeRewriteRule> second;
      private final int hashCode;

      public OrElse(TypeRewriteRule first, Supplier<TypeRewriteRule> second) {
         this.first = first;
         this.second = second;
         this.hashCode = Objects.hash(first, second);
      }

      @Override
      public <A> Optional<RewriteResult<A, ?>> rewrite(Type<A> type) {
         Optional<RewriteResult<A, ?>> view = this.first.rewrite(type);
         return view.isPresent() ? view : this.second.get().rewrite(type);
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else {
            return !(obj instanceof TypeRewriteRule.OrElse that) ? false : Objects.equals(this.first, that.first) && Objects.equals(this.second, that.second);
         }
      }

      @Override
      public int hashCode() {
         return this.hashCode;
      }
   }

   final class Seq implements TypeRewriteRule {
      protected final List<TypeRewriteRule> rules;
      private final int hashCode;

      public Seq(List<TypeRewriteRule> rules) {
         this.rules = ImmutableList.copyOf(rules);
         this.hashCode = this.rules.hashCode();
      }

      @Override
      public <A> Optional<RewriteResult<A, ?>> rewrite(Type<A> type) {
         RewriteResult<A, ?> result = RewriteResult.nop(type);

         for (TypeRewriteRule rule : this.rules) {
            Optional<RewriteResult<A, ?>> newResult = this.cap1(rule, result);
            if (!newResult.isPresent()) {
               return Optional.empty();
            }

            result = newResult.get();
         }

         return Optional.of(result);
      }

      protected <A, B> Optional<RewriteResult<A, ?>> cap1(TypeRewriteRule rule, RewriteResult<A, B> f) {
         return rule.rewrite(f.view().newType()).map(s -> s.compose(f));
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else {
            return !(obj instanceof TypeRewriteRule.Seq that) ? false : Objects.equals(this.rules, that.rules);
         }
      }

      @Override
      public int hashCode() {
         return this.hashCode;
      }
   }
}
