package com.mojang.datafixers.types.templates;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.FamilyOptic;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.TypedOptic;
import com.mojang.datafixers.optics.Optics;
import com.mojang.datafixers.optics.profunctors.Cartesian;
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

public record CompoundList(TypeTemplate key, TypeTemplate element) implements TypeTemplate {
   @Override
   public int size() {
      return Math.max(this.key.size(), this.element.size());
   }

   @Override
   public TypeFamily apply(TypeFamily family) {
      return index -> DSL.compoundList(this.key.apply(family).apply(index), this.element.apply(family).apply(index));
   }

   @Override
   public <A, B> FamilyOptic<A, B> applyO(FamilyOptic<A, B> input, Type<A> aType, Type<B> bType) {
      return TypeFamily.familyOptic(i -> this.cap(this.element.applyO(input, aType, bType).apply(i)));
   }

   private <S, T, A, B> TypedOptic<?, ?, A, B> cap(TypedOptic<S, T, A, B> concreteOptic) {
      Type<Pair<String, S>> sTypeEntry = DSL.and(DSL.string(), concreteOptic.sType());
      Type<Pair<String, T>> tTypeEntry = DSL.and(DSL.string(), concreteOptic.tType());
      return new TypedOptic<>(
            TraversalP.Mu.TYPE_TOKEN,
            DSL.compoundList(concreteOptic.sType()),
            DSL.compoundList(concreteOptic.tType()),
            sTypeEntry,
            tTypeEntry,
            Optics.listTraversal()
         )
         .compose(new TypedOptic<>(Cartesian.Mu.TYPE_TOKEN, sTypeEntry, tTypeEntry, concreteOptic.sType(), concreteOptic.tType(), Optics.proj2()))
         .compose(concreteOptic);
   }

   @Override
   public <FT, FR> Either<TypeTemplate, Type.FieldNotFoundException> findFieldOrType(int index, @Nullable String name, Type<FT> type, Type<FR> resultType) {
      return this.element.findFieldOrType(index, name, type, resultType).mapLeft(element1 -> new CompoundList(this.key, element1));
   }

   @Override
   public IntFunction<RewriteResult<?, ?>> hmap(TypeFamily family, IntFunction<RewriteResult<?, ?>> function) {
      return i -> {
         RewriteResult<?, ?> f1 = this.key.hmap(family, function).apply(i);
         RewriteResult<?, ?> f2 = this.element.hmap(family, function).apply(i);
         return this.cap(this.apply(family).apply(i), f1, f2);
      };
   }

   private <L, R> RewriteResult<?, ?> cap(Type<?> type, RewriteResult<L, ?> f1, RewriteResult<R, ?> f2) {
      return ((CompoundList.CompoundListType)type).mergeViews(f1, f2);
   }

   @Override
   public String toString() {
      return "CompoundList[" + this.element + "]";
   }

   public static final class CompoundListType<K, V> extends Type<java.util.List<Pair<K, V>>> {
      protected final Type<K> key;
      protected final Type<V> element;

      public CompoundListType(Type<K> key, Type<V> element) {
         this.key = key;
         this.element = element;
      }

      @Override
      public RewriteResult<java.util.List<Pair<K, V>>, ?> all(TypeRewriteRule rule, boolean recurse, boolean checkIndex) {
         return this.mergeViews(this.key.rewriteOrNop(rule), this.element.rewriteOrNop(rule));
      }

      public <K2, V2> RewriteResult<java.util.List<Pair<K, V>>, ?> mergeViews(RewriteResult<K, K2> leftView, RewriteResult<V, V2> rightView) {
         RewriteResult<java.util.List<Pair<K, V>>, java.util.List<Pair<K2, V>>> v1 = fixKeys(this, this.key, this.element, leftView);
         RewriteResult<java.util.List<Pair<K2, V>>, java.util.List<Pair<K2, V2>>> v2 = fixValues(
            v1.view().newType(), leftView.view().newType(), this.element, rightView
         );
         return v2.compose(v1);
      }

