package com.mojang.datafixers.types.templates;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.FamilyOptic;
import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.TypedOptic;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.optics.Optic;
import com.mojang.datafixers.optics.Optics;
import com.mojang.datafixers.optics.Traversal;
import com.mojang.datafixers.optics.profunctors.TraversalP;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.families.RecursiveTypeFamily;
import com.mojang.datafixers.types.families.TypeFamily;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import java.util.function.IntFunction;
import org.jspecify.annotations.Nullable;

public record Product(TypeTemplate f, TypeTemplate g) implements TypeTemplate {
   @Override
   public int size() {
      return Math.max(this.f.size(), this.g.size());
   }

   @Override
   public TypeFamily apply(final TypeFamily family) {
      return new TypeFamily() {
         @Override
         public Type<?> apply(int index) {
            return DSL.and(Product.this.f.apply(family).apply(index), Product.this.g.apply(family).apply(index));
         }
      };
   }

   @Override
   public <A, B> FamilyOptic<A, B> applyO(FamilyOptic<A, B> input, Type<A> aType, Type<B> bType) {
      return TypeFamily.familyOptic(i -> this.cap(this.f.applyO(input, aType, bType), this.g.applyO(input, aType, bType), i));
   }

   private <A, B, LS, RS, LT, RT> TypedOptic<?, ?, A, B> cap(FamilyOptic<A, B> lo, FamilyOptic<A, B> ro, int index) {
      TypeToken<TraversalP.Mu> bound = TraversalP.Mu.TYPE_TOKEN;
      TypedOptic<LS, LT, A, B> lp = (TypedOptic<LS, LT, A, B>)lo.apply(index);
      TypedOptic<RS, RT, A, B> rp = (TypedOptic<RS, RT, A, B>)ro.apply(index);
      Optic<? super TraversalP.Mu, LS, LT, A, B> l = lp.<TraversalP.Mu>upCast(bound).orElseThrow(IllegalArgumentException::new);
      Optic<? super TraversalP.Mu, RS, RT, A, B> r = rp.<TraversalP.Mu>upCast(bound).orElseThrow(IllegalArgumentException::new);
      final Traversal<LS, LT, A, B> lt = Optics.toTraversal(l);
      final Traversal<RS, RT, A, B> rt = Optics.toTraversal(r);
      return new TypedOptic<>(
         ImmutableSet.of(bound),
         DSL.and(lp.sType(), rp.sType()),
         DSL.and(lp.tType(), rp.tType()),
         lp.aType(),
         lp.bType(),
         new Traversal<Pair<LS, RS>, Pair<LT, RT>, A, B>() {
            @Override
            public <F extends K1> FunctionType<Pair<LS, RS>, App<F, Pair<LT, RT>>> wander(Applicative<F, ?> applicative, FunctionType<A, App<F, B>> input) {
               return p -> applicative.ap2(
                  applicative.point(Pair::of), lt.wander(applicative, input).apply(p.getFirst()), rt.wander(applicative, input).apply(p.getSecond())
               );
            }
         }
      );
   }

   @Override
   public <FT, FR> Either<TypeTemplate, Type.FieldNotFoundException> findFieldOrType(int index, @Nullable String name, Type<FT> type, Type<FR> resultType) {
      Either<TypeTemplate, Type.FieldNotFoundException> either = this.f.findFieldOrType(index, name, type, resultType);
      return either.map(
         f2 -> Either.left(new Product(f2, this.g)), r -> this.g.findFieldOrType(index, name, type, resultType).mapLeft(g2 -> new Product(this.f, g2))
      );
   }

   @Override
   public IntFunction<RewriteResult<?, ?>> hmap(TypeFamily family, IntFunction<RewriteResult<?, ?>> function) {
      return i -> {
         RewriteResult<?, ?> f1 = this.f.hmap(family, function).apply(i);
         RewriteResult<?, ?> f2 = this.g.hmap(family, function).apply(i);
         return this.cap(this.apply(family).apply(i), f1, f2);
      };
   }

   private <L, R> RewriteResult<?, ?> cap(Type<?> type, RewriteResult<L, ?> f1, RewriteResult<R, ?> f2) {
      return ((Product.ProductType)type).mergeViews(f1, f2);
   }

   @Override
   public String toString() {
      return "(" + this.f + ", " + this.g + ")";
   }

   public static final class ProductType<F, G> extends Type<Pair<F, G>> {
      protected final Type<F> first;
      protected final Type<G> second;
      private int hashCode;

      public ProductType(Type<F> first, Type<G> second) {
         this.first = first;
         this.second = second;
      }

      public Type<F> first() {
         return this.first;
      }

      public Type<G> second() {
         return this.second;
      }

      @Override
      public RewriteResult<Pair<F, G>, ?> all(TypeRewriteRule rule, boolean recurse, boolean checkIndex) {
         return this.mergeViews(this.first.rewriteOrNop(rule), this.second.rewriteOrNop(rule));
      }

