package com.mojang.datafixers;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.Monoid;
import com.mojang.datafixers.optics.Forget;
import com.mojang.datafixers.optics.ForgetOpt;
import com.mojang.datafixers.optics.Optics;
import com.mojang.datafixers.optics.ReForgetC;
import com.mojang.datafixers.optics.Traversal;
import com.mojang.datafixers.optics.profunctors.TraversalP;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.RecursivePoint;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Typed<A> {
   protected final Type<A> type;
   protected final DynamicOps<?> ops;
   protected final A value;

   public Typed(Type<A> type, DynamicOps<?> ops, A value) {
      this.type = type;
      this.ops = ops;
      this.value = value;
   }

   @Override
   public String toString() {
      return "Typed[" + this.value + "]";
   }

   public <FT> FT get(OpticFinder<FT> optic) {
      return Forget.unbox(
            optic.findType(this.type, false)
               .orThrow()
               .apply(new TypeToken<Forget.Instance.Mu<FT>>() {}, new Forget.Instance<>(), Optics.forget(Function.identity()))
         )
         .run(this.value);
   }

   public <FT> Typed<FT> getTyped(OpticFinder<FT> optic) {
      TypedOptic<A, ?, FT, FT> o = optic.findType(this.type, false).orThrow();
      return new Typed<>(
         o.aType(),
         this.ops,
         Forget.unbox(o.apply(new TypeToken<Forget.Instance.Mu<FT>>() {}, new Forget.Instance<>(), Optics.forget(Function.identity()))).run(this.value)
      );
   }

   public <FT> Optional<FT> getOptional(OpticFinder<FT> optic) {
      TypedOptic<A, ?, FT, FT> optic1 = optic.findType(this.type, false).orThrow();
      return ForgetOpt.unbox(
            optic1.apply(
               new TypeToken<ForgetOpt.Instance.Mu<FT>>() {}, new ForgetOpt.Instance<>(), (App2<ForgetOpt.Mu<FT>, FT, FT>)Optics.forgetOpt(Optional::of)
            )
         )
         .run(this.value);
   }

   public <FT> FT getOrCreate(OpticFinder<FT> optic) {
      return DataFixUtils.<FT>or(this.getOptional(optic), () -> optic.type().point(this.ops))
         .orElseThrow(() -> new IllegalStateException("Could not create default value for type: " + optic.type()));
   }

   public <FT> FT getOrDefault(OpticFinder<FT> optic, FT def) {
      return ForgetOpt.unbox(
            optic.findType(this.type, false)
               .orThrow()
               .apply(new TypeToken<ForgetOpt.Instance.Mu<FT>>() {}, new ForgetOpt.Instance<>(), (App2<ForgetOpt.Mu<FT>, FT, FT>)Optics.forgetOpt(Optional::of))
         )
         .run(this.value)
         .orElse(def);
   }

   // ===== 修改：移除 Object 强制转换，直接使用 FT =====
   public <FT> Optional<Typed<FT>> getOptionalTyped(OpticFinder<FT> optic) {
      TypedOptic<A, ?, FT, FT> o = optic.findType(this.type, false).orThrow();
      return ForgetOpt.unbox(
            o.apply(
               new TypeToken<ForgetOpt.Instance.Mu<FT>>() {},
               new ForgetOpt.Instance<>(),
               Optics.forgetOpt(Optional::of)
            )
         )
         .run(this.value)
         .map(v -> new Typed<>(o.aType(), this.ops, v));
   }

   public <FT> Typed<FT> getOrCreateTyped(OpticFinder<FT> optic) {
      return DataFixUtils.<Typed<FT>>or(this.getOptionalTyped(optic), () -> optic.type().pointTyped(this.ops))
         .orElseThrow(() -> new IllegalStateException("Could not create default value for type: " + optic.type()));
   }

   public <FT> Typed<?> set(OpticFinder<FT> optic, FT newValue) {
      return this.set(optic, new Typed<>(optic.type(), this.ops, newValue));
   }

   public <FT, FR> Typed<?> set(OpticFinder<FT> optic, Type<FR> newType, FR newValue) {
      return this.set(optic, new Typed<>(newType, this.ops, newValue));
   }

   public <FT, FR> Typed<?> set(OpticFinder<FT> optic, Typed<FR> newValue) {
      TypedOptic<A, ?, FT, FR> field = optic.findType(this.type, newValue.type, false).orThrow();
      return this.setCap(field, newValue);
   }

   private <B, FT, FR> Typed<B> setCap(TypedOptic<A, B, FT, FR> field, Typed<FR> newValue) {
      B b = (B)ReForgetC.unbox(
            field.apply(
               new TypeToken<ReForgetC.Instance.Mu<FR>>() {},
               new ReForgetC.Instance<>(),
               (App2<ReForgetC.Mu<FR>, FT, FR>)Optics.reForgetC("set", Either.left((Function<FR, B>)Function.identity()))
            )
         )
         .run(this.value, newValue.value);
      return new Typed<>(field.tType(), this.ops, b);
   }

   public <FT> Typed<?> updateTyped(OpticFinder<FT> optic, Function<Typed<?>, Typed<?>> updater) {
      return this.updateTyped(optic, optic.type(), updater);
   }

   public <FT, FR> Typed<?> updateTyped(OpticFinder<FT> optic, Type<FR> newType, Function<Typed<?>, Typed<?>> updater) {
      TypedOptic<A, ?, FT, FR> field = optic.findType(this.type, newType, false).orThrow();
      return this.updateCap(field, ft -> {
         Typed<?> newValue = updater.apply(new Typed<>(optic.type(), this.ops, ft));
         return field.bType().ifSame(newValue).orElseThrow(() -> new IllegalArgumentException("Function didn't update to the expected type"));
      });
   }

   public <FT> Typed<?> update(OpticFinder<FT> optic, Function<FT, FT> updater) {
      return this.update(optic, optic.type(), updater);
   }

   public <FT, FR> Typed<?> update(OpticFinder<FT> optic, Type<FR> newType, Function<FT, FR> updater) {
      TypedOptic<A, ?, FT, FR> field = optic.findType(this.type, newType, false).orThrow();
      return this.updateCap(field, updater);
   }

   public <FT> Typed<?> updateRecursiveTyped(OpticFinder<FT> optic, Function<Typed<?>, Typed<?>> updater) {
      return this.updateRecursiveTyped(optic, optic.type(), updater);
   }

   public <FT, FR> Typed<?> updateRecursiveTyped(OpticFinder<FT> optic, Type<FR> newType, Function<Typed<?>, Typed<?>> updater) {
      TypedOptic<A, ?, FT, FR> field = optic.findType(this.type, newType, true).orThrow();
      return this.updateCap(field, ft -> {
         Typed<?> newValue = updater.apply(new Typed<>(optic.type(), this.ops, ft));
         return field.bType().ifSame(newValue).orElseThrow(() -> new IllegalArgumentException("Function didn't update to the expected type"));
      });
   }

   public <FT> Typed<?> updateRecursive(OpticFinder<FT> optic, Function<FT, FT> updater) {
      return this.updateRecursive(optic, optic.type(), updater);
   }

   public <FT, FR> Typed<?> updateRecursive(OpticFinder<FT> optic, Type<FR> newType, Function<FT, FR> updater) {
      TypedOptic<A, ?, FT, FR> field = optic.findType(this.type, newType, true).orThrow();
      return this.updateCap(field, updater);
   }

   private <B, FT, FR> Typed<B> updateCap(TypedOptic<A, B, FT, FR> field, Function<FT, FR> updater) {
      Traversal<A, B, FT, FR> traversal = Optics.toTraversal(field.<TraversalP.Mu>upCast(TraversalP.Mu.TYPE_TOKEN).orElseThrow(IllegalArgumentException::new));
      B b = IdF.get(traversal.<IdF.Mu>wander(IdF.Instance.INSTANCE, ft -> IdF.create(updater.apply(ft))).apply(this.value));
      return new Typed<>(field.tType(), this.ops, b);
   }

   public <FT> List<Typed<FT>> getAllTyped(OpticFinder<FT> optic) {
      TypedOptic<A, ?, FT, ?> field = optic.findType(this.type, optic.type(), false).orThrow();
      return this.getAll(field).stream().map(ft -> new Typed<>(optic.type(), this.ops, (FT)ft)).collect(Collectors.toList());
   }

   public <FT> List<FT> getAll(TypedOptic<A, ?, FT, ?> field) {
      Traversal<A, ?, FT, ?> traversal = Optics.toTraversal(field.<TraversalP.Mu>upCast(TraversalP.Mu.TYPE_TOKEN).orElseThrow(IllegalArgumentException::new));
      return Const.unbox(
         traversal.<Const.Mu<List<FT>>>wander(new Const.Instance<>(Monoid.listMonoid()), ft -> Const.create(ImmutableList.of(ft))).apply(this.value)
      );
   }

   public Typed<A> out() {
      if (!(this.type instanceof RecursivePoint.RecursivePointType)) {
         throw new IllegalArgumentException("Not recursive");
      }

      Type<A> unfold = ((RecursivePoint.RecursivePointType)this.type).unfold();
      return new Typed<>(unfold, this.ops, this.value);
   }

   // ===== 修改：显式指定 Optics.inj1 的类型参数 =====
   public <B> Typed<Either<A, B>> inj1(Type<B> type) {
      return new Typed<>(DSL.or(this.type, type), this.ops, Optics.<A, B>inj1().build(this.value));
   }

   // ===== 修改：显式指定 Optics.inj2 的类型参数 =====
   public <B> Typed<Either<B, A>> inj2(Type<B> type) {
      return new Typed<>(DSL.or(type, this.type), this.ops, Optics.<B, A>inj2().build(this.value));
   }

   public static <A, B> Typed<Pair<A, B>> pair(Typed<A> first, Typed<B> second) {
      return new Typed<>(DSL.and(first.type, second.type), first.ops, Pair.of(first.value, second.value));
   }

   public Type<A> getType() {
      return this.type;
   }

   public DynamicOps<?> getOps() {
      return this.ops;
   }

   public A getValue() {
      return this.value;
   }

   public DataResult<? extends Dynamic<?>> write() {
      return this.type.writeDynamic(this.ops, this.value);
   }
}