package com.mojang.serialization.codecs;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;

public record XorCodec<F, S>(Codec<F> first, Codec<S> second) implements Codec<Either<F, S>> {
   @Override
   public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> ops, T input) {
      DataResult<Pair<Either<F, S>, T>> firstRead = this.first.decode(ops, input).map(vo -> vo.mapFirst(Either::left));
      DataResult<Pair<Either<F, S>, T>> secondRead = this.second.decode(ops, input).map(vo -> vo.mapFirst(Either::right));
      Optional<Pair<Either<F, S>, T>> firstResult = firstRead.result();
      Optional<Pair<Either<F, S>, T>> secondResult = secondRead.result();
      if (firstResult.isPresent() && secondResult.isPresent()) {
         return DataResult.error(
            () -> "Both alternatives read successfully, can not pick the correct one; first: " + firstResult.get() + " second: " + secondResult.get(),
            firstResult.get()
         );
      } else if (firstResult.isPresent()) {
         return firstRead;
      } else {
         return secondResult.isPresent() ? secondRead : firstRead.apply2((f, s) -> s, secondRead);
      }
   }

   public <T> DataResult<T> encode(Either<F, S> input, DynamicOps<T> ops, T prefix) {
      return input.map(value1 -> this.first.encode((F)value1, ops, prefix), value2 -> this.second.encode((S)value2, ops, prefix));
   }
}
