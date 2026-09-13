package com.mojang.datafixers.functions;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.types.Func;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

final class Comp<A, B> extends PointFree<Function<A, B>> {
   protected final PointFree<? extends Function<?, ?>>[] functions;
   private final Type<Function<A, B>> type;

   protected Comp(PointFree<? extends Function<?, ?>>... functions) {
      this.functions = functions;
      PointFree<? extends Function<?, ?>> first = functions[0];
      PointFree<? extends Function<?, ?>> last = functions[functions.length - 1];
      this.type = DSL.func((Type<A>)((Func)last.type()).first(), ((Func)first.type()).second());
   }

   protected Comp(PointFree<? extends Function<?, ?>>[] functions, Type<Function<A, B>> type) {
      this.functions = functions;
      this.type = type;
   }

   @Override
   public Type<Function<A, B>> type() {
      return this.type;
   }

   @Override
   public String toString(int level) {
      String content = Arrays.stream(this.functions)
         .map(function -> function.toString(level + 1))
         .collect(Collectors.joining("\n" + indent(level + 1) + "◦\n" + indent(level + 1)));
      return "(\n" + indent(level + 1) + content + "\n" + indent(level) + ")";
   }

   @Override
   public Optional<? extends PointFree<Function<A, B>>> all(PointFreeRule rule) {
      List<PointFree<? extends Function<?, ?>>> newFunctions = new ArrayList<>(this.functions.length);
      boolean rewritten = false;

      for (PointFree<? extends Function<?, ?>> function : this.functions) {
         PointFree<? extends Function<?, ?>> rewrite = rule.rewriteOrNop(function);
         if (rewrite != function) {
            rewritten = true;
            if (rewrite instanceof Comp<?, ?> comp) {
               Collections.addAll(newFunctions, comp.functions);
            } else {
               newFunctions.add(rewrite);
            }
         } else {
            newFunctions.add(function);
         }
      }

      return Optional.of(rewritten ? new Comp<>(newFunctions.toArray(PointFree[]::new), this.type) : this);
   }

   @Override
   public Optional<? extends PointFree<Function<A, B>>> one(PointFreeRule rule) {
      for (int i = 0; i < this.functions.length; i++) {
         PointFree<? extends Function<?, ?>> function = this.functions[i];
         Optional<? extends PointFree<? extends Function<?, ?>>> rewrite = rule.rewrite(function);
         if (rewrite.isPresent()) {
            if (rewrite.get() instanceof Comp<?, ?> comp) {
               PointFree<? extends Function<?, ?>>[] newFunctions = new PointFree[this.functions.length - 1 + comp.functions.length];
               System.arraycopy(this.functions, 0, newFunctions, 0, i);
               System.arraycopy(comp.functions, 0, newFunctions, i, comp.functions.length);
               System.arraycopy(this.functions, i + 1, newFunctions, i + comp.functions.length, this.functions.length - i - 1);
               return Optional.of(new Comp<>(newFunctions, this.type));
            }

            PointFree<? extends Function<?, ?>>[] newFunctions = Arrays.copyOf(this.functions, this.functions.length);
            newFunctions[i] = (PointFree<? extends Function<?, ?>>)rewrite.get();
            return Optional.of(new Comp<>(newFunctions, this.type));
         }
      }

      return Optional.empty();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Comp<?, ?> comp = (Comp<?, ?>)o;
         return Arrays.equals(this.functions, comp.functions);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.functions);
   }

   @Override
   public Function<DynamicOps<?>, Function<A, B>> eval() {
      return ops -> input -> {
         Object value = input;

         for (int i = this.functions.length - 1; i >= 0; i--) {
            PointFree<? extends Function<?, ?>> f = this.functions[i];
            value = applyUnchecked((Function<?, ?>)f.evalCached().apply(ops), value);
         }

         return (B)value;
      };
   }

   private static <A, B> B applyUnchecked(Function<A, B> function, Object input) {
      return function.apply((A)input);
   }
}
