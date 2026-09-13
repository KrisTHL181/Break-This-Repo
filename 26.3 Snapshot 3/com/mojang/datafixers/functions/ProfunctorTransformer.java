package com.mojang.datafixers.functions;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.TypedOptic;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.DynamicOps;
import java.util.Objects;
import java.util.function.Function;

final class ProfunctorTransformer<S, T, A, B> extends PointFree<Function<Function<A, B>, Function<S, T>>> {
   protected final TypedOptic<S, T, A, B> optic;

   public ProfunctorTransformer(TypedOptic<S, T, A, B> optic) {
      this.optic = optic;
   }

   public <S2, T2> ProfunctorTransformer<S2, T2, A, B> castOuterUnchecked(Type<S2> sType, Type<T2> tType) {
      return new ProfunctorTransformer<>(this.optic.castOuterUnchecked(sType, tType));
   }

   @Override
   public Type<Function<Function<A, B>, Function<S, T>>> type() {
      return DSL.func(DSL.func(this.optic.aType(), this.optic.bType()), DSL.func(this.optic.sType(), this.optic.tType()));
   }

   @Override
   public String toString(int level) {
      return "Optic[" + this.optic + "]";
   }

   @Override
   public Function<DynamicOps<?>, Function<Function<A, B>, Function<S, T>>> eval() {
      Function<App2<FunctionType.Mu, A, B>, App2<FunctionType.Mu, S, T>> func = this.optic
         .<FunctionType.Instance.Mu>upCast(FunctionType.Instance.Mu.TYPE_TOKEN)
         .orElseThrow()
         .eval(FunctionType.Instance.INSTANCE);
      Function<Function<A, B>, Function<S, T>> unwrappedFunction = input -> FunctionType.unbox(func.apply(FunctionType.create(input)));
      return ops -> unwrappedFunction;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ProfunctorTransformer<?, ?, ?, ?> that = (ProfunctorTransformer<?, ?, ?, ?>)o;
         return Objects.equals(this.optic, that.optic);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.optic.hashCode();
   }
}
