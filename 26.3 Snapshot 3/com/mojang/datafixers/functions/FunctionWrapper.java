package com.mojang.datafixers.functions;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.DynamicOps;
import java.util.Objects;
import java.util.function.Function;

final class FunctionWrapper<A, B> extends PointFree<Function<A, B>> {
   private final String name;
   protected final Function<DynamicOps<?>, Function<A, B>> fun;
   private final Type<Function<A, B>> type;

   FunctionWrapper(String name, Function<DynamicOps<?>, Function<A, B>> fun, Type<A> input, Type<B> output) {
      this.name = name;
      this.fun = fun;
      this.type = DSL.func(input, output);
   }

   @Override
   public Type<Function<A, B>> type() {
      return this.type;
   }

   @Override
   public String toString(int level) {
      return "fun[" + this.name + "]";
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         FunctionWrapper<?, ?> that = (FunctionWrapper<?, ?>)o;
         return Objects.equals(this.fun, that.fun) && Objects.equals(this.type, that.type);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.fun.hashCode();
   }

   @Override
   public Function<DynamicOps<?>, Function<A, B>> eval() {
      return this.fun;
   }
}
