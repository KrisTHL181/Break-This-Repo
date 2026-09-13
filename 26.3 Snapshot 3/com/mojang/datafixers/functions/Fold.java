package com.mojang.datafixers.functions;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.View;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.families.Algebra;
import com.mojang.datafixers.types.families.ListAlgebra;
import com.mojang.datafixers.types.families.RecursiveTypeFamily;
import com.mojang.datafixers.types.templates.RecursivePoint;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;

final class Fold<A, B> extends PointFree<Function<A, B>> {
   private static final Map<Fold.HmapCacheKey, IntFunction<RewriteResult<?, ?>>> HMAP_CACHE = Maps.newConcurrentMap();
   private static final Map<Pair<IntFunction<RewriteResult<?, ?>>, Integer>, RewriteResult<?, ?>> HMAP_APPLY_CACHE = Maps.newConcurrentMap();
   protected final RecursivePoint.RecursivePointType<A> aType;
   protected final RecursivePoint.RecursivePointType<B> bType;
   protected final Algebra algebra;
   protected final int index;

   public Fold(RecursivePoint.RecursivePointType<A> aType, RecursivePoint.RecursivePointType<B> bType, Algebra algebra, int index) {
      this.aType = aType;
      this.bType = bType;
      this.algebra = algebra;
      this.index = index;
   }

   @Override
   public Type<Function<A, B>> type() {
      return DSL.func(this.aType, this.bType);
   }

   @Override
   Optional<? extends PointFree<Function<A, B>>> all(PointFreeRule rule) {
      int familySize = this.aType.family().size();
      List<RewriteResult<?, ?>> newAlgebra = new ArrayList<>(familySize);
      boolean changed = false;

      for (int i = 0; i < familySize; i++) {
         RewriteResult<?, ?> view = this.algebra.apply(i);
         PointFree<? extends Function<?, ?>> function = view.view().function();
         PointFree<? extends Function<?, ?>> rewrite = rule.rewriteOrNop(function);
         if (rewrite != function) {
            newAlgebra.add(cap(view, rewrite));
            changed = true;
         } else {
            newAlgebra.add(view);
         }
      }

      return changed ? Optional.of(new Fold<>(this.aType, this.bType, new ListAlgebra("Rewrite all", newAlgebra), this.index)) : Optional.empty();
   }

   private static <A, B> RewriteResult<A, B> cap(RewriteResult<A, B> view, PointFree<? extends Function<?, ?>> rewrite) {
      return RewriteResult.create(new View<>((PointFree<Function<A, B>>)rewrite), view.recData());
   }

   // ===== 修改：使用双重强制转换修复类型不匹配 =====
   private <FB> PointFree<Function<A, B>> cap(RewriteResult<?, FB> resResult) {
      RewriteResult<A, B> op = (RewriteResult<A, B>)this.algebra.apply(this.index);
      return Functions.comp(op.view().function(), (PointFree<Function<A, A>>)(PointFree<?>)resResult.view().function());
   }

   @Override
   public Function<DynamicOps<?>, Function<A, B>> eval() {
      return ops -> a -> {
         RecursiveTypeFamily family = this.aType.family();
         RecursiveTypeFamily newFamily = this.bType.family();
         IntFunction<RewriteResult<?, ?>> hmapped = HMAP_CACHE.computeIfAbsent(
            new Fold.HmapCacheKey(family, newFamily, this.algebra),
            key -> key.family().template().hmap(key.family(), key.family().fold(key.algebra(), key.newFamily()))
         );
         RewriteResult<?, ?> result = HMAP_APPLY_CACHE.computeIfAbsent(Pair.of(hmapped, this.index), key -> key.getFirst().apply(key.getSecond()));
         PointFree<Function<A, B>> eval = this.cap(result);
         return eval.evalCached().apply(ops).apply(a);
      };
   }

   @Override
   public String toString(int level) {
      return "fold(" + this.aType + ", " + this.index + ", \n" + indent(level + 1) + this.algebra.toString(level + 1) + "\n" + indent(level) + ")";
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Fold<?, ?> fold = (Fold<?, ?>)o;
         return Objects.equals(this.aType, fold.aType) && Objects.equals(this.bType, fold.bType) && Objects.equals(this.algebra, fold.algebra);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = this.aType.hashCode();
      result = 31 * result + this.bType.hashCode();
      return 31 * result + this.algebra.hashCode();
   }

   private record HmapCacheKey(RecursiveTypeFamily family, RecursiveTypeFamily newFamily, Algebra algebra) {
   }
}