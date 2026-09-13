package com.mojang.serialization;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Function3;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public sealed interface DataResult<R> extends App<DataResult.Mu, R> permits DataResult.Success, DataResult.Error {
   static <R> DataResult<R> unbox(App<DataResult.Mu, R> box) {
      return (DataResult<R>)box;
   }

   static <R> DataResult<R> success(R result) {
      return success(result, Lifecycle.experimental());
   }

   static <R> DataResult<R> error(Supplier<String> message, R partialResult) {
      return error(message, partialResult, Lifecycle.experimental());
   }

   static <R> DataResult<R> error(Supplier<String> message) {
      return error(message, Lifecycle.experimental());
   }

   static <R> DataResult<R> success(R result, Lifecycle lifecycle) {
      return new DataResult.Success<>(result, lifecycle);
   }

   static <R> DataResult<R> error(Supplier<String> message, R partialResult, Lifecycle lifecycle) {
      return new DataResult.Error<>(message, Optional.of(partialResult), lifecycle);
   }

   static <R> DataResult<R> error(Supplier<String> message, Lifecycle lifecycle) {
      return new DataResult.Error<>(message, Optional.empty(), lifecycle);
   }

   static <K, V> Function<K, DataResult<V>> partialGet(Function<K, V> partialGet, Supplier<String> errorPrefix) {
      return name -> Optional.ofNullable(partialGet.apply(name)).map(DataResult::success).orElseGet(() -> error(() -> errorPrefix.get() + name));
   }

   static DataResult.Instance instance() {
      return DataResult.Instance.INSTANCE;
   }

   static String appendMessages(String first, String second) {
      return first + "; " + second;
   }

   Optional<R> result();

   Optional<DataResult.Error<R>> error();

   Lifecycle lifecycle();

   boolean hasResultOrPartial();

   Optional<R> resultOrPartial(Consumer<String> var1);

   Optional<R> resultOrPartial();

   <E extends Throwable> R getOrThrow(Function<String, E> var1) throws E;

   <E extends Throwable> R getPartialOrThrow(Function<String, E> var1) throws E;

   default R getOrThrow() {
      return this.getOrThrow(IllegalStateException::new);
   }

   default R getPartialOrThrow() {
      return this.getPartialOrThrow(IllegalStateException::new);
   }

   <T> DataResult<T> map(Function<? super R, ? extends T> var1);

   <T> T mapOrElse(Function<? super R, ? extends T> var1, Function<? super DataResult.Error<R>, ? extends T> var2);

   DataResult<R> ifSuccess(Consumer<? super R> var1);

   DataResult<R> ifError(Consumer<? super DataResult.Error<R>> var1);

   DataResult<R> promotePartial(Consumer<String> var1);

   <R2> DataResult<R2> flatMap(Function<? super R, ? extends DataResult<R2>> var1);

   <R2> DataResult<R2> ap(DataResult<Function<R, R2>> var1);

   default <R2, S> DataResult<S> apply2(BiFunction<R, R2, S> function, DataResult<R2> second) {
      return unbox(instance().apply2(function, this, second));
   }

   default <R2, S> DataResult<S> apply2stable(BiFunction<R, R2, S> function, DataResult<R2> second) {
      Applicative<DataResult.Mu, DataResult.Instance.Mu> instance = instance();
      DataResult<BiFunction<R, R2, S>> f = unbox(instance.point(function)).setLifecycle(Lifecycle.stable());
      return unbox(instance.ap2(f, this, second));
   }

   default <R2, R3, S> DataResult<S> apply3(Function3<R, R2, R3, S> function, DataResult<R2> second, DataResult<R3> third) {
      return unbox(instance().apply3(function, this, second, third));
   }

   DataResult<R> setPartial(Supplier<R> var1);

   DataResult<R> setPartial(R var1);

   DataResult<R> mapError(UnaryOperator<String> var1);

   DataResult<R> setLifecycle(Lifecycle var1);

   default DataResult<R> addLifecycle(Lifecycle lifecycle) {
      return this.setLifecycle(this.lifecycle().add(lifecycle));
   }

   boolean isSuccess();

   default boolean isError() {
      return !this.isSuccess();
   }

   record Error<R>(Supplier<String> messageSupplier, Optional<R> partialValue, Lifecycle lifecycle) implements DataResult<R> {
      public String message() {
         return this.messageSupplier.get();
      }

      @Override
      public Optional<R> result() {
         return Optional.empty();
      }

      @Override
      public Optional<DataResult.Error<R>> error() {
         return Optional.of(this);
      }

      @Override
      public boolean hasResultOrPartial() {
         return this.partialValue.isPresent();
      }

      @Override
      public Optional<R> resultOrPartial(Consumer<String> onError) {
         onError.accept(this.messageSupplier.get());
         return this.partialValue;
      }

      @Override
      public Optional<R> resultOrPartial() {
         return this.partialValue;
      }

      @Override
      public <E extends Throwable> R getOrThrow(Function<String, E> exceptionSupplier) throws E {
         throw exceptionSupplier.apply(this.message());
      }

      @Override
      public <E extends Throwable> R getPartialOrThrow(Function<String, E> exceptionSupplier) throws E {
         if (this.partialValue.isPresent()) {
            return this.partialValue.get();
         } else {
            throw exceptionSupplier.apply(this.message());
         }
      }

      public <T> DataResult.Error<T> map(Function<? super R, ? extends T> function) {
         return (DataResult.Error<T>)(this.partialValue.isEmpty()
            ? this
            : new DataResult.Error<>(this.messageSupplier, this.partialValue.map(function), this.lifecycle));
      }

      @Override
      public <T> T mapOrElse(Function<? super R, ? extends T> successFunction, Function<? super DataResult.Error<R>, ? extends T> errorFunction) {
         return (T)errorFunction.apply(this);
      }

      @Override
      public DataResult<R> ifSuccess(Consumer<? super R> ifSuccess) {
         return this;
      }

      @Override
      public DataResult<R> ifError(Consumer<? super DataResult.Error<R>> ifError) {
         ifError.accept(this);
         return this;
      }

      @Override
      public DataResult<R> promotePartial(Consumer<String> onError) {
         onError.accept(this.messageSupplier.get());
         return this.partialValue.<DataResult>map(value -> new DataResult.Success((R)value, this.lifecycle)).orElse(this);
      }

      public <R2> DataResult.Error<R2> flatMap(Function<? super R, ? extends DataResult<R2>> function) {
         if (this.partialValue.isEmpty()) {
            return (DataResult.Error<R2>)this;
         } else {
            DataResult<R2> second = (DataResult<R2>)function.apply(this.partialValue.get());
            Lifecycle combinedLifecycle = this.lifecycle.add(second.lifecycle());
            if (second instanceof DataResult.Success<R2> secondSuccess) {
               return new DataResult.Error<>(this.messageSupplier, Optional.of(secondSuccess.value), combinedLifecycle);
            } else if (second instanceof DataResult.Error<R2> secondError) {
               return new DataResult.Error<>(
                  () -> DataResult.appendMessages(this.messageSupplier.get(), secondError.messageSupplier.get()), secondError.partialValue, combinedLifecycle
               );
            } else {
               throw new UnsupportedOperationException();
            }
         }
      }

      public <R2> DataResult.Error<R2> ap(DataResult<Function<R, R2>> functionResult) {
         Lifecycle combinedLifecycle = this.lifecycle.add(functionResult.lifecycle());
         if (functionResult instanceof DataResult.Success<Function<R, R2>> func) {
            return new DataResult.Error<>(this.messageSupplier, this.partialValue.map(func.value), combinedLifecycle);
         } else if (functionResult instanceof DataResult.Error<Function<R, R2>> funcError) {
            return new DataResult.Error(
               () -> DataResult.appendMessages(this.messageSupplier.get(), funcError.messageSupplier.get()),
               this.partialValue.flatMap(a -> funcError.partialValue.map(f -> (R)f.apply((R)a))),
               combinedLifecycle
            );
         } else {
            throw new UnsupportedOperationException();
         }
      }

      public DataResult.Error<R> setPartial(Supplier<R> partial) {
         return this.setPartial(partial.get());
      }

      public DataResult.Error<R> setPartial(R partial) {
         return new DataResult.Error<>(this.messageSupplier, Optional.of(partial), this.lifecycle);
      }

      public DataResult.Error<R> mapError(UnaryOperator<String> function) {
         return new DataResult.Error<>(() -> function.apply(this.messageSupplier.get()), this.partialValue, this.lifecycle);
      }

      public DataResult.Error<R> setLifecycle(Lifecycle lifecycle) {
         return this.lifecycle.equals(lifecycle) ? this : new DataResult.Error<>(this.messageSupplier, this.partialValue, lifecycle);
      }

      @Override
      public boolean isSuccess() {
         return false;
      }

      @Override
      public String toString() {
         return "DataResult.Error['" + this.message() + "'" + this.partialValue.<String>map(value -> ": " + value).orElse("") + "]";
      }
   }

   enum Instance implements Applicative<DataResult.Mu, DataResult.Instance.Mu> {
      INSTANCE;

      @Override
      public <T, R> App<DataResult.Mu, R> map(Function<? super T, ? extends R> func, App<DataResult.Mu, T> ts) {
         return DataResult.unbox(ts).map(func);
      }

      @Override
      public <A> App<DataResult.Mu, A> point(A a) {
         return DataResult.success(a);
      }

      @Override
      public <A, R> Function<App<DataResult.Mu, A>, App<DataResult.Mu, R>> lift1(App<DataResult.Mu, Function<A, R>> function) {
         return fa -> this.ap(function, fa);
      }

      @Override
      public <A, R> App<DataResult.Mu, R> ap(App<DataResult.Mu, Function<A, R>> func, App<DataResult.Mu, A> arg) {
         return DataResult.unbox(arg).ap(DataResult.unbox(func));
      }

      @Override
      public <A, B, R> App<DataResult.Mu, R> ap2(App<DataResult.Mu, BiFunction<A, B, R>> func, App<DataResult.Mu, A> a, App<DataResult.Mu, B> b) {
         DataResult<BiFunction<A, B, R>> fr = DataResult.unbox(func);
         DataResult<A> ra = DataResult.unbox(a);
         DataResult<B> rb = DataResult.unbox(b);
         return fr.result().isPresent() && ra.result().isPresent() && rb.result().isPresent()
            ? new DataResult.Success<>(fr.result().get().apply(ra.result().get(), rb.result().get()), fr.lifecycle().add(ra.lifecycle()).add(rb.lifecycle()))
            : Applicative.super.ap2(func, a, b);
      }

      @Override
      public <T1, T2, T3, R> App<DataResult.Mu, R> ap3(
         App<DataResult.Mu, Function3<T1, T2, T3, R>> func, App<DataResult.Mu, T1> t1, App<DataResult.Mu, T2> t2, App<DataResult.Mu, T3> t3
      ) {
         DataResult<Function3<T1, T2, T3, R>> fr = DataResult.unbox(func);
         DataResult<T1> dr1 = DataResult.unbox(t1);
         DataResult<T2> dr2 = DataResult.unbox(t2);
         DataResult<T3> dr3 = DataResult.unbox(t3);
         return fr.result().isPresent() && dr1.result().isPresent() && dr2.result().isPresent() && dr3.result().isPresent()
            ? new DataResult.Success<>(
               fr.result().get().apply(dr1.result().get(), dr2.result().get(), dr3.result().get()),
               fr.lifecycle().add(dr1.lifecycle()).add(dr2.lifecycle()).add(dr3.lifecycle())
            )
            : Applicative.super.ap3(func, t1, t2, t3);
      }

      public static final class Mu implements Applicative.Mu {
      }
   }

   final class Mu implements K1 {
   }

   record Success<R>(R value, Lifecycle lifecycle) implements DataResult<R> {
      @Override
      public Optional<R> result() {
         return Optional.of(this.value);
      }

      @Override
      public Optional<DataResult.Error<R>> error() {
         return Optional.empty();
      }

      @Override
      public boolean hasResultOrPartial() {
         return true;
      }

      @Override
      public Optional<R> resultOrPartial(Consumer<String> onError) {
         return Optional.of(this.value);
      }

      @Override
      public Optional<R> resultOrPartial() {
         return Optional.of(this.value);
      }

      @Override
      public <E extends Throwable> R getOrThrow(Function<String, E> exceptionSupplier) throws E {
         return this.value;
      }

      @Override
      public <E extends Throwable> R getPartialOrThrow(Function<String, E> exceptionSupplier) throws E {
         return this.value;
      }

      @Override
      public <T> DataResult<T> map(Function<? super R, ? extends T> function) {
         return (DataResult<T>)(new DataResult.Success<>(function.apply(this.value), this.lifecycle));
      }

      @Override
      public <T> T mapOrElse(Function<? super R, ? extends T> successFunction, Function<? super DataResult.Error<R>, ? extends T> errorFunction) {
         return (T)successFunction.apply(this.value);
      }

      @Override
      public DataResult<R> ifSuccess(Consumer<? super R> ifSuccess) {
         ifSuccess.accept(this.value);
         return this;
      }

      @Override
      public DataResult<R> ifError(Consumer<? super DataResult.Error<R>> ifError) {
         return this;
      }

      @Override
      public DataResult<R> promotePartial(Consumer<String> onError) {
         return this;
      }

      @Override
      public <R2> DataResult<R2> flatMap(Function<? super R, ? extends DataResult<R2>> function) {
         return function.apply(this.value).addLifecycle(this.lifecycle);
      }

      @Override
      public <R2> DataResult<R2> ap(DataResult<Function<R, R2>> functionResult) {
         Lifecycle combinedLifecycle = this.lifecycle.add(functionResult.lifecycle());
         if (functionResult instanceof DataResult.Success<Function<R, R2>> funcSuccess) {
            return new DataResult.Success<>(funcSuccess.value.apply(this.value), combinedLifecycle);
         } else if (functionResult instanceof DataResult.Error<Function<R, R2>> funcError) {
            return new DataResult.Error(funcError.messageSupplier, funcError.partialValue.map(f -> (R)f.apply(this.value)), combinedLifecycle);
         } else {
            throw new UnsupportedOperationException();
         }
      }

      @Override
      public DataResult<R> setPartial(Supplier<R> partial) {
         return this;
      }

      @Override
      public DataResult<R> setPartial(R partial) {
         return this;
      }

      @Override
      public DataResult<R> mapError(UnaryOperator<String> function) {
         return this;
      }

      @Override
      public DataResult<R> setLifecycle(Lifecycle lifecycle) {
         return this.lifecycle.equals(lifecycle) ? this : new DataResult.Success<>(this.value, lifecycle);
      }

      @Override
      public boolean isSuccess() {
         return true;
      }

      @Override
      public String toString() {
         return "DataResult.Success[" + this.value + "]";
      }
   }
}
