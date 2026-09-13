package com.mojang.datafixers.functions;

import com.mojang.datafixers.types.Func;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.DynamicOps;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

final class Apply<A, B> extends PointFree<B> {
   protected final PointFree<Function<A, B>> func;
   protected final PointFree<A> arg;
   protected final Type<B> type;

   public Apply(PointFree<Function<A, B>> func, PointFree<A> arg) {
      this(func, arg, ((Func)func.type()).second());
   }

   Apply(PointFree<Function<A, B>> func, PointFree<A> arg, Type<B> type) {
      this.func = func;
      this.arg = arg;
      this.type = type;
   }

   @Override
   public Function<DynamicOps<?>, B> eval() {
      return ops -> this.func.evalCached().apply(ops).apply(this.arg.evalCached().apply(ops));
   }

   @Override
   public Type<B> type() {
      return this.type;
   }

   @Override
   public String toString(int level) {
      return "(ap " + this.func.toString(level + 1) + "\n" + indent(level + 1) + this.arg.toString(level + 1) + "\n" + indent(level) + ")";
   }

   @Override
   public Optional<? extends PointFree<B>> all(PointFreeRule rule) {
      PointFree<Function<A, B>> f = rule.rewriteOrNop(this.func);
      PointFree<A> a = rule.rewriteOrNop(this.arg);
      return f == this.func && a == this.arg ? Optional.of(this) : Optional.of(new Apply<>(f, a, this.type));
   }

   @Override
   public Optional<? extends PointFree<B>> one(PointFreeRule rule) {
      return rule.rewrite(this.func)
         .map(f -> new Apply<>((PointFree<Function<A, B>>)f, this.arg, this.type))
         .or(() -> rule.rewrite(this.arg).map(a -> new Apply<>(this.func, (PointFree<A>)a, this.type)));
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof Apply<?, ?> apply) ? false : Objects.equals(this.func, apply.func) && Objects.equals(this.arg, apply.arg);
      }
   }

   @Override
   public int hashCode() {
      int result = this.func.hashCode();
      return 31 * result + this.arg.hashCode();
   }
}
