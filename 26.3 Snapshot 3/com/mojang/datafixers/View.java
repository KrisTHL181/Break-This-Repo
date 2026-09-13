package com.mojang.datafixers;

import com.mojang.datafixers.functions.Functions;
import com.mojang.datafixers.functions.PointFree;
import com.mojang.datafixers.functions.PointFreeRule;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.types.Func;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import java.util.function.Function;

public record View<A, B>(PointFree<Function<A, B>> function) implements App2<View.Mu, A, B> {
   static <A, B> View<A, B> unbox(App2<View.Mu, A, B> box) {
      return (View<A, B>)box;
   }

   public static <A> View<A, A> nopView(Type<A> type) {
      return new View<>(Functions.id(type));
   }

   public Type<A> type() {
      return ((Func)this.funcType()).first();
   }

   public Type<B> newType() {
      return ((Func)this.funcType()).second();
   }

   public Type<Function<A, B>> funcType() {
      return this.function.type();
   }

   @Override
   public String toString() {
      return "View[" + this.function + "," + this.newType() + "]";
   }

   public Optional<? extends View<A, B>> rewrite(PointFreeRule rule) {
      return rule.rewrite(this.function()).map(View::new);
   }

   public View<A, B> rewriteOrNop(PointFreeRule rule) {
      return DataFixUtils.orElse(this.rewrite(rule), this);
   }

   public <C> View<A, C> flatMap(Function<Type<B>, View<B, C>> function) {
      View<B, C> instance = function.apply(this.newType());
      return new View<>(Functions.comp(instance.function(), this.function()));
   }

   public static <A, B> View<A, B> create(PointFree<Function<A, B>> function) {
      return new View<>(function);
   }

   public static <A, B> View<A, B> create(String name, Type<A> type, Type<B> newType, Function<DynamicOps<?>, Function<A, B>> function) {
      return new View<>(Functions.fun(name, function, type, newType));
   }

   // ===== 修改：拆分三元运算符，显式强制转换 =====
   public <C> View<C, B> compose(View<C, A> that) {
      if (this.isNop()) {
         return (View<C, B>) that;
      } else if (that.isNop()) {
         return (View<C, B>) this;
      } else {
         return new View<>(Functions.comp(this.function(), that.function()));
      }
   }

   public boolean isNop() {
      return Functions.isId(this.function());
   }

   static final class Mu implements K2 {
   }
}