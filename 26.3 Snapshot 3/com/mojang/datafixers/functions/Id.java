package com.mojang.datafixers.functions;

import com.mojang.datafixers.types.Type;
import com.mojang.serialization.DynamicOps;
import java.util.function.Function;

final class Id<A> extends PointFree<Function<A, A>> {
   private final Type<Function<A, A>> type;

   Id(Type<Function<A, A>> type) {
      this.type = type;
   }

   @Override
   public Type<Function<A, A>> type() {
      return this.type;
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof Id<?> id && this.type.equals(id.type);
   }

   @Override
   public int hashCode() {
      return this.type.hashCode();
   }

   @Override
   public String toString(int level) {
      return "id";
   }

   @Override
   public Function<DynamicOps<?>, Function<A, A>> eval() {
      return ops -> Function.identity();
   }
}