      public <F2, G2> RewriteResult<Pair<F, G>, ?> mergeViews(RewriteResult<F, F2> leftView, RewriteResult<G, G2> rightView) {
         RewriteResult<Pair<F, G>, Pair<F2, G>> v1 = fixLeft(this, this.first, this.second, leftView);
         RewriteResult<Pair<F2, G>, Pair<F2, G2>> v2 = fixRight(v1.view().newType(), leftView.view().newType(), this.second, rightView);
         return v2.compose(v1);
      }

      @Override
      public Optional<RewriteResult<Pair<F, G>, ?>> one(TypeRewriteRule rule) {
         return DataFixUtils.or(
            rule.rewrite(this.first).map(v -> fixLeft(this, this.first, this.second, (RewriteResult<F, ?>)v)),
            () -> rule.rewrite(this.second).map(v -> fixRight(this, this.first, this.second, (RewriteResult<G, ?>)v))
         );
      }

      private static <F, G, F2> RewriteResult<Pair<F, G>, Pair<F2, G>> fixLeft(Type<Pair<F, G>> type, Type<F> first, Type<G> second, RewriteResult<F, F2> view) {
         return opticView(type, view, TypedOptic.proj1(first, second, view.view().newType()));
      }

      private static <F, G, G2> RewriteResult<Pair<F, G>, Pair<F, G2>> fixRight(Type<Pair<F, G>> type, Type<F> first, Type<G> second, RewriteResult<G, G2> view) {
         return opticView(type, view, TypedOptic.proj2(first, second, view.view().newType()));
      }

      @Override
      public Type<?> updateMu(RecursiveTypeFamily newFamily) {
         return DSL.and(this.first.updateMu(newFamily), this.second.updateMu(newFamily));
      }

      @Override
      public TypeTemplate buildTemplate() {
         return DSL.and(this.first.template(), this.second.template());
      }

      @Override
      public Optional<TaggedChoice.TaggedChoiceType<?>> findChoiceType(String name, int index) {
         return DataFixUtils.or(this.first.findChoiceType(name, index), () -> this.second.findChoiceType(name, index));
      }

      @Override
      public Optional<Type<?>> findCheckedType(int index) {
         return DataFixUtils.or(this.first.findCheckedType(index), () -> this.second.findCheckedType(index));
      }

      @Override
      public Codec<Pair<F, G>> buildCodec() {
         return Codec.pair(this.first.codec(), this.second.codec());
      }

      @Override
      public String toString() {
         return "(" + this.first + ", " + this.second + ")";
      }

      @Override
      public boolean equals(Object obj, boolean ignoreRecursionPoints, boolean checkIndex) {
         return !(obj instanceof Product.ProductType<?, ?> that)
            ? false
            : this.first.equals(that.first, ignoreRecursionPoints, checkIndex) && this.second.equals(that.second, ignoreRecursionPoints, checkIndex);
      }

      @Override
      public int hashCode() {
         if (this.hashCode == 0) {
            int result = this.first.hashCode();
            result = 31 * result + this.second.hashCode();
            this.hashCode = result;
         }

         return this.hashCode;
      }

      @Override
      public Optional<Type<?>> findFieldTypeOpt(String name) {
         return DataFixUtils.or(this.first.findFieldTypeOpt(name), () -> this.second.findFieldTypeOpt(name));
      }

      @Override
      public Optional<Pair<F, G>> point(DynamicOps<?> ops) {
         return this.first.point(ops).flatMap(f -> this.second.point(ops).map(g -> Pair.of((F)f, (G)g)));
      }

      @Override
      public <FT, FR> Either<TypedOptic<Pair<F, G>, ?, FT, FR>, Type.FieldNotFoundException> findTypeInChildren(
         Type<FT> type, Type<FR> resultType, Type.TypeMatcher<FT, FR> matcher, boolean recurse
      ) {
         Either<TypedOptic<F, ?, FT, FR>, Type.FieldNotFoundException> firstFieldLens = this.first.findType(type, resultType, matcher, recurse);
         return firstFieldLens.map(this::capLeft, r -> {
            Either<TypedOptic<G, ?, FT, FR>, Type.FieldNotFoundException> secondFieldLens = this.second.findType(type, resultType, matcher, recurse);
            return secondFieldLens.mapLeft(this::capRight);
         });
      }

      private <FT, F2, FR> Either<TypedOptic<Pair<F, G>, ?, FT, FR>, Type.FieldNotFoundException> capLeft(TypedOptic<F, F2, FT, FR> optic) {
         return Either.left(TypedOptic.proj1(optic.sType(), this.second, optic.tType()).compose(optic));
      }

      private <FT, G2, FR> TypedOptic<Pair<F, G>, ?, FT, FR> capRight(TypedOptic<G, G2, FT, FR> optic) {
         return TypedOptic.proj2(this.first, optic.sType(), optic.tType()).compose(optic);
      }
   }
}
