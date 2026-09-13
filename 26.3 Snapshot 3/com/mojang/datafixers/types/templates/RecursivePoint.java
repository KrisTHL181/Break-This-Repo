package com.mojang.datafixers.types.templates;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.FamilyOptic;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.TypedOptic;
import com.mojang.datafixers.View;
import com.mojang.datafixers.functions.Functions;
import com.mojang.datafixers.functions.PointFreeRule;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.families.RecursiveTypeFamily;
import com.mojang.datafixers.types.families.TypeFamily;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.BitSet;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public record RecursivePoint(int index) implements TypeTemplate {
   @Override
   public int size() {
      return this.index + 1;
   }

   @Override
   public TypeFamily apply(TypeFamily family) {
      final Type<?> result = family.apply(this.index);
      return new TypeFamily() {
         @Override
         public Type<?> apply(int index) {
            return result;
         }
      };
   }

   @Override
   public <A, B> FamilyOptic<A, B> applyO(FamilyOptic<A, B> input, Type<A> aType, Type<B> bType) {
      return TypeFamily.familyOptic(i -> input.apply(this.index));
   }

   @Override
   public <FT, FR> Either<TypeTemplate, Type.FieldNotFoundException> findFieldOrType(int index, @Nullable String name, Type<FT> type, Type<FR> resultType) {
      return Either.right(new Type.FieldNotFoundException("Recursion point"));
   }

   @Override
   public IntFunction<RewriteResult<?, ?>> hmap(TypeFamily family, IntFunction<RewriteResult<?, ?>> function) {
      return i -> {
         RewriteResult<?, ?> result = function.apply(this.index);
         return this.cap(family, result);
      };
   }

   public <S, T> RewriteResult<S, T> cap(TypeFamily family, RewriteResult<S, T> result) {
      Type<?> sourceType = family.apply(this.index);
      if (!(sourceType instanceof RecursivePoint.RecursivePointType)) {
         throw new IllegalArgumentException("Type error: Recursive point template template got a non-recursice type as an input.");
      }

      if (!Objects.equals(result.view().type(), sourceType)) {
         throw new IllegalArgumentException("Type error: hmap function input type");
      }

      BitSet bitSet = (BitSet)result.recData().clone();
      bitSet.set(this.index);
      return RewriteResult.create(result.view(), bitSet);
   }

   @Override
   public String toString() {
      return "Id[" + this.index + "]";
   }

   public static final class RecursivePointType<A> extends Type<A> {
      private final RecursiveTypeFamily family;
      private final int index;
      private final Supplier<Type<A>> delegate;
      @Nullable
      private volatile Type<A> type;

      public RecursivePointType(RecursiveTypeFamily family, int index, Supplier<Type<A>> delegate) {
         this.family = family;
         this.index = index;
         this.delegate = delegate;
      }

      public RecursiveTypeFamily family() {
         return this.family;
      }

      public int index() {
         return this.index;
      }

      public Type<A> unfold() {
         if (this.type == null) {
            this.type = this.delegate.get();
         }

         return this.type;
      }

      @Override
      protected Codec<A> buildCodec() {
         return new Codec<A>() {
            @Override
            public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
               return RecursivePointType.this.unfold().codec().decode(ops, input).setLifecycle(Lifecycle.experimental());
            }

            @Override
            public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
               return RecursivePointType.this.unfold().codec().encode(input, ops, prefix).setLifecycle(Lifecycle.experimental());
            }
         };
      }

      @Override
      public RewriteResult<A, ?> all(TypeRewriteRule rule, boolean recurse, boolean checkIndex) {
         return this.unfold().all(rule, recurse, checkIndex);
      }

      @Override
      public Optional<RewriteResult<A, ?>> one(TypeRewriteRule rule) {
         return this.unfold().one(rule);
      }

      @Override
      public Optional<RewriteResult<A, ?>> everywhere(TypeRewriteRule rule, PointFreeRule optimizationRule, boolean recurse, boolean checkIndex) {
         if (recurse) {
            Optional<RewriteResult<A, ?>> result = this.family.everywhere(this.index, rule, optimizationRule).map(view -> (RewriteResult<A, ?>)view);
            if (result.isPresent()) {
               return result;
            }
         }

         return Optional.of(RewriteResult.nop(this));
      }

      @Override
      public Type<?> updateMu(RecursiveTypeFamily newFamily) {
         return newFamily.apply(this.index);
      }

      @Override
      public TypeTemplate buildTemplate() {
         return DSL.id(this.index);
      }

      @Override
      public Optional<TaggedChoice.TaggedChoiceType<?>> findChoiceType(String name, int index) {
         return this.unfold().findChoiceType(name, this.index);
      }

      @Override
      public Optional<Type<?>> findCheckedType(int index) {
         return this.unfold().findCheckedType(this.index);
      }

      @Override
      public Optional<Type<?>> findFieldTypeOpt(String name) {
         return this.unfold().findFieldTypeOpt(name);
      }

      @Override
      public Optional<A> point(DynamicOps<?> ops) {
         return this.unfold().point(ops);
      }

      @Override
      public <FT, FR> Either<TypedOptic<A, ?, FT, FR>, Type.FieldNotFoundException> findTypeInChildren(
         Type<FT> type, Type<FR> resultType, Type.TypeMatcher<FT, FR> matcher, boolean recurse
      ) {
         return this.family.findType(this.index, type, resultType, matcher, recurse).mapLeft(o -> {
            if (!Objects.equals(this, o.sType())) {
               throw new IllegalStateException(":/");
            } else {
               return (TypedOptic<A, ?, FT, FR>)o;
            }
         });
      }

      @Override
      public String toString() {
         return "MuType[" + this.family.name() + "_" + this.index + "]";
      }

      @Override
      public boolean equals(Object obj, boolean ignoreRecursionPoints, boolean checkIndex) {
         return !(obj instanceof RecursivePoint.RecursivePointType<?> type)
            ? false
            : (ignoreRecursionPoints || Objects.equals(this.family, type.family)) && this.index == type.index;
      }

      @Override
      public int hashCode() {
         int result = this.family.hashCode();
         return 31 * result + this.index;
      }

      public View<A, A> in() {
         return View.create(Functions.in(this));
      }

      public View<A, A> out() {
         return View.create(Functions.out(this));
      }
   }
}
