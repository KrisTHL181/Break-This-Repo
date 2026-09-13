package com.mojang.datafixers.types.templates;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.FamilyOptic;
import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.TypedOptic;
import com.mojang.datafixers.View;
import com.mojang.datafixers.functions.Functions;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.optics.Affine;
import com.mojang.datafixers.optics.Lens;
import com.mojang.datafixers.optics.Optic;
import com.mojang.datafixers.optics.Optics;
import com.mojang.datafixers.optics.Traversal;
import com.mojang.datafixers.optics.profunctors.AffineP;
import com.mojang.datafixers.optics.profunctors.Cartesian;
import com.mojang.datafixers.optics.profunctors.TraversalP;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.families.RecursiveTypeFamily;
import com.mojang.datafixers.types.families.TypeFamily;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

public final class TaggedChoice<K> implements TypeTemplate {
   private final String name;
   private final Type<K> keyType;
   private final Object2ObjectMap<K, TypeTemplate> templates;
   private final Map<Pair<TypeFamily, Integer>, Type<?>> types = Maps.newConcurrentMap();
   private final int size;

   public TaggedChoice(String name, Type<K> keyType, Object2ObjectMap<K, TypeTemplate> templates) {
      this.name = name;
      this.keyType = keyType;
      this.templates = templates;
      this.size = templates.values().stream().mapToInt(TypeTemplate::size).max().orElse(0);
   }

   @Override
   public int size() {
      return this.size;
   }

   @Override
   public TypeFamily apply(TypeFamily family) {
      return index -> this.types.computeIfAbsent(Pair.of(family, index), key -> {
         Object2ObjectMap<K, Type<?>> types = new Object2ObjectOpenHashMap(this.templates.size());
         ObjectIterator var3 = Object2ObjectMaps.fastIterable(this.templates).iterator();

         while (var3.hasNext()) {
            Entry<K, TypeTemplate> entry = (Entry<K, TypeTemplate>)var3.next();
            types.put(entry.getKey(), entry.getValue().apply(key.getFirst()).apply(key.getSecond()));
         }

         return DSL.taggedChoiceType(this.name, this.keyType, types);
      });
   }

   @Override
   public <A, B> FamilyOptic<A, B> applyO(FamilyOptic<A, B> input, Type<A> aType, Type<B> bType) {
      throw new UnsupportedOperationException();
   }

   @Override
   public <A, B> Either<TypeTemplate, Type.FieldNotFoundException> findFieldOrType(int index, @Nullable String name, Type<A> type, Type<B> resultType) {
      return Either.right(new Type.FieldNotFoundException("Not implemented"));
   }

   @Override
   public IntFunction<RewriteResult<?, ?>> hmap(TypeFamily family, IntFunction<RewriteResult<?, ?>> function) {
      return index -> {
         RewriteResult<Pair<K, ?>, Pair<K, ?>> result = RewriteResult.nop((TaggedChoice.TaggedChoiceType)this.apply(family).apply(index));
         ObjectIterator var5 = this.templates.entrySet().iterator();

         while (var5.hasNext()) {
            Entry<K, TypeTemplate> entry = (Entry<K, TypeTemplate>)var5.next();
            RewriteResult<?, ?> elementResult = entry.getValue().hmap(family, function).apply(index);
            result = TaggedChoice.TaggedChoiceType.elementResult(entry.getKey(), (TaggedChoice.TaggedChoiceType<K>)result.view().newType(), elementResult)
               .compose(result);
         }

         return result;
      };
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else {
         return !(obj instanceof TaggedChoice<?> other)
            ? false
            : Objects.equals(this.name, other.name) && Objects.equals(this.keyType, other.keyType) && Objects.equals(this.templates, other.templates);
      }
   }

   @Override
   public int hashCode() {
      int result = this.name.hashCode();
      result = 31 * result + this.keyType.hashCode();
      return 31 * result + this.templates.hashCode();
   }

   @Override
   public String toString() {
      return "TaggedChoice[" + this.name + ", " + Joiner.on(", ").withKeyValueSeparator(" -> ").join(this.templates) + "]";
   }

   public static final class TaggedChoiceType<K> extends Type<Pair<K, ?>> {
      private final String name;
      private final Type<K> keyType;
      protected final Object2ObjectMap<K, Type<?>> types;
      private final int hashCode;

      public TaggedChoiceType(String name, Type<K> keyType, Object2ObjectMap<K, Type<?>> types) {
         this.name = name;
         this.keyType = keyType;
         this.types = types;
         this.hashCode = Objects.hash(name, keyType, types);
      }

      @Override
      public RewriteResult<Pair<K, ?>, ?> all(TypeRewriteRule rule, boolean recurse, boolean checkIndex) {
         Object2ObjectMap<K, RewriteResult<?, ?>> results = new Object2ObjectOpenHashMap(this.types.size());
         ObjectIterator newTypes = Object2ObjectMaps.fastIterable(this.types).iterator();

         while (newTypes.hasNext()) {
            Entry<K, Type<?>> entry = (Entry<K, Type<?>>)newTypes.next();
            Optional<? extends RewriteResult<?, ?>> result = rule.rewrite(entry.getValue());
            if (result.isPresent() && !result.get().view().isNop()) {
               results.put(entry.getKey(), result.get());
            }
         }

         if (results.isEmpty()) {
            return RewriteResult.nop(this);
         }

         if (results.size() == 1) {
            Entry<K, ? extends RewriteResult<?, ?>> entry = (Entry<K, ? extends RewriteResult<?, ?>>)results.entrySet().iterator().next();
            return elementResult(entry.getKey(), this, (RewriteResult<?, ?>)entry.getValue());
         }

         Object2ObjectMap<K, Type<?>> newTypesx = new Object2ObjectOpenHashMap(this.types);
         BitSet recData = new BitSet();
         ObjectIterator var12 = Object2ObjectMaps.fastIterable(results).iterator();

         while (var12.hasNext()) {
            Entry<K, ? extends RewriteResult<?, ?>> entry = (Entry<K, ? extends RewriteResult<?, ?>>)var12.next();
            newTypesx.put(entry.getKey(), entry.getValue().view().newType());
            recData.or(entry.getValue().recData());
         }

         return RewriteResult.create(
            View.create(
               Functions.fun(
                  "TaggedChoiceTypeRewriteResult " + results.size(),
                  new TaggedChoice.TaggedChoiceType.RewriteFunc<>(results),
                  this,
                  DSL.taggedChoiceType(this.name, this.keyType, newTypesx)
               )
            ),
            recData
         );
      }

      public static <K, FT, FR> RewriteResult<Pair<K, ?>, Pair<K, ?>> elementResult(K key, TaggedChoice.TaggedChoiceType<K> type, RewriteResult<FT, FR> result) {
         return opticView(type, result, TypedOptic.tagged(type, key, result.view().type(), result.view().newType()));
      }

      @Override
      public Optional<RewriteResult<Pair<K, ?>, ?>> one(TypeRewriteRule rule) {
         ObjectIterator var2 = this.types.entrySet().iterator();

         while (var2.hasNext()) {
            Entry<K, Type<?>> entry = (Entry<K, Type<?>>)var2.next();
            Optional<? extends RewriteResult<?, ?>> elementResult = rule.rewrite(entry.getValue());
            if (elementResult.isPresent()) {
               return Optional.of(elementResult(entry.getKey(), this, (RewriteResult<?, ?>)elementResult.get()));
            }
         }

         return Optional.empty();
      }

      @Override
      public Type<?> updateMu(RecursiveTypeFamily newFamily) {
         Object2ObjectMap<K, Type<?>> newTypes = new Object2ObjectOpenHashMap(this.types.size());
         ObjectIterator var3 = Object2ObjectMaps.fastIterable(this.types).iterator();

         while (var3.hasNext()) {
            it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<K, Type<?>> entry = (it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<K, Type<?>>)var3.next();
            newTypes.put(entry.getKey(), ((Type)entry.getValue()).updateMu(newFamily));
         }

         return DSL.taggedChoiceType(this.name, this.keyType, newTypes);
      }

      @Override
      public TypeTemplate buildTemplate() {
         Object2ObjectMap<K, TypeTemplate> templates = new Object2ObjectOpenHashMap(this.types.size());
         ObjectIterator var2 = Object2ObjectMaps.fastIterable(this.types).iterator();

         while (var2.hasNext()) {
            it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<K, Type<?>> entry = (it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<K, Type<?>>)var2.next();
            templates.put(entry.getKey(), ((Type)entry.getValue()).template());
         }

         return DSL.taggedChoice(this.name, this.keyType, templates);
      }

      @Override
      protected Codec<Pair<K, ?>> buildCodec() {
         return this.keyType
            .codec()
            .partialDispatch(
               this.name, pair -> DataResult.success(pair.getFirst()), key -> this.getMapCodec((K)key).map(codec -> asEntryPair((K)key, (MapCodec<?>)codec))
            );
      }

      private static <K, V> MapCodec<Pair<K, V>> asEntryPair(K key, MapCodec<V> valueCodec) {
         return valueCodec.xmap(value -> Pair.of(key, (V)value), Pair::getSecond);
      }

      // ===== 修改：显式类型转换 =====
      private DataResult<? extends MapCodec<?>> getMapCodec(K key) {
         return Optional.ofNullable((Type)this.types.get(key))
            .map(type -> DataResult.success((MapCodec<?>)MapCodec.assumeMapUnsafe(((Type)type).codec())))
            .orElseGet(() -> DataResult.error(() -> "Unsupported key: " + key));
      }

      @Override
      public Optional<Type<?>> findFieldTypeOpt(String name) {
         return this.types.values().stream().map(t -> t.findFieldTypeOpt(name)).filter(Optional::isPresent).findFirst().flatMap(Function.identity());
      }

      @Override
      public Optional<Pair<K, ?>> point(DynamicOps<?> ops) {
         return this.types
            .entrySet()
            .stream()
            .map(e -> ((Type)e.getValue()).point(ops).map(value -> Pair.of(e.getKey(), value)))
            .filter(Optional::isPresent)
            .findFirst()
            .flatMap(Function.identity())
            .map(p -> (Pair<K, ?>)p);
      }

      public Optional<Typed<Pair<K, ?>>> point(DynamicOps<?> ops, K key, Object value) {
         return !this.types.containsKey(key) ? Optional.empty() : Optional.of(new Typed<>(this, ops, Pair.of(key, value)));
      }

      @Override
      public <FT, FR> Either<TypedOptic<Pair<K, ?>, ?, FT, FR>, Type.FieldNotFoundException> findTypeInChildren(
         Type<FT> type, Type<FR> resultType, Type.TypeMatcher<FT, FR> matcher, boolean recurse
      ) {
         final Map<K, ? extends TypedOptic<?, ?, FT, FR>> optics = this.types
            .entrySet()
            .stream()
            .map(e -> Pair.of(e.getKey(), ((Type)e.getValue()).findType(type, resultType, matcher, recurse)))
            .filter(e -> e.getSecond().left().isPresent())
            .map(e -> e.mapSecond(o -> (TypedOptic)o.left().get()))
            // ===== 修改：显式类型参数 =====
            .collect(Pair.<K, TypedOptic<?, ?, FT, FR>>toMap());
         if (optics.isEmpty()) {
            return Either.right(new Type.FieldNotFoundException("Not found in any choices"));
         }

         if (optics.size() == 1) {
            Entry<K, ? extends TypedOptic<?, ?, FT, FR>> entry = optics.entrySet().iterator().next();
            return Either.left(this.cap(this, entry.getKey(), (TypedOptic<?, ?, FT, FR>)entry.getValue()));
         }

         Set<TypeToken<? extends K1>> bounds = Sets.newHashSet();
         optics.values().forEach(o -> bounds.addAll(o.bounds()));
         Optic<?, Pair<K, ?>, Pair<K, ?>, FT, FR> optic;
         TypeToken<? extends K1> bound;
         if (TypedOptic.instanceOf(bounds, Cartesian.Mu.TYPE_TOKEN) && optics.size() == this.types.size()) {
            bound = Cartesian.Mu.TYPE_TOKEN;
            optic = new Lens<Pair<K, ?>, Pair<K, ?>, FT, FR>() {
               public FT view(Pair<K, ?> s) {
                  TypedOptic<?, ?, FT, FR> opticx = (TypedOptic<?, ?, FT, FR>)optics.get(s.getFirst());
                  return (FT)this.capView(s, opticx);
               }

               private <S, T> FT capView(Pair<K, ?> s, TypedOptic<S, T, FT, FR> opticx) {
                  return Optics.toLens(opticx.<Cartesian.Mu>upCast(Cartesian.Mu.TYPE_TOKEN).orElseThrow(IllegalArgumentException::new)).view((S)s.getSecond());
               }

               public Pair<K, ?> update(FR b, Pair<K, ?> s) {
                  TypedOptic<?, ?, FT, FR> opticx = (TypedOptic<?, ?, FT, FR>)optics.get(s.getFirst());
                  return this.capUpdate(b, s, opticx);
               }

               private <S, T> Pair<K, ?> capUpdate(FR b, Pair<K, ?> s, TypedOptic<S, T, FT, FR> opticx) {
                  return Pair.of(
                     s.getFirst(),
                     Optics.toLens(opticx.<Cartesian.Mu>upCast(Cartesian.Mu.TYPE_TOKEN).orElseThrow(IllegalArgumentException::new)).update(b, (S)s.getSecond())
                  );
               }
            };
         } else if (TypedOptic.instanceOf(bounds, AffineP.Mu.TYPE_TOKEN)) {
            bound = AffineP.Mu.TYPE_TOKEN;
            optic = new Affine<Pair<K, ?>, Pair<K, ?>, FT, FR>() {
               public Either<Pair<K, ?>, FT> preview(Pair<K, ?> s) {
                  if (!optics.containsKey(s.getFirst())) {
                     return Either.left(s);
                  }

                  TypedOptic<?, ?, FT, FR> opticx = (TypedOptic<?, ?, FT, FR>)optics.get(s.getFirst());
                  return this.capPreview(s, opticx);
               }

               private <S, T> Either<Pair<K, ?>, FT> capPreview(Pair<K, ?> s, TypedOptic<S, T, FT, FR> opticx) {
                  return Optics.toAffine(opticx.<AffineP.Mu>upCast(AffineP.Mu.TYPE_TOKEN).orElseThrow(IllegalArgumentException::new))
                     .preview((S)s.getSecond())
                     .mapLeft(t -> Pair.of(s.getFirst(), t));
               }

               public Pair<K, ?> set(FR b, Pair<K, ?> s) {
                  if (!optics.containsKey(s.getFirst())) {
                     return s;
                  }

                  TypedOptic<?, ?, FT, FR> opticx = (TypedOptic<?, ?, FT, FR>)optics.get(s.getFirst());
                  return this.capSet(b, s, opticx);
               }

               private <S, T> Pair<K, ?> capSet(FR b, Pair<K, ?> s, TypedOptic<S, T, FT, FR> opticx) {
                  return Pair.of(
                     s.getFirst(),
                     Optics.toAffine(opticx.<AffineP.Mu>upCast(AffineP.Mu.TYPE_TOKEN).orElseThrow(IllegalArgumentException::new)).set(b, (S)s.getSecond())
                  );
               }
            };
         } else {
            if (!TypedOptic.instanceOf(bounds, TraversalP.Mu.TYPE_TOKEN)) {
               throw new IllegalStateException("Could not merge TaggedChoiceType optics, unknown bound: " + Arrays.toString(bounds.toArray()));
            }

            bound = TraversalP.Mu.TYPE_TOKEN;
            optic = new Traversal<Pair<K, ?>, Pair<K, ?>, FT, FR>() {
               @Override
               public <F extends K1> FunctionType<Pair<K, ?>, App<F, Pair<K, ?>>> wander(Applicative<F, ?> applicative, FunctionType<FT, App<F, FR>> input) {
                  return pair -> {
                     if (!optics.containsKey(pair.getFirst())) {
                        return applicative.point(pair);
                     }

                     TypedOptic<?, ?, FT, FR> opticx = (TypedOptic<?, ?, FT, FR>)optics.get(pair.getFirst());
                     return this.capTraversal(applicative, input, pair, opticx);
                  };
               }

               private <S, T, F extends K1> App<F, Pair<K, ?>> capTraversal(
                  Applicative<F, ?> applicative, FunctionType<FT, App<F, FR>> input, Pair<K, ?> pair, TypedOptic<S, T, FT, FR> opticx
               ) {
                  Traversal<S, T, FT, FR> traversal = Optics.toTraversal(
                     opticx.<TraversalP.Mu>upCast(TraversalP.Mu.TYPE_TOKEN).orElseThrow(IllegalArgumentException::new)
                  );
                  return applicative.ap(value -> Pair.of(pair.getFirst(), value), traversal.wander(applicative, input).apply((S)pair.getSecond()));
               }
            };
         }

         Object2ObjectMap<K, Type<?>> newTypes = new Object2ObjectOpenHashMap(this.types);
         ObjectIterator var10 = Object2ObjectMaps.fastIterable(newTypes).iterator();

         while (var10.hasNext()) {
            it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<K, Type<?>> entry = (it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<K, Type<?>>)var10.next();
            TypedOptic<?, ?, FT, FR> typeOptic = (TypedOptic<?, ?, FT, FR>)optics.get(entry.getKey());
            if (typeOptic != null) {
               entry.setValue(typeOptic.tType());
            }
         }

         return Either.left(new TypedOptic<>(bound, this, DSL.taggedChoiceType(this.name, this.keyType, newTypes), type, resultType, optic));
      }

      private <S, T, FT, FR> TypedOptic<Pair<K, ?>, Pair<K, ?>, FT, FR> cap(TaggedChoice.TaggedChoiceType<K> choiceType, K key, TypedOptic<S, T, FT, FR> optic) {
         return TypedOptic.tagged(choiceType, key, optic.sType(), optic.tType()).compose(optic);
      }

      @Override
      public Optional<TaggedChoice.TaggedChoiceType<?>> findChoiceType(String name, int index) {
         return Objects.equals(name, this.name) ? Optional.of(this) : Optional.empty();
      }

      @Override
      public Optional<Type<?>> findCheckedType(int index) {
         return this.types.values().stream().map(type -> type.findCheckedType(index)).filter(Optional::isPresent).findFirst().flatMap(Function.identity());
      }

      @Override
      public boolean equals(Object obj, boolean ignoreRecursionPoints, boolean checkIndex) {
         if (this == obj) {
            return true;
         } else if (!(obj instanceof TaggedChoice.TaggedChoiceType<?> other)) {
            return false;
         } else {
            if (!Objects.equals(this.name, other.name)) {
               return false;
            }

            if (!this.keyType.equals(other.keyType, ignoreRecursionPoints, checkIndex)) {
               return false;
            }

            if (this.types.size() != other.types.size()) {
               return false;
            }

            ObjectIterator var5 = this.types.entrySet().iterator();

            while (var5.hasNext()) {
               Entry<K, Type<?>> entry = (Entry<K, Type<?>>)var5.next();
               if (!entry.getValue().equals(other.types.get(entry.getKey()), ignoreRecursionPoints, checkIndex)) {
                  return false;
               }
            }

            return true;
         }
      }

      @Override
      public int hashCode() {
         return this.hashCode;
      }

      @Override
      public String toString() {
         return "TaggedChoiceType[" + this.name + ", " + Joiner.on(", \n").withKeyValueSeparator(" -> ").join(this.types) + "]\n";
      }

      public String getName() {
         return this.name;
      }

      public Type<K> getKeyType() {
         return this.keyType;
      }

      public boolean hasType(K key) {
         return this.types.containsKey(key);
      }

      public Map<K, Type<?>> types() {
         return this.types;
      }

      private static final class RewriteFunc<K> implements Function<DynamicOps<?>, Function<Pair<K, ?>, Pair<K, ?>>> {
         private final Map<K, ? extends RewriteResult<?, ?>> results;

         public RewriteFunc(Map<K, ? extends RewriteResult<?, ?>> results) {
            this.results = results;
         }

         public FunctionType<Pair<K, ?>, Pair<K, ?>> apply(DynamicOps<?> ops) {
            return input -> {
               RewriteResult<?, ?> result = (RewriteResult<?, ?>)this.results.get(input.getFirst());
               return result == null ? input : this.capRuleApply(ops, input, result);
            };
         }

         private <A, B> Pair<K, B> capRuleApply(DynamicOps<?> ops, Pair<K, ?> input, RewriteResult<A, B> result) {
            return input.mapSecond(v -> result.view().function().evalCached().apply(ops).apply((A)v));
         }

         @Override
         public boolean equals(Object o) {
            if (this == o) {
               return true;
            } else if (o != null && this.getClass() == o.getClass()) {
               TaggedChoice.TaggedChoiceType.RewriteFunc<?> that = (TaggedChoice.TaggedChoiceType.RewriteFunc<?>)o;
               return Objects.equals(this.results, that.results);
            } else {
               return false;
            }
         }

         @Override
         public int hashCode() {
            return this.results.hashCode();
         }
      }
   }
}