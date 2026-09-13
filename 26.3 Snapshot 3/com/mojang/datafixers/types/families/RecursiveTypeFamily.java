package com.mojang.datafixers.types.families;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.FamilyOptic;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.TypedOptic;
import com.mojang.datafixers.View;
import com.mojang.datafixers.functions.Functions;
import com.mojang.datafixers.functions.PointFree;
import com.mojang.datafixers.functions.PointFreeRule;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.RecursivePoint;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import org.jspecify.annotations.Nullable;

public final class RecursiveTypeFamily implements TypeFamily {
   private static final Interner<TypeTemplate> TEMPLATE_INTERNER = Interners.newWeakInterner();
   private final String name;
   private final TypeTemplate template;
   private final int size;
   private final Int2ObjectMap<RecursivePoint.RecursivePointType<?>> types = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap());
   private final int hashCode;

   public RecursiveTypeFamily(String name, TypeTemplate template) {
      this.name = name;
      this.template = (TypeTemplate)TEMPLATE_INTERNER.intern(template);
      this.size = template.size();
      this.hashCode = Objects.hashCode(template);
   }

   public <A> RecursivePoint.RecursivePointType<A> buildMuType(Type<A> newType, @Nullable RecursiveTypeFamily newFamily) {
      if (newFamily == null) {
         TypeTemplate newTypeTemplate = newType.template();
         if (Objects.equals(this.template, newTypeTemplate)) {
            newFamily = this;
         } else {
            newFamily = new RecursiveTypeFamily("ruled " + this.name, newTypeTemplate);
         }
      }

      RecursivePoint.RecursivePointType<A> newMuType = null;

      for (int i1 = 0; i1 < newFamily.size; i1++) {
         RecursivePoint.RecursivePointType<?> type = newFamily.apply(i1);
         Type<?> unfold = type.unfold();
         if (newType.equals(unfold, true, false)) {
            newMuType = (RecursivePoint.RecursivePointType<A>)type;
            break;
         }
      }

      if (newMuType == null) {
         throw new IllegalStateException("Couldn't determine the new type properly");
      } else {
         return newMuType;
      }
   }

   public String name() {
      return this.name;
   }

   public TypeTemplate template() {
      return this.template;
   }

   public int size() {
      return this.size;
   }

   public IntFunction<RewriteResult<?, ?>> fold(Algebra algebra, RecursiveTypeFamily newFamily) {
      return index -> {
         RewriteResult<?, ?> result = algebra.apply(index);
         return RewriteResult.create(View.create(foldUnchecked(this, newFamily, algebra, index)), result.recData());
      };
   }

   private static <A, B> PointFree<Function<A, B>> foldUnchecked(RecursiveTypeFamily family, RecursiveTypeFamily newFamily, Algebra algebra, int index) {
      RecursivePoint.RecursivePointType<A> type = (RecursivePoint.RecursivePointType<A>)family.apply(index);
      RecursivePoint.RecursivePointType<B> newType = (RecursivePoint.RecursivePointType<B>)newFamily.apply(index);
      return Functions.fold(type, newType, algebra, index);
   }

   public RecursivePoint.RecursivePointType<?> apply(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException();
      } else {
         return (RecursivePoint.RecursivePointType<?>)this.types
            .computeIfAbsent(index, i -> new RecursivePoint.RecursivePointType<>(this, i, () -> this.template.apply(this).apply(i)));
      }
   }

   public <A, B> Either<TypedOptic<?, ?, A, B>, Type.FieldNotFoundException> findType(
      int index, Type<A> aType, Type<B> bType, Type.TypeMatcher<A, B> matcher, boolean recurse
   ) {
      return this.apply(index).unfold().findType(aType, bType, matcher, false).flatMap(optic -> {
         TypeTemplate nc = optic.tType().template();
         List<FamilyOptic<A, B>> fo = Lists.newArrayList();
         RecursiveTypeFamily newFamily = new RecursiveTypeFamily(this.name, nc);
         RecursivePoint.RecursivePointType<?> sType = this.apply(index);
         RecursivePoint.RecursivePointType<?> tType = newFamily.apply(index);
         if (recurse) {
            FamilyOptic<A, B> arg = i -> fo.get(0).apply(i);
            fo.add(this.template.applyO(arg, aType, bType));
            TypedOptic<?, ?, A, B> parts = fo.get(0).apply(index);
            return Either.left(parts.castOuterUnchecked((Type<A>)sType, (Type<A>)tType));
         } else {
            return this.mkSimpleOptic(sType, tType, aType, bType, matcher);
         }
      });
   }

   private <S, T, A, B> Either<TypedOptic<?, ?, A, B>, Type.FieldNotFoundException> mkSimpleOptic(
      RecursivePoint.RecursivePointType<S> sType, RecursivePoint.RecursivePointType<T> tType, Type<A> aType, Type<B> bType, Type.TypeMatcher<A, B> matcher
   ) {
      return sType.unfold().findType(aType, bType, matcher, false).mapLeft(o -> o.castOuterUnchecked(sType, tType));
   }

   public Optional<RewriteResult<?, ?>> everywhere(int index, TypeRewriteRule rule, PointFreeRule optimizationRule) {
      Type<?> sourceType = this.apply(index).unfold();
      RewriteResult<?, ?> sourceView = DataFixUtils.orElse(sourceType.everywhere(rule, optimizationRule, false, false), RewriteResult.nop(sourceType));
      RecursivePoint.RecursivePointType<?> newType = this.buildMuType(sourceView.view().newType(), null);
      RecursiveTypeFamily newFamily = newType.family();
      List<RewriteResult<?, ?>> views = Lists.newArrayList();
      boolean foundAny = false;

      for (int i = 0; i < this.size; i++) {
         RecursivePoint.RecursivePointType<?> type = this.apply(i);
         Type<?> unfold = type.unfold();
         boolean nop1 = true;
         RewriteResult<?, ?> view = DataFixUtils.orElse(unfold.everywhere(rule, optimizationRule, false, true), RewriteResult.nop(unfold));
         if (!view.view().isNop()) {
            nop1 = false;
         }

         RecursivePoint.RecursivePointType<?> newMuType = this.buildMuType(view.view().newType(), newFamily);
         boolean nop = this.cap2(views, type, rule, optimizationRule, nop1, view, newMuType);
         foundAny = foundAny || !nop;
      }

      if (!foundAny) {
         return Optional.empty();
      }

      Algebra algebra = new ListAlgebra("everywhere", views);
      RewriteResult<?, ?> fold = this.fold(algebra, newFamily).apply(index);
      return Optional.of(RewriteResult.create(View.create(fold.view().function()), fold.recData()));
   }

   private <A, B> boolean cap2(
      List<RewriteResult<?, ?>> views,
      RecursivePoint.RecursivePointType<A> type,
      TypeRewriteRule rule,
      PointFreeRule optimizationRule,
      boolean nop,
      RewriteResult<?, ?> view,
      RecursivePoint.RecursivePointType<B> newType
   ) {
      RewriteResult<A, B> newView = RewriteResult.create(newType.in(), new BitSet()).compose((RewriteResult<A, B>)view);
      Optional<RewriteResult<B, ?>> rewrite = rule.rewrite(newView.view().newType());
      if (rewrite.isPresent() && !rewrite.get().view().isNop()) {
         nop = false;
         view = rewrite.get().compose(newView);
      }

      view = RewriteResult.create(view.view().rewriteOrNop(optimizationRule), view.recData());
      views.add(view);
      return nop;
   }

   @Override
   public String toString() {
      return "Mu[" + this.name + ", " + this.size + ", " + this.template + "]";
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof RecursiveTypeFamily family) ? false : this.template == family.template;
      }
   }

   @Override
   public int hashCode() {
      return this.hashCode;
   }
}
