package com.mojang.datafixers.functions;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DynamicOps;
import java.util.function.Function;

final class Bang<A> extends PointFree<Function<A, Unit>> {
   private final Type<A> type;

   Bang(Type<A> type) {
      this.type = type;
   }

   @Override
   public Type<Function<A, Unit>> type() {
      return DSL.func(this.type, DSL.emptyPartType());
   }

   @Override
   public String toString(int level) {
      return "!";
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof Bang<?> bang && this.type.equals(bang.type);
   }

   @Override
   public int hashCode() {
      return this.type.hashCode();
   }

   @Override
   public Function<DynamicOps<?>, Function<A, Unit>> eval() {
      return ops -> a -> Unit.INSTANCE;
   }
}