      @Override
      public Optional<RewriteResult<java.util.List<Pair<K, V>>, ?>> one(TypeRewriteRule rule) {
         return DataFixUtils.or(
            rule.rewrite(this.key).map(v -> fixKeys(this, this.key, this.element, (RewriteResult<K, ?>)v)),
            () -> rule.rewrite(this.element).map(v -> fixValues(this, this.key, this.element, (RewriteResult<V, ?>)v))
         );
      }

      private static <K, V, K2> RewriteResult<java.util.List<Pair<K, V>>, java.util.List<Pair<K2, V>>> fixKeys(
         Type<java.util.List<Pair<K, V>>> type, Type<K> first, Type<V> second, RewriteResult<K, K2> view
      ) {
         return opticView(type, view, TypedOptic.compoundListKeys(first, view.view().newType(), second));
      }

      private static <K, V, V2> RewriteResult<java.util.List<Pair<K, V>>, java.util.List<Pair<K, V2>>> fixValues(
         Type<java.util.List<Pair<K, V>>> type, Type<K> first, Type<V> second, RewriteResult<V, V2> view
      ) {
         return opticView(type, view, TypedOptic.compoundListElements(first, second, view.view().newType()));
      }

      @Override
      public Type<?> updateMu(RecursiveTypeFamily newFamily) {
         return DSL.compoundList(this.key.updateMu(newFamily), this.element.updateMu(newFamily));
      }

      @Override
      public TypeTemplate buildTemplate() {
         return new CompoundList(this.key.template(), this.element.template());
      }

      @Override
      public Optional<java.util.List<Pair<K, V>>> point(DynamicOps<?> ops) {
         return Optional.of(ImmutableList.of());
      }

      @Override
      public <FT, FR> Either<TypedOptic<java.util.List<Pair<K, V>>, ?, FT, FR>, Type.FieldNotFoundException> findTypeInChildren(
         Type<FT> type, Type<FR> resultType, Type.TypeMatcher<FT, FR> matcher, boolean recurse
      ) {
         Either<TypedOptic<K, ?, FT, FR>, Type.FieldNotFoundException> firstFieldLens = this.key.findType(type, resultType, matcher, recurse);
         return firstFieldLens.map(this::capLeft, r -> {
            Either<TypedOptic<V, ?, FT, FR>, Type.FieldNotFoundException> secondFieldLens = this.element.findType(type, resultType, matcher, recurse);
            return secondFieldLens.mapLeft(this::capRight);
         });
      }

      private <FT, K2, FR> Either<TypedOptic<java.util.List<Pair<K, V>>, ?, FT, FR>, Type.FieldNotFoundException> capLeft(TypedOptic<K, K2, FT, FR> optic) {
         return Either.left(TypedOptic.compoundListKeys(optic.sType(), optic.tType(), this.element).compose(optic));
      }

      private <FT, V2, FR> TypedOptic<java.util.List<Pair<K, V>>, ?, FT, FR> capRight(TypedOptic<V, V2, FT, FR> optic) {
         return TypedOptic.compoundListElements(this.key, optic.sType(), optic.tType()).compose(optic);
      }

      @Override
      protected Codec<java.util.List<Pair<K, V>>> buildCodec() {
         return Codec.compoundList(this.key.codec(), this.element.codec());
      }

      @Override
      public String toString() {
         return "CompoundList[" + this.key + " -> " + this.element + "]";
      }

      @Override
      public boolean equals(Object obj, boolean ignoreRecursionPoints, boolean checkIndex) {
         return !(obj instanceof CompoundList.CompoundListType<?, ?> that)
            ? false
            : this.key.equals(that.key, ignoreRecursionPoints, checkIndex) && this.element.equals(that.element, ignoreRecursionPoints, checkIndex);
      }

      @Override
      public int hashCode() {
         int result = this.key.hashCode();
         return 31 * result + this.element.hashCode();
      }

      public Type<K> getKey() {
         return this.key;
      }

      public Type<V> getElement() {
         return this.element;
      }
   }
}
