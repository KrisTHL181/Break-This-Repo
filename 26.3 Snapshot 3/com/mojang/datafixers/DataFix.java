package com.mojang.datafixers;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.BitSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DataFix {
   private static final Logger LOGGER = LoggerFactory.getLogger(DataFix.class);
   private final Schema outputSchema;
   private final boolean changesType;
   @Nullable
   private TypeRewriteRule rule;

   public DataFix(Schema outputSchema, boolean changesType) {
      this.outputSchema = outputSchema;
      this.changesType = changesType;
   }

   protected <A> TypeRewriteRule fixTypeEverywhere(String name, Type<A> type, Function<DynamicOps<?>, Function<A, A>> function) {
      return this.fixTypeEverywhere(name, type, type, function, new BitSet());
   }

   protected <A, B> TypeRewriteRule convertUnchecked(String name, Type<A> type, Type<B> newType) {
      return this.fixTypeEverywhere(name, type, newType, ops -> (Function<A, B>)Function.identity(), new BitSet());
   }

   protected TypeRewriteRule writeAndRead(String name, Type<?> type, Type<?> newType) {
      return this.writeFixAndRead(name, type, newType, Function.identity());
   }

   protected <A, B> TypeRewriteRule writeFixAndRead(String name, Type<A> type, Type<B> newType, Function<Dynamic<?>, Dynamic<?>> fix) {
      AtomicReference<Type<A>> patchedType = new AtomicReference<>();
      RewriteResult<A, B> view = unchecked(name, type, newType, ops -> input -> {
         Optional<? extends Dynamic<?>> written = patchedType.getPlain().writeDynamic(ops, input).resultOrPartial(LOGGER::error);
         if (written.isEmpty()) {
            throw new RuntimeException("Could not write the object in " + name);
         } else {
            Dynamic<?> fixed = fix.apply((Dynamic<?>)written.get());
            Optional<? extends Pair<Typed<B>, ?>> read = newType.readTyped(fixed).resultOrPartial(LOGGER::error);
            if (read.isEmpty()) {
               throw new RuntimeException("Could not read the new object in " + name);
            } else {
               return read.get().getFirst().getValue();
            }
         }
      }, new BitSet());
      TypeRewriteRule rule = this.fixTypeEverywhere(type, view);
      patchedType.setPlain((Type<A>)type.all(rule, true, false).view().newType());
      return rule;
   }

   protected <A, B> TypeRewriteRule fixTypeEverywhere(String name, Type<A> type, Type<B> newType, Function<DynamicOps<?>, Function<A, B>> function) {
      return this.fixTypeEverywhere(name, type, newType, function, new BitSet());
   }

   protected <A, B> TypeRewriteRule fixTypeEverywhere(
      String name, Type<A> type, Type<B> newType, Function<DynamicOps<?>, Function<A, B>> function, BitSet bitSet
   ) {
      return this.fixTypeEverywhere(type, unchecked(name, type, newType, function, bitSet));
   }

   protected <A> TypeRewriteRule fixTypeEverywhereTyped(String name, Type<A> type, Function<Typed<?>, Typed<?>> function) {
      return this.fixTypeEverywhereTyped(name, type, function, new BitSet());
   }

   protected <A> TypeRewriteRule fixTypeEverywhereTyped(String name, Type<A> type, Function<Typed<?>, Typed<?>> function, BitSet bitSet) {
      return this.fixTypeEverywhereTyped(name, type, type, function, bitSet);
   }

   protected <A, B> TypeRewriteRule fixTypeEverywhereTyped(String name, Type<A> type, Type<B> newType, Function<Typed<?>, Typed<?>> function) {
      return this.fixTypeEverywhereTyped(name, type, newType, function, new BitSet());
   }

   protected <A, B> TypeRewriteRule fixTypeEverywhereTyped(String name, Type<A> type, Type<B> newType, Function<Typed<?>, Typed<?>> function, BitSet bitSet) {
      return this.fixTypeEverywhere(type, checked(name, type, newType, function, bitSet));
   }

   private static <A, B> RewriteResult<A, B> unchecked(
      String name, Type<A> type, Type<B> newType, Function<DynamicOps<?>, Function<A, B>> function, BitSet bitSet
   ) {
      return RewriteResult.create(View.create(name, type, newType, new DataFix.NamedFunctionWrapper<>(name, function)), bitSet);
   }

   public static <A, B> RewriteResult<A, B> checked(String name, Type<A> type, Type<B> newType, Function<Typed<?>, Typed<?>> function, BitSet bitSet) {
      return RewriteResult.create(View.create(name, type, newType, new DataFix.NamedFunctionWrapper<>(name, ops -> a -> {
         Typed<?> result = function.apply(new Typed<>(type, ops, a));
         if (!newType.equals(result.type, true, false)) {
            throw new IllegalStateException(String.format("Dynamic type check failed: %s not equal to %s", newType, result.type));
         } else {
            return (B)result.value;
         }
      })), bitSet);
   }

   protected <A, B> TypeRewriteRule fixTypeEverywhere(Type<A> type, RewriteResult<A, B> view) {
      return TypeRewriteRule.checkOnce(
         TypeRewriteRule.everywhere(TypeRewriteRule.ifSame(type, view), DataFixerUpper.OPTIMIZATION_RULE, true, true), this::onFail
      );
   }

   protected void onFail(Type<?> type) {
      LOGGER.info("Not matched: " + this + " " + type);
   }

   public final int getVersionKey() {
      return this.getOutputSchema().getVersionKey();
   }

   public TypeRewriteRule getRule() {
      if (this.rule == null) {
         this.rule = this.makeRule();
      }

      return this.rule;
   }

   protected abstract TypeRewriteRule makeRule();

   protected Schema getInputSchema() {
      return this.changesType ? this.outputSchema.getParent() : this.getOutputSchema();
   }

   protected Schema getOutputSchema() {
      return this.outputSchema;
   }

   private static final class NamedFunctionWrapper<A, B> implements Function<DynamicOps<?>, Function<A, B>> {
      private final String name;
      private final Function<DynamicOps<?>, Function<A, B>> delegate;

      public NamedFunctionWrapper(String name, Function<DynamicOps<?>, Function<A, B>> delegate) {
         this.name = name;
         this.delegate = delegate;
      }

      public Function<A, B> apply(DynamicOps<?> ops) {
         return this.delegate.apply(ops);
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            DataFix.NamedFunctionWrapper<?, ?> that = (DataFix.NamedFunctionWrapper<?, ?>)o;
            return Objects.equals(this.name, that.name);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return this.name.hashCode();
      }
   }
}
