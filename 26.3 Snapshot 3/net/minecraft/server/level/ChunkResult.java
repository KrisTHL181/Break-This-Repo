package net.minecraft.server.level;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public interface ChunkResult<T> {
   static <T> ChunkResult<T> of(final T value) {
      return new ChunkResult.Success<>(value);
   }

   static <T> ChunkResult<T> error(final String error) {
      return error(() -> error);
   }

   static <T> ChunkResult<T> error(final Supplier<String> errorSupplier) {
      return new ChunkResult.Fail<>(errorSupplier);
   }

   boolean isSuccess();

   @Nullable T orElse(@Nullable T orElse);

   static <R> @Nullable R orElse(final ChunkResult<? extends R> chunkResult, final @Nullable R orElse) {
      R result = (R)chunkResult.orElse(null);
      return result != null ? result : orElse;
   }

   @Nullable String getError();

   ChunkResult<T> ifSuccess(Consumer<T> consumer);

   <R> ChunkResult<R> map(Function<T, R> map);

   <E extends Throwable> T orElseThrow(Supplier<E> exceptionSupplier) throws E;

   record Fail<T>(Supplier<String> error) implements ChunkResult<T> {
      @Override
      public boolean isSuccess() {
         return false;
      }

      @Override
      public @Nullable T orElse(final @Nullable T orElse) {
         return orElse;
      }

      @Override
      public String getError() {
         return this.error.get();
      }

      @Override
      public ChunkResult<T> ifSuccess(final Consumer<T> consumer) {
         return this;
      }

      @Override
      public <R> ChunkResult<R> map(final Function<T, R> map) {
         return new ChunkResult.Fail(this.error);
      }

      @Override
      public <E extends Throwable> T orElseThrow(final Supplier<E> exceptionSupplier) throws E {
         throw exceptionSupplier.get();
      }
   }

   record Success<T>(T value) implements ChunkResult<T> {
      @Override
      public boolean isSuccess() {
         return true;
      }

      @Override
      public T orElse(final @Nullable T orElse) {
         return this.value;
      }

      @Override
      public @Nullable String getError() {
         return null;
      }

      @Override
      public ChunkResult<T> ifSuccess(final Consumer<T> consumer) {
         consumer.accept(this.value);
         return this;
      }

      @Override
      public <R> ChunkResult<R> map(final Function<T, R> map) {
         return new ChunkResult.Success<>(map.apply(this.value));
      }

      @Override
      public <E extends Throwable> T orElseThrow(final Supplier<E> exceptionSupplier) throws E {
         return this.value;
      }
   }
}
