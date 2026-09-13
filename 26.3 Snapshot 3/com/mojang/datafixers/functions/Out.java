package com.mojang.datafixers.functions;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.RecursivePoint;
import com.mojang.serialization.DynamicOps;
import java.util.Objects;
import java.util.function.Function;

final class Out<A> extends PointFree<Function<A, A>> {
   private final RecursivePoint.RecursivePointType<A> type;

   public Out(RecursivePoint.RecursivePointType<A> type) {
      this.type = type;
   }

   @Override
   public Type<Function<A, A>> type() {
      return DSL.func(this.type, this.type.unfold());
   }

   @Override
   public String toString(int level) {
      return "Out[" + this.type + "]";
   }

   @Override
   public boolean equals(Object obj) {
      return this == obj ? true : obj instanceof Out && Objects.equals(this.type, ((Out)obj).type);
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
