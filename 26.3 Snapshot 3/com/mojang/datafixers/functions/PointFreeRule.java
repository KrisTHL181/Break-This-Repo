package com.mojang.datafixers.functions;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.TypedOptic;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.optics.Optics;
import com.mojang.datafixers.types.Func;
import com.mojang.datafixers.types.constant.EmptyPart;
import com.mojang.datafixers.types.families.Algebra;
import com.mojang.datafixers.types.families.ListAlgebra;
import com.mojang.datafixers.types.families.RecursiveTypeFamily;
import com.mojang.datafixers.types.templates.Product;
import com.mojang.datafixers.types.templates.Sum;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public interface PointFreeRule {
   <A> Optional<? extends PointFree<A>> rewrite(PointFree<A> var1);

   default <A> PointFree<A> rewriteOrNop(PointFree<A> expr) {
      return DataFixUtils.orElse(this.rewrite(expr), expr);
   }

   static PointFreeRule nop() {
      return PointFreeRule.Nop.INSTANCE;
   }

   static PointFreeRule seq(PointFreeRule... rules) {
      return new PointFreeRule.Seq(rules);
   }

   static PointFreeRule choice(PointFreeRule... rules) {
      if (rules.length == 1) {
         return rules[0];
      } else {
         return rules.length == 2 ? new PointFreeRule.Choice2(rules[0], rules[1]) : new PointFreeRule.Choice(rules);
      }
   }

   static PointFreeRule all(PointFreeRule rule) {
      return new PointFreeRule.All(rule);
   }

   static PointFreeRule one(PointFreeRule rule) {
      return new PointFreeRule.One(rule);
   }

   static PointFreeRule once(PointFreeRule rule) {
      return new PointFreeRule.Once(rule);
   }

   static PointFreeRule many(PointFreeRule rule) {
      return new PointFreeRule.Many(rule);
   }

   static PointFreeRule everywhere(PointFreeRule topDown, PointFreeRule bottomUp) {
      return new PointFreeRule.Everywhere(topDown, bottomUp);
   }

   record All(PointFreeRule rule) implements PointFreeRule {
      @Override
      public <A> Optional<? extends PointFree<A>> rewrite(PointFree<A> expr) {
         return expr.all(this.rule);
      }
   }

   enum AppNest implements PointFreeRule {
      INSTANCE;

      @Override
      public <A> Optional<? extends PointFree<A>> rewrite(PointFree<A> expr) {
         return expr instanceof Apply<?, ?> applyFirst && applyFirst.arg instanceof Apply<?, ?> applySecond
            ? Optional.of(Functions.app(this.compose(applyFirst.func, applySecond.func), (PointFree<A>)applySecond.arg))
            : Optional.empty();
      }

      private <A, B, C> PointFree<Function<A, C>> compose(PointFree<? extends Function<?, ?>> first, PointFree<? extends Function<?, ?>> second) {
         return first instanceof ProfunctorTransformer<?, ?, ?, ?> firstOptic && second instanceof ProfunctorTransformer<?, ?, ?, ?> secondOptic
            ? this.cap(firstOptic, secondOptic)
            : Functions.comp((PointFree<Function<?, C>>)first, (PointFree<Function<A, ?>>)second);
      }

      private <R, X, Y, S, T, A, B> R cap(ProfunctorTransformer<X, Y, ?, ?> first, ProfunctorTransformer<S, T, A, B> second) {
         ProfunctorTransformer<X, Y, S, T> firstCasted = (ProfunctorTransformer<X, Y, S, T>)first;
         return (R)Functions.<X, Y, A, B>profunctorTransformer(firstCasted.optic.compose(second.optic));
      }
   }

   enum BangEta implements PointFreeRule {
      INSTANCE;

      @Override
      public <A> Optional<? extends PointFree<A>> rewrite(PointFree<A> expr) {
         if (expr instanceof Bang) {
            return Optional.empty();
         } else {
            return expr.type() instanceof Func<?, ?> func && func.second() instanceof EmptyPart
               ? Optional.of((PointFree<A>)Functions.bang(func.first()))
               : Optional.empty();
         }
      }
   }

   enum CataFuseDifferent implements PointFreeRule.CompRewrite {
      INSTANCE;

      @Override
      public Optional<? extends PointFree<? extends Function<?, ?>>> doRewrite(
         PointFree<? extends Function<?, ?>> first, PointFree<? extends Function<?, ?>> second
      ) {
         if (first instanceof Fold<?, ?> firstFold && second instanceof Fold<?, ?> secondFold) {
            RecursiveTypeFamily family = firstFold.aType.family();
            if (firstFold.index == secondFold.index && Objects.equals(family, secondFold.aType.family())) {
               RecursiveTypeFamily newFamily = firstFold.bType.family();
               List<RewriteResult<?, ?>> newAlgebra = Lists.newArrayList();
               BitSet firstModifies = new BitSet(family.size());
               BitSet secondModifies = new BitSet(family.size());

               for (int i = 0; i < family.size(); i++) {
                  RewriteResult<?, ?> firstAlgFunc = firstFold.algebra.apply(i);
                  RewriteResult<?, ?> secondAlgFunc = secondFold.algebra.apply(i);
                  boolean firstId = firstAlgFunc.view().isNop();
                  boolean secondId = secondAlgFunc.view().isNop();
                  if (!firstId && !secondId) {
                     return Optional.empty();
                  }

                  firstModifies.set(i, !firstId);
                  secondModifies.set(i, !secondId);
               }

               for (int i = 0; i < family.size(); i++) {
                  RewriteResult<?, ?> firstAlgFunc = firstFold.algebra.apply(i);
                  RewriteResult<?, ?> secondAlgFunc = secondFold.algebra.apply(i);
                  if (firstAlgFunc.recData().intersects(secondModifies) || secondAlgFunc.recData().intersects(firstModifies)) {
                     return Optional.empty();
                  }

                  if (firstAlgFunc.view().isNop()) {
                     newAlgebra.add(secondAlgFunc);
                  } else {
                     newAlgebra.add(firstAlgFunc);
                  }
               }

               Algebra algebra = new ListAlgebra("FusedDifferent", newAlgebra);
               return Optional.of(family.fold(algebra, newFamily).apply(firstFold.index).view().function());
            }
         }

         return Optional.empty();
      }
   }

   enum CataFuseSame implements PointFreeRule.CompRewrite {
      INSTANCE;

      @Override
      public Optional<? extends PointFree<? extends Function<?, ?>>> doRewrite(
         PointFree<? extends Function<?, ?>> first, PointFree<? extends Function<?, ?>> second
      ) {
         if (first instanceof Fold<?, ?> firstFold && second instanceof Fold<?, ?> secondFold) {
            RecursiveTypeFamily family = firstFold.aType.family();
            if (firstFold.index == secondFold.index && Objects.equals(family, secondFold.aType.family())) {
               RecursiveTypeFamily newFamily = firstFold.bType.family();
               List<RewriteResult<?, ?>> newAlgebra = Lists.newArrayList();
               boolean foundOne = false;

               for (int i = 0; i < family.size(); i++) {
                  RewriteResult<?, ?> firstAlgFunc = firstFold.algebra.apply(i);
                  RewriteResult<?, ?> secondAlgFunc = secondFold.algebra.apply(i);
                  boolean firstId = firstAlgFunc.view().isNop();
                  boolean secondId = secondAlgFunc.view().isNop();
                  if (firstId && secondId) {
                     newAlgebra.add(firstAlgFunc);
                  } else {
                     if (foundOne || firstId || secondId) {
                        return Optional.empty();
                     }

                     newAlgebra.add(this.getCompose(firstAlgFunc, secondAlgFunc));
                     foundOne = true;
                  }
               }

               Algebra algebra = new ListAlgebra("FusedSame", newAlgebra);
               return Optional.of(family.fold(algebra, newFamily).apply(firstFold.index).view().function());
            }
         }

         return Optional.empty();
      }

      private <B> RewriteResult<?, ?> getCompose(RewriteResult<B, ?> firstAlgFunc, RewriteResult<?, ?> secondAlgFunc) {
         return firstAlgFunc.compose((RewriteResult<?, B>)secondAlgFunc);
      }
   }

   record Choice(PointFreeRule[] rules) implements PointFreeRule {
      @Override
      public <A> Optional<? extends PointFree<A>> rewrite(PointFree<A> expr) {
         for (PointFreeRule rule : this.rules) {
            Optional<? extends PointFree<A>> view = rule.rewrite(expr);
            if (view.isPresent()) {
               return view;
            }
         }

         return Optional.empty();
      }

      @Override
      public boolean equals(Object obj) {
         return obj == this ? true : obj instanceof PointFreeRule.Choice that && Arrays.equals(this.rules, that.rules);
      }

      @Override
      public int hashCode() {
         return Arrays.hashCode(this.rules);
      }
   }

   record Choice2(PointFreeRule first, PointFreeRule second) implements PointFreeRule {
      @Override
      public <A> Optional<? extends PointFree<A>> rewrite(PointFree<A> expr) {
         Optional<? extends PointFree<A>> view = this.first.rewrite(expr);
         return view.isPresent() ? view : this.second.rewrite(expr);
      }
   }

   interface CompRewrite extends PointFreeRule {
      static PointFreeRule.CompRewrite together(PointFreeRule.CompRewrite... rules) {
         return (first, second) -> {
            for (PointFreeRule.CompRewrite rule : rules) {
               Optional<? extends PointFree<? extends Function<?, ?>>> view = rule.doRewrite(first, second);
               if (view.isPresent()) {
                  return view;
               }
            }

            return Optional.empty();
         };
      }

      @Override
      default <A> Optional<? extends PointFree<A>> rewrite(PointFree<A> expr) {
         return expr instanceof Comp<?, ?> comp
            ? this.rewrite(comp.functions)
               .map(rewrite -> (PointFree<A>)(rewrite.length == 1 ? rewrite[0] : new Comp((PointFree<? extends Function<?, ?>>[])rewrite)))
            : Optional.empty();
      }

      private Optional<PointFree<? extends Function<?, ?>>[]> rewrite(PointFree<? extends Function<?, ?>>[] functions) {
         Deque<PointFree<? extends Function<?, ?>>> result = new ArrayDeque<>(functions.length);
         boolean rewritten = false;
         Deque<PointFree<? extends Function<?, ?>>> queue = new ArrayDeque<>(functions.length);
         Collections.addAll(queue, functions);

         while (!queue.isEmpty()) {
            PointFree<? extends Function<?, ?>> next = queue.removeFirst();
            PointFree<? extends Function<?, ?>> last = result.peekLast();
            Optional<? extends PointFree<? extends Function<?, ?>>> rewrite = last != null ? this.doRewrite(last, next) : Optional.empty();
            if (rewrite.isPresent()) {
               result.removeLast();
               addFirst(queue, (PointFree<? extends Function<?, ?>>)rewrite.get());
               rewritten = true;
            } else {
               result.add(next);
            }
         }

         return rewritten ? Optional.of(result.toArray(PointFree[]::new)) : Optional.empty();
      }

      private static void addFirst(Deque<PointFree<? extends Function<?, ?>>> queue, PointFree<? extends Function<?, ?>> function) {
         if (function instanceof Comp<?, ?> comp) {
            for (int i = comp.functions.length - 1; i >= 0; i--) {
               queue.addFirst(comp.functions[i]);
            }
         } else {
            queue.addFirst(function);
         }
      }

      Optional<? extends PointFree<? extends Function<?, ?>>> doRewrite(PointFree<? extends Function<?, ?>> var1, PointFree<? extends Function<?, ?>> var2);
   }

   record Everywhere(PointFreeRule topDown, PointFreeRule bottomUp) implements PointFreeRule {
      @Override
      public <A> Optional<? extends PointFree<A>> rewrite(PointFree<A> expr) {
         PointFree<A> topDown = this.topDown.rewriteOrNop(expr);
         PointFree<A> all = DataFixUtils.orElse(topDown.all(this), topDown);
         PointFree<A> bottomUp = this.bottomUp.rewriteOrNop(all);
         return Optional.of(bottomUp);
      }
   }

   enum LensAppId implements PointFreeRule {
      INSTANCE;

      @Override
      public <A> Optional<? extends PointFree<A>> rewrite(PointFree<A> expr) {
         if (expr instanceof Apply<?, A> apply) {
            PointFree<? extends Function<?, A>> func = apply.func;
            if (func instanceof ProfunctorTransformer && Functions.isId(apply.arg)) {
               return Optional.of(Functions.id(((Func)apply.type()).first()));
            }
         }

         return Optional.empty();
      }
   }

   enum LensComp implements PointFreeRule.CompRewrite {
      INSTANCE;

      @Override
      public Optional<? extends PointFree<? extends Function<?, ?>>> doRewrite(
         PointFree<? extends Function<?, ?>> first, PointFree<? extends Function<?, ?>> second
      ) {
         if (first instanceof Apply<?, ?> applyFirst && second instanceof Apply<?, ?> applySecond) {
            PointFree<? extends Function<?, ?>> firstFunc = applyFirst.func;
            PointFree<? extends Function<?, ?>> secondFunc = applySecond.func;
            if (firstFunc instanceof ProfunctorTransformer<?, ?, ?, ?> transformerFirst
               && secondFunc instanceof ProfunctorTransformer<?, ?, ?, ?> transformerSecond) {
               List<? extends TypedOptic.Element<?, ?, ?, ?>> decomposedFirst = transformerFirst.optic.elements();
               List<? extends TypedOptic.Element<?, ?, ?, ?>> decomposedSecond = transformerSecond.optic.elements();
               int prefixSize = findCommonPrefix(decomposedFirst, decomposedSecond);
               if (prefixSize == 0) {
                  return Optional.empty();
               }

               if (prefixSize == decomposedFirst.size() && prefixSize == decomposedSecond.size()) {
                  return Optional.of(this.capApp(transformerFirst.optic, this.capComp(applyFirst.arg, applySecond.arg)));
               }

               Set<TypeToken<? extends K1>> bounds = Sets.union(transformerFirst.optic.bounds(), transformerSecond.optic.bounds());
               TypedOptic<?, ?, ?, ?> prefix = new TypedOptic(bounds, decomposedFirst.subList(0, prefixSize));
               PointFree<?> firstFork = this.capApp(new TypedOptic(bounds, decomposedFirst.subList(prefixSize, decomposedFirst.size())), applyFirst.arg);
               PointFree<?> secondFork = this.capApp(new TypedOptic(bounds, decomposedSecond.subList(prefixSize, decomposedSecond.size())), applySecond.arg);
               return Optional.of(this.capApp(prefix, this.capComp(firstFork, secondFork)));
            }
         }

         return Optional.empty();
      }

      private static int findCommonPrefix(List<? extends TypedOptic.Element<?, ?, ?, ?>> first, List<? extends TypedOptic.Element<?, ?, ?, ?>> second) {
         int size = Math.min(first.size(), second.size());

         for (int i = 0; i < size; i++) {
            if (!first.get(i).optic().equals(second.get(i).optic())) {
               return i;
            }
         }

         return size;
      }

      private <A, B, C> PointFree<Function<A, C>> capComp(PointFree<?> f1, PointFree<?> f2) {
         return Functions.comp((PointFree<Function<B, C>>)f1, (PointFree<Function<A, B>>)f2);
      }

      private <R, A, B, S, T> PointFree<R> capApp(TypedOptic<S, T, A, B> optic, PointFree<?> f) {
         return (PointFree<R>)(optic.elements().isEmpty() ? f : Functions.app(new ProfunctorTransformer<>(optic), (PointFree<Function<A, B>>)f));
      }
   }

   record Many(PointFreeRule rule) implements PointFreeRule {
      @Override
      public <A> Optional<? extends PointFree<A>> rewrite(PointFree<A> expr) {
         Optional<? extends PointFree<A>> result = Optional.of(expr);

         while (true) {
            Optional<? extends PointFree<A>> newResult = result.flatMap(this.rule::rewrite);
            if (newResult.isEmpty()) {
               return result;
            }

            result = newResult;
         }
      }
   }

   enum Nop implements PointFreeRule, Supplier<PointFreeRule> {
      INSTANCE;

      @Override
      public <A> Optional<PointFree<A>> rewrite(PointFree<A> expr) {
         return Optional.of(expr);
      }

      public PointFreeRule get() {
         return this;
      }
   }

   record Once(PointFreeRule rule) implements PointFreeRule {
      @Override
      public <A> Optional<? extends PointFree<A>> rewrite(PointFree<A> expr) {
         Optional<? extends PointFree<A>> view = this.rule.rewrite(expr);
         return view.isPresent() ? view : expr.one(this);
      }
   }

   record One(PointFreeRule rule) implements PointFreeRule {
      @Override
      public <A> Optional<? extends PointFree<A>> rewrite(PointFree<A> expr) {
         return expr.one(this.rule);
      }
   }

   record Seq(PointFreeRule[] rules) implements PointFreeRule {
      @Override
      public <A> Optional<? extends PointFree<A>> rewrite(PointFree<A> expr) {
         PointFree<A> result = expr;

         for (PointFreeRule rule : this.rules) {
            result = rule.rewriteOrNop(result);
         }

         return Optional.of(result);
      }

      @Override
      public boolean equals(Object obj) {
         return obj == this ? true : obj instanceof PointFreeRule.Seq that && Arrays.equals(this.rules, that.rules);
      }

      @Override
      public int hashCode() {
         return Arrays.hashCode(this.rules);
      }
   }

   // ===== 修改：SortInj 和 SortProj 中的强制转换，使用双重转换绕过类型检查 =====
   enum SortInj implements PointFreeRule.CompRewrite {
      INSTANCE;

      @Override
      public Optional<? extends PointFree<? extends Function<?, ?>>> doRewrite(
         PointFree<? extends Function<?, ?>> first, PointFree<? extends Function<?, ?>> second
      ) {
         if (first instanceof Apply<?, ?> applyFirst && second instanceof Apply<?, ?> applySecond) {
            PointFree<? extends Function<?, ?>> firstFunc = applyFirst.func;
            PointFree<? extends Function<?, ?>> secondFunc = applySecond.func;
            if (firstFunc instanceof ProfunctorTransformer<?, ?, ?, ?> firstOptic && secondFunc instanceof ProfunctorTransformer<?, ?, ?, ?> secondOptic) {
               if (!Optics.isInj2(firstOptic.optic.outermost())) {
                  return Optional.empty();
               }

               if (!Optics.isInj1(secondOptic.optic.outermost())) {
                  return Optional.empty();
               }

               return Optional.of(this.cap(applyFirst, applySecond));
            }
         }

         return Optional.empty();
      }

      private <R, A, A2, B, B2> R cap(Apply<?, ?> first, Apply<?, ?> second) {
         // ===== 修改：使用双重强制转换 =====
         ProfunctorTransformer<Either<A, B2>, Either<A2, B2>, A, A2> firstFunc = 
             (ProfunctorTransformer<Either<A, B2>, Either<A2, B2>, A, A2>)(PointFree<?>)first.func;
         ProfunctorTransformer<Either<A, B>, Either<A, B2>, B, B2> secondFunc = 
             (ProfunctorTransformer<Either<A, B>, Either<A, B2>, B, B2>)(PointFree<?>)second.func;
         PointFree<Function<A, A2>> firstArg = (PointFree<Function<A, A2>>)first.arg;
         PointFree<Function<B, B2>> secondArg = (PointFree<Function<B, B2>>)second.arg;
         Func<Either<A, B2>, Either<A2, B2>> firstType = (Func<Either<A, B2>, Either<A2, B2>>)first.type;
         Func<Either<A, B>, Either<A, B2>> secondType = (Func<Either<A, B>, Either<A, B2>>)second.type;
         Sum.SumType<A, B> input = (Sum.SumType<A, B>)secondType.first();
         Sum.SumType<A2, B2> output = (Sum.SumType<A2, B2>)firstType.second();
         return (R)(new Comp(
            new Apply<>(secondFunc.castOuterUnchecked(DSL.or(output.first(), input.second()), output), secondArg),
            new Apply<>(firstFunc.castOuterUnchecked(input, DSL.or(output.first(), input.second())), firstArg)
         ));
      }
   }

   enum SortProj implements PointFreeRule.CompRewrite {
      INSTANCE;

      @Override
      public Optional<? extends PointFree<? extends Function<?, ?>>> doRewrite(
         PointFree<? extends Function<?, ?>> first, PointFree<? extends Function<?, ?>> second
      ) {
         if (first instanceof Apply<?, ?> applyFirst && second instanceof Apply<?, ?> applySecond) {
            PointFree<? extends Function<?, ?>> firstFunc = applyFirst.func;
            PointFree<? extends Function<?, ?>> secondFunc = applySecond.func;
            if (firstFunc instanceof ProfunctorTransformer<?, ?, ?, ?> firstOptic && secondFunc instanceof ProfunctorTransformer<?, ?, ?, ?> secondOptic) {
               if (!Optics.isProj2(firstOptic.optic.outermost())) {
                  return Optional.empty();
               }

               if (!Optics.isProj1(secondOptic.optic.outermost())) {
                  return Optional.empty();
               }

               return Optional.of(this.cap(applyFirst, applySecond));
            }
         }

         return Optional.empty();
      }

      private <R, A, A2, B, B2> R cap(Apply<?, ?> first, Apply<?, ?> second) {
         // ===== 修改：使用双重强制转换 =====
         ProfunctorTransformer<Pair<A, B2>, Pair<A2, B2>, A, A2> firstFunc = 
             (ProfunctorTransformer<Pair<A, B2>, Pair<A2, B2>, A, A2>)(PointFree<?>)first.func;
         ProfunctorTransformer<Pair<A, B>, Pair<A, B2>, B, B2> secondFunc = 
             (ProfunctorTransformer<Pair<A, B>, Pair<A, B2>, B, B2>)(PointFree<?>)second.func;
         PointFree<Function<A, A2>> firstArg = (PointFree<Function<A, A2>>)first.arg;
         PointFree<Function<B, B2>> secondArg = (PointFree<Function<B, B2>>)second.arg;
         Func<Pair<A, B2>, Pair<A2, B2>> firstType = (Func<Pair<A, B2>, Pair<A2, B2>>)first.type;
         Func<Pair<A, B>, Pair<A, B2>> secondType = (Func<Pair<A, B>, Pair<A, B2>>)second.type;
         Product.ProductType<A, B> input = (Product.ProductType<A, B>)secondType.first();
         Product.ProductType<A2, B2> output = (Product.ProductType<A2, B2>)firstType.second();
         return (R)(new Comp(
            new Apply<>(secondFunc.castOuterUnchecked(DSL.and(output.first(), input.second()), output), secondArg),
            new Apply<>(firstFunc.castOuterUnchecked(input, DSL.and(output.first(), input.second())), firstArg)
         ));
      }
   }
}