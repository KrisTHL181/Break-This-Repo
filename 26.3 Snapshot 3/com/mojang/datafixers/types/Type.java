package com.mojang.datafixers.types;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.FieldFinder;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.TypedOptic;
import com.mojang.datafixers.View;
import com.mojang.datafixers.functions.Functions;
import com.mojang.datafixers.functions.PointFreeRule;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.types.families.RecursiveTypeFamily;
import com.mojang.datafixers.types.templates.TaggedChoice;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public abstract class Type<A> implements App<Type.Mu, A> {
   private static final Map<Type.RewriteCacheKey, CompletableFuture<Optional<? extends RewriteResult<?, ?>>>> PENDING_REWRITE_CACHE = Maps.newConcurrentMap();
   private static final Map<Type.RewriteCacheKey, Optional<? extends RewriteResult<?, ?>>> REWRITE_CACHE = Maps.newConcurrentMap();
   @Nullable
   private TypeTemplate template;
   @Nullable
   private Codec<A> codec;

   public static <A> Type<A> unbox(App<Type.Mu, A> box) {
      return (Type<A>)box;
   }

   public RewriteResult<A, ?> rewriteOrNop(TypeRewriteRule rule) {
      return DataFixUtils.orElseGet(rule.rewrite(this), () -> RewriteResult.nop(this));
   }

   public static <S, T, A, B> RewriteResult<S, T> opticView(Type<S> type, RewriteResult<A, B> view, TypedOptic<S, T, A, B> optic) {
      return (RewriteResult<S, T>)(view.view().isNop()
         ? RewriteResult.nop(type)
         : RewriteResult.create(
            View.create(Functions.app(Functions.profunctorTransformer((TypedOptic<A, B, A, B>)optic), view.view().function())), view.recData()
         ));
   }

   public RewriteResult<A, ?> all(TypeRewriteRule rule, boolean recurse, boolean checkIndex) {
      return RewriteResult.nop(this);
   }

   public Optional<RewriteResult<A, ?>> one(TypeRewriteRule rule) {
      return Optional.empty();
   }

   public Optional<RewriteResult<A, ?>> everywhere(TypeRewriteRule rule, PointFreeRule optimizationRule, boolean recurse, boolean checkIndex) {
      TypeRewriteRule rule2 = TypeRewriteRule.seq(
         TypeRewriteRule.orElse(rule, TypeRewriteRule::nop),
         TypeRewriteRule.all(TypeRewriteRule.everywhere(rule, optimizationRule, recurse, checkIndex), recurse, checkIndex)
      );
      return this.rewrite(rule2, optimizationRule);
   }

   public Type<?> updateMu(RecursiveTypeFamily newFamily) {
      return this;
   }

   public TypeTemplate template() {
      if (this.template == null) {
         this.template = this.buildTemplate();
      }

      return this.template;
   }

   public abstract TypeTemplate buildTemplate();

   public Optional<TaggedChoice.TaggedChoiceType<?>> findChoiceType(String name, int index) {
      return Optional.empty();
   }

   public Optional<Type<?>> findCheckedType(int index) {
      return Optional.empty();
   }

   public final <T> DataResult<Pair<A, Dynamic<T>>> read(Dynamic<T> input) {
      return this.codec().decode(input.getOps(), input.getValue()).map(v -> v.mapSecond(t -> new Dynamic<>(input.getOps(), (T)t)));
   }

   public final Codec<A> codec() {
      if (this.codec == null) {
         this.codec = this.buildCodec();
      }

      return this.codec;
   }

   protected abstract Codec<A> buildCodec();

   public final <T> DataResult<T> write(DynamicOps<T> ops, A value) {
      return this.codec().encode(value, ops, ops.empty());
   }

   public final <T> DataResult<Dynamic<T>> writeDynamic(DynamicOps<T> ops, A value) {
      return this.write(ops, value).map(result -> new Dynamic<>(ops, (T)result));
   }

   public <T> DataResult<Pair<Typed<A>, T>> readTyped(Dynamic<T> input) {
      return this.readTyped(input.getOps(), input.getValue());
   }

   public <T> DataResult<Pair<Typed<A>, T>> readTyped(DynamicOps<T> ops, T input) {
      return this.codec().decode(ops, input).map(vo -> vo.mapFirst(v -> new Typed<>(this, ops, (A)v)));
   }

   public <T> DataResult<Pair<Optional<?>, T>> read(DynamicOps<T> ops, TypeRewriteRule rule, PointFreeRule fRule, T input) {
      return this.codec()
         .decode(ops, input)
         .map(vo -> vo.mapFirst(v -> this.rewrite(rule, fRule).map(r -> ((Function)r.view().function().evalCached().apply(ops)).apply(v))));
   }

   public <T> DataResult<T> readAndWrite(DynamicOps<T> ops, Type<?> expectedType, TypeRewriteRule rule, PointFreeRule fRule, T input) {
      Optional<RewriteResult<A, ?>> rewriteResult = this.rewrite(rule, fRule);
      if (!rewriteResult.isPresent()) {
         return DataResult.error(() -> "Could not build a rewrite rule: " + rule + " " + fRule, input);
      }

      View<A, ?> view = rewriteResult.get().view();
      return view.isNop()
         ? DataResult.success(input)
         : this.codec().decode(ops, input).flatMap(pair -> this.capWrite(ops, expectedType, pair.getSecond(), pair.getFirst(), view));
   }

   private <T, B> DataResult<T> capWrite(DynamicOps<T> ops, Type<?> expectedType, T rest, A value, View<A, B> f) {
      if (!expectedType.equals(f.newType(), true, true)) {
         return DataResult.error(() -> "Rewritten type doesn't match");
      }

      B fixed = f.function().evalCached().apply(ops).apply(value);
      return f.newType().codec().encode(fixed, ops, rest);
   }

   public Optional<RewriteResult<A, ?>> rewrite(TypeRewriteRule rule, PointFreeRule fRule) {
      Type.RewriteCacheKey key = new Type.RewriteCacheKey(this, rule, fRule);
      Optional<? extends RewriteResult<?, ?>> rewrite = REWRITE_CACHE.get(key);
      if (rewrite != null) {
         return (Optional<RewriteResult<A, ?>>)rewrite;
      } else {
         AtomicReference<CompletableFuture<Optional<? extends RewriteResult<?, ?>>>> ref = new AtomicReference<>();
         CompletableFuture<Optional<? extends RewriteResult<?, ?>>> pending = PENDING_REWRITE_CACHE.computeIfAbsent(key, k -> {
            CompletableFuture<Optional<? extends RewriteResult<?, ?>>> value = new CompletableFuture<>();
            ref.setPlain(value);
            return value;
         });
         if (ref.getPlain() != null) {
            Optional<RewriteResult<A, ?>> result = rule.rewrite(this)
               .flatMap(r -> r.view().rewrite(fRule).map(view -> RewriteResult.create((View<A, ?>)view, r.recData())));
            REWRITE_CACHE.put(key, result);
            pending.complete(result);
            PENDING_REWRITE_CACHE.remove(key);
            return result;
         } else {
            return (Optional<RewriteResult<A, ?>>)pending.join();
         }
      }
   }

   public <FT, FR> Type<?> getSetType(OpticFinder<FT> optic, Type<FR> newType) {
      return optic.findType(this, newType, false).orThrow().tType();
   }

   public Optional<Type<?>> findFieldTypeOpt(String name) {
      return Optional.empty();
   }

   public Type<?> findFieldType(String name) {
      return this.findFieldTypeOpt(name).orElseThrow(() -> new IllegalArgumentException("Field not found: " + name));
   }

   public OpticFinder<?> findField(String name) {
      return new FieldFinder<>(name, this.findFieldType(name));
   }

   public Optional<A> point(DynamicOps<?> ops) {
      return Optional.empty();
   }

   public Optional<Typed<A>> pointTyped(DynamicOps<?> ops) {
      return this.point(ops).map(value -> new Typed<>(this, ops, (A)value));
   }

   public <FT, FR> Either<TypedOptic<A, ?, FT, FR>, Type.FieldNotFoundException> findTypeCached(
      Type<FT> type, Type<FR> resultType, Type.TypeMatcher<FT, FR> matcher, boolean recurse
   ) {
      return this.findType(type, resultType, matcher, recurse);
   }

   public <FT, FR> Either<TypedOptic<A, ?, FT, FR>, Type.FieldNotFoundException> findType(
      Type<FT> type, Type<FR> resultType, Type.TypeMatcher<FT, FR> matcher, boolean recurse
   ) {
      return matcher.match(this)
         .map(Either::left, r -> r instanceof Type.Continue ? this.findTypeInChildren(type, resultType, matcher, recurse) : Either.right(r));
   }

   public <FT, FR> Either<TypedOptic<A, ?, FT, FR>, Type.FieldNotFoundException> findTypeInChildren(
      Type<FT> type, Type<FR> resultType, Type.TypeMatcher<FT, FR> matcher, boolean recurse
   ) {
      return Either.right(new Type.FieldNotFoundException("No more children"));
   }

   public OpticFinder<A> finder() {
      return DSL.typeFinder(this);
   }

   public <B> Optional<A> ifSame(Typed<B> value) {
      return this.ifSame(value.getType(), value.getValue());
   }

   public <B> Optional<A> ifSame(Type<B> type, B value) {
      return this.equals(type, true, true) ? Optional.of((A)value) : Optional.empty();
   }

   public <B> Optional<RewriteResult<A, ?>> ifSame(Type<B> type, RewriteResult<B, ?> value) {
      return this.equals(type, true, true) ? Optional.of((RewriteResult<A, ?>)value) : Optional.empty();
   }

   @Override
   public final boolean equals(Object o) {
      return this == o ? true : this.equals(o, false, true);
   }

   public abstract boolean equals(Object var1, boolean var2, boolean var3);

   public static final class Continue extends Type.FieldNotFoundException {
      public Continue() {
         super("Continue");
      }
   }

   public static class FieldNotFoundException extends Type.TypeError {
      public FieldNotFoundException(String message) {
         super(message);
      }
   }

   public static class Mu implements K1 {
   }

   private record RewriteCacheKey(Type<?> type, TypeRewriteRule rule, PointFreeRule optimizationRule) {
   }

   public abstract static class TypeError {
      private final String message;

      public TypeError(String message) {
         this.message = message;
      }

      @Override
      public String toString() {
         return this.message;
      }
   }

   public interface TypeMatcher<FT, FR> {
      <S> Either<TypedOptic<S, ?, FT, FR>, Type.FieldNotFoundException> match(Type<S> var1);
   }
}
