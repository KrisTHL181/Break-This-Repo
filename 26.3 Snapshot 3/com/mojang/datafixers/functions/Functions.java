package com.mojang.datafixers.functions;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.TypedOptic;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.families.Algebra;
import com.mojang.datafixers.types.templates.RecursivePoint;
import com.mojang.serialization.DynamicOps;
import java.util.function.Function;

public abstract class Functions {
   // ===== 修改：使用双重强制转换解决类型推断问题 =====
   public static <A, B, C> PointFree<Function<A, C>> comp(PointFree<Function<B, C>> f1, PointFree<Function<A, B>> f2) {
      if (isId(f1)) {
         return (PointFree<Function<A, C>>)(PointFree<?>)f2;
      } else if (isId(f2)) {
         return (PointFree<Function<A, C>>)(PointFree<?>)f1;
      } else if (f1 instanceof Comp<B, C> comp1 && f2 instanceof Comp<A, B> comp2) {
         PointFree<? extends Function<?, ?>>[] functions = new PointFree[comp1.functions.length + comp2.functions.length];
         System.arraycopy(comp1.functions, 0, functions, 0, comp1.functions.length);
         System.arraycopy(comp2.functions, 0, functions, comp1.functions.length, comp2.functions.length);
         return new Comp<>(functions);
      } else if (f1 instanceof Comp<B, C> comp1) {
         PointFree<? extends Function<?, ?>>[] functions = new PointFree[comp1.functions.length + 1];
         System.arraycopy(comp1.functions, 0, functions, 0, comp1.functions.length);
         functions[functions.length - 1] = f2;
         return new Comp<>(functions);
      } else if (f2 instanceof Comp<A, B> comp2) {
         PointFree<? extends Function<?, ?>>[] functions = new PointFree[1 + comp2.functions.length];
         functions[0] = f1;
         System.arraycopy(comp2.functions, 0, functions, 1, comp2.functions.length);
         return new Comp<>(functions);
      } else {
         return new Comp<>(f1, f2);
      }
   }

   public static <A, B> PointFree<Function<A, B>> fun(String name, Function<DynamicOps<?>, Function<A, B>> fun, Type<A> input, Type<B> output) {
      return new FunctionWrapper<>(name, fun, input, output);
   }

   public static <A, B> PointFree<B> app(PointFree<Function<A, B>> fun, PointFree<A> arg) {
      return new Apply<>(fun, arg);
   }

   public static <S, T, A, B> PointFree<Function<Function<A, B>, Function<S, T>>> profunctorTransformer(TypedOptic<S, T, A, B> lens) {
      return new ProfunctorTransformer<>(lens);
   }

   public static <A> Bang<A> bang(Type<A> type) {
      return new Bang<>(type);
   }

   public static <A> PointFree<Function<A, A>> in(RecursivePoint.RecursivePointType<A> type) {
      return new In<>(type);
   }

   public static <A> PointFree<Function<A, A>> out(RecursivePoint.RecursivePointType<A> type) {
      return new Out<>(type);
   }

   public static <A, B> PointFree<Function<A, B>> fold(
      RecursivePoint.RecursivePointType<A> aType, RecursivePoint.RecursivePointType<B> bType, Algebra algebra, int index
   ) {
      return new Fold<>(aType, bType, algebra, index);
   }

   public static <A> PointFree<Function<A, A>> id(Type<A> type) {
      return new Id<>(DSL.func(type, type));
   }

   public static boolean isId(PointFree<?> function) {
      return function instanceof Id;
   }
}