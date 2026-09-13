package com.mojang.datafixers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.reflect.TypeToken;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.K2;
import com.mojang.datafixers.optics.InjTagged;
import com.mojang.datafixers.optics.Optic;
import com.mojang.datafixers.optics.Optics;
import com.mojang.datafixers.optics.profunctors.Cartesian;
import com.mojang.datafixers.optics.profunctors.Cocartesian;
import com.mojang.datafixers.optics.profunctors.Profunctor;
import com.mojang.datafixers.optics.profunctors.TraversalP;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record TypedOptic<S, T, A, B>(Set<TypeToken<? extends K1>> bounds, List<? extends TypedOptic.Element<?, ?, ?, ?>> elements) {
   public TypedOptic(TypeToken<? extends K1> proofBound, Type<S> sType, Type<T> tType, Type<A> aType, Type<B> bType, Optic<?, S, T, A, B> optic) {
      this(ImmutableSet.of(proofBound), sType, tType, aType, bType, optic);
   }

   public TypedOptic(Set<TypeToken<? extends K1>> proofBounds, Type<S> sType, Type<T> tType, Type<A> aType, Type<B> bType, Optic<?, S, T, A, B> optic) {
      this(proofBounds, List.of(new TypedOptic.Element<>(sType, tType, aType, bType, optic)));
   }

   public <P extends K2, Proof2 extends K1> App2<P, S, T> apply(TypeToken<Proof2> token, App<Proof2, P> proof, App2<P, A, B> argument) {
      return this.<Proof2>upCast(token).orElseThrow(() -> new IllegalArgumentException("Couldn't upcast")).eval(proof).apply(argument);
   }

   public Optic<?, S, T, ?, ?> outermost() {
      return this.outermostElement().optic();
   }

   public Optic<?, ?, ?, A, B> innermost() {
      return this.innermostElement().optic();
   }

   private TypedOptic.Element<S, T, ?, ?> outermostElement() {
      return (TypedOptic.Element<S, T, ?, ?>)this.elements.get(0);
   }

   private TypedOptic.Element<?, ?, A, B> innermostElement() {
      return (TypedOptic.Element<?, ?, A, B>)this.elements.get(this.elements.size() - 1);
   }

   public Type<S> sType() {
      return this.outermostElement().sType();
   }

   public Type<T> tType() {
      return this.outermostElement().tType();
   }

   public Type<A> aType() {
      return this.innermostElement().aType();
   }

   public Type<B> bType() {
      return this.innermostElement().bType();
   }

   public <A1, B1> TypedOptic<S, T, A1, B1> compose(TypedOptic<A, B, A1, B1> other) {
      Builder<TypeToken<? extends K1>> proof = ImmutableSet.builder();
      proof.addAll(this.bounds);
      proof.addAll(other.bounds);
      com.google.common.collect.ImmutableList.Builder<TypedOptic.Element<?, ?, ?, ?>> elements = ImmutableList.builderWithExpectedSize(
         this.elements().size() + other.elements().size()
      );
      elements.addAll(this.elements());
      elements.addAll(other.elements());
      return (TypedOptic<S, T, A1, B1>)(new TypedOptic<>(proof.build(), elements.build()));
   }

   // ===== 修改：使用 toCollection(ArrayList::new) 替代 toList()，修复类型推断 =====
   public <Proof2 extends K1> Optional<Optic<? super Proof2, S, T, A, B>> upCast(TypeToken<Proof2> proof) {
      if (instanceOf(this.bounds, proof)) {
         if (this.elements.size() == 1) {
            return Optional.of((Optic<? super Proof2, S, T, A, B>)this.elements.get(0).optic());
         }

         List<Optic<? super Proof2, ?, ?, ?, ?>> optics = this.elements.stream()
            .<Optic<? super Proof2, ?, ?, ?, ?>>map(e -> (Optic<? super Proof2, ?, ?, ?, ?>)e.optic())
            .collect(Collectors.toCollection(ArrayList::new));
         return Optional.of(new Optic.CompositionOptic<>(optics));
      } else {
         return Optional.empty();
      }
   }

   public static <Proof2 extends K1> boolean instanceOf(Collection<TypeToken<? extends K1>> bounds, TypeToken<Proof2> proof) {
      return bounds.stream().allMatch(bound -> bound.isSupertypeOf(proof));
   }

   public static <S, T> TypedOptic<S, T, S, T> adapter(Type<S> sType, Type<T> tType) {
      return new TypedOptic<>(Profunctor.Mu.TYPE_TOKEN, sType, tType, sType, tType, Optics.id());
   }

   public static <F, G, F2> TypedOptic<Pair<F, G>, Pair<F2, G>, F, F2> proj1(Type<F> fType, Type<G> gType, Type<F2> newType) {
      return new TypedOptic<>(Cartesian.Mu.TYPE_TOKEN, DSL.and(fType, gType), DSL.and(newType, gType), fType, newType, Optics.proj1());
   }

   public static <F, G, G2> TypedOptic<Pair<F, G>, Pair<F, G2>, G, G2> proj2(Type<F> fType, Type<G> gType, Type<G2> newType) {
      return new TypedOptic<>(Cartesian.Mu.TYPE_TOKEN, DSL.and(fType, gType), DSL.and(fType, newType), gType, newType, Optics.proj2());
   }

   public static <F, G, F2> TypedOptic<Either<F, G>, Either<F2, G>, F, F2> inj1(Type<F> fType, Type<G> gType, Type<F2> newType) {
      return new TypedOptic<>(Cocartesian.Mu.TYPE_TOKEN, DSL.or(fType, gType), DSL.or(newType, gType), fType, newType, Optics.inj1());
   }

   public static <F, G, G2> TypedOptic<Either<F, G>, Either<F, G2>, G, G2> inj2(Type<F> fType, Type<G> gType, Type<G2> newType) {
      return new TypedOptic<>(Cocartesian.Mu.TYPE_TOKEN, DSL.or(fType, gType), DSL.or(fType, newType), gType, newType, Optics.inj2());
   }

   public static <K, V, K2> TypedOptic<List<Pair<K, V>>, List<Pair<K2, V>>, K, K2> compoundListKeys(Type<K> aType, Type<K2> bType, Type<V> valueType) {
      return new TypedOptic<>(
            TraversalP.Mu.TYPE_TOKEN,
            DSL.compoundList(aType, valueType),
            DSL.compoundList(bType, valueType),
            DSL.and(aType, valueType),
            DSL.and(bType, valueType),
            Optics.listTraversal()
         )
         .compose(new TypedOptic<>(TraversalP.Mu.TYPE_TOKEN, DSL.and(aType, valueType), DSL.and(bType, valueType), aType, bType, Optics.proj1()));
   }

   public static <K, V, V2> TypedOptic<List<Pair<K, V>>, List<Pair<K, V2>>, V, V2> compoundListElements(Type<K> keyType, Type<V> aType, Type<V2> bType) {
      return new TypedOptic<>(
            TraversalP.Mu.TYPE_TOKEN,
            DSL.compoundList(keyType, aType),
            DSL.compoundList(keyType, bType),
            DSL.and(keyType, aType),
            DSL.and(keyType, bType),
            Optics.listTraversal()
         )
         .compose(new TypedOptic<>(TraversalP.Mu.TYPE_TOKEN, DSL.and(keyType, aType), DSL.and(keyType, bType), aType, bType, Optics.proj2()));
   }

   public static <A, B> TypedOptic<List<A>, List<B>, A, B> list(Type<A> aType, Type<B> bType) {
      return new TypedOptic<>(TraversalP.Mu.TYPE_TOKEN, DSL.list(aType), DSL.list(bType), aType, bType, Optics.listTraversal());
   }

   public static <K, A, B> TypedOptic<Pair<K, ?>, Pair<K, ?>, A, B> tagged(TaggedChoice.TaggedChoiceType<K> sType, K key, Type<A> aType, Type<B> bType) {
      return new TypedOptic<>(Cocartesian.Mu.TYPE_TOKEN, sType, replaceTagged(sType, key, aType, bType), aType, bType, new InjTagged<>(key));
   }

   private static <K, A, B> Type<Pair<K, ?>> replaceTagged(TaggedChoice.TaggedChoiceType<K> sType, K key, Type<A> aType, Type<B> bType) {
      if (Objects.equals(aType, bType)) {
         return sType;
      }

      if (!Objects.equals(sType.types().get(key), aType)) {
         throw new IllegalArgumentException("Focused type doesn't match.");
      }

      Map<K, Type<?>> newTypes = Maps.newHashMap(sType.types());
      newTypes.put(key, bType);
      return DSL.taggedChoiceType(sType.getName(), sType.getKeyType(), newTypes);
   }

   public TypedOptic<S, T, A, B> castOuter(Type<S> sType, Type<T> tType) {
      return this.castOuterUnchecked(sType, tType);
   }

   public <S2, T2> TypedOptic<S2, T2, A, B> castOuterUnchecked(Type<S2> sType, Type<T2> tType) {
      List<TypedOptic.Element<?, ?, ?, ?>> newElements = new ArrayList<>(this.elements);
      newElements.set(0, newElements.get(0).castOuterUnchecked(sType, tType));
      return (TypedOptic<S2, T2, A, B>)(new TypedOptic<>(this.bounds, newElements));
   }

   @Override
   public String toString() {
      return "(" + this.elements.stream().map(Object::toString).collect(Collectors.joining(" ◦ ")) + ")";
   }

   public record Element<S, T, A, B>(Type<S> sType, Type<T> tType, Type<A> aType, Type<B> bType, Optic<?, S, T, A, B> optic) {
      public <S2, T2> TypedOptic.Element<S2, T2, A, B> castOuterUnchecked(Type<S2> sType, Type<T2> tType) {
         return new TypedOptic.Element<>(sType, tType, this.aType, this.bType, (Optic<?, S2, T2, A, B>)this.optic);
      }

      @Override
      public String toString() {
         return this.optic.toString();
      }
   }
}