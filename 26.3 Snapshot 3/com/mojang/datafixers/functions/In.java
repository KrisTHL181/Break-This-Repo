package com.mojang.datafixers.functions;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.RecursivePoint;
import com.mojang.serialization.DynamicOps;
import java.util.Objects;
import java.util.function.Function;

final class In<A> extends PointFree<Function<A, A>> {
   protected final RecursivePoint.RecursivePointType<A> type;

   public In(RecursivePoint.RecursivePointType<A> type) {
      this.type = type;
   }

   @Override
   public Type<Function<A, A>> type() {
      return DSL.func(this.type.unfold(), this.type);
   }

   @Override
   public String toString(int level) {
      return "In[" + this.type + "]";
   }

   @Override
   public boolean equals(Object obj) {
      return this == obj ? true : obj instanceof In && Objects.equals(this.type, ((In)obj).type);
   }

   @Override
   public int hashCode() {
      return this.type.hashCode();
   }

   @Override
   public Function<DynamicOps<?>, Function<A, A>> eval() {
      return ops -> Function.identity();
   }
}
