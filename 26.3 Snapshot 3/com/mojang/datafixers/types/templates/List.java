package com.mojang.datafixers.types.templates;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.FamilyOptic;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.TypedOptic;
import com.mojang.datafixers.optics.Optics;
import com.mojang.datafixers.optics.profunctors.TraversalP;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.families.RecursiveTypeFamily;
import com.mojang.datafixers.types.families.TypeFamily;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import java.util.function.IntFunction;
import org.jspecify.annotations.Nullable;

public record List(TypeTemplate element) implements TypeTemplate {
   @Override
   public int size() {
      return this.element.size();
   }

   @Override
   public TypeFamily apply(final TypeFamily family) {
      return new TypeFamily() {
         @Override
         public Type<?> apply(int index) {
            return DSL.list(List.this.element.apply(family).apply(index));
         }
      };
   }

   @Override
   public <A, B> FamilyOptic<A, B> applyO(FamilyOptic<A, B> input, Type<A> aType, Type<B> bType) {
      return TypeFamily.familyOptic(i -> this.cap(this.element.applyO(input, aType, bType).apply(i)));
   }

   private <S, T, A, B> TypedOptic<?, ?, A, B> cap(TypedOptic<S, T, A, B> concreteOptic) {
      return new TypedOptic<>(
            TraversalP.Mu.TYPE_TOKEN,
            DSL.list(concreteOptic.sType()),
            DSL.list(concreteOptic.tType()),
            concreteOptic.sType(),
            concreteOptic.tType(),
            Optics.listTraversal()
         )
         .compose(concreteOptic);
   }

   @Override
   public <FT, FR> Either<TypeTemplate, Type.FieldNotFoundException> findFieldOrType(int index, @Nullable String name, Type<FT> type, Type<FR> resultType) {
      return this.element.findFieldOrType(index, name, type, resultType).mapLeft(List::new);
   }

   @Override
   public IntFunction<RewriteResult<?, ?>> hmap(TypeFamily family, IntFunction<RewriteResult<?, ?>> function) {
      return i -> {
         RewriteResult<?, ?> view = this.element.hmap(family, function).apply(i);
         return this.cap(this.apply(family).apply(i), view);
      };
   }

   private <E> RewriteResult<?, ?> cap(Type<?> type, RewriteResult<E, ?> view) {
      return ((List.ListType)type).fix(view);
   }

   @Override
   public String toString() {
      return "List[" + this.element + "]";
   }

   public static final class ListType<A> extends Type<java.util.List<A>> {
      protected final Type<A> element;

      public ListType(Type<A> element) {
         this.element = element;
      }

      @Override
      public RewriteResult<java.util.List<A>, ?> all(TypeRewriteRule rule, boolean recurse, boolean checkIndex) {
         RewriteResult<A, ?> view = this.element.rewriteOrNop(rule);
         return this.fix(view);
      }

      @Override
      public Optional<RewriteResult<java.util.List<A>, ?>> one(TypeRewriteRule rule) {
         return rule.rewrite(this.element).map(this::fix);
      }

      @Override
      public Type<?> updateMu(RecursiveTypeFamily newFamily) {
         return DSL.list(this.element.updateMu(newFamily));
      }

      @Override
      public TypeTemplate buildTemplate() {
         return DSL.list(this.element.template());
      }

      @Override
      public Optional<java.util.List<A>> point(DynamicOps<?> ops) {
         return Optional.of(ImmutableList.of());
      }

      @Override
      public <FT, FR> Either<TypedOptic<java.util.List<A>, ?, FT, FR>, Type.FieldNotFoundException> findTypeInChildren(
         Type<FT> type, Type<FR> resultType, Type.TypeMatcher<FT, FR> matcher, boolean recurse
      ) {
         Either<TypedOptic<A, ?, FT, FR>, Type.FieldNotFoundException> firstFieldLens = this.element.findType(type, resultType, matcher, recurse);
         return firstFieldLens.mapLeft(this::capLeft);
      }

      private <FT, FR, B> TypedOptic<java.util.List<A>, ?, FT, FR> capLeft(TypedOptic<A, B, FT, FR> optic) {
         return TypedOptic.list(optic.sType(), optic.tType()).compose(optic);
      }

      public <B> RewriteResult<java.util.List<A>, ?> fix(RewriteResult<A, B> view) {
         return opticView(this, view, TypedOptic.list(this.element, view.view().newType()));
      }

      @Override
      public Codec<java.util.List<A>> buildCodec() {
         return Codec.list(this.element.codec());
      }

      @Override
      public String toString() {
         return "List[" + this.element + "]";
      }

      @Override
      public boolean equals(Object obj, boolean ignoreRecursionPoints, boolean checkIndex) {
         return obj instanceof List.ListType && this.element.equals(((List.ListType)obj).element, ignoreRecursionPoints, checkIndex);
      }

      @Override
      public int hashCode() {
         return this.element.hashCode();
      }

      public Type<A> getElement() {
         return this.element;
      }
   }
}
