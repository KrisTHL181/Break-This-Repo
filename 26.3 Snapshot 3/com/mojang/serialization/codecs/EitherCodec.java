package com.mojang.serialization.codecs;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public record EitherCodec<F, S>(Codec<F> first, Codec<S> second) implements Codec<Either<F, S>> {
   @Override
   public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> ops, T input) {
      DataResult<Pair<Either<F, S>, T>> firstRead = this.first.decode(ops, input).map(vo -> vo.mapFirst(Either::left));
      if (firstRead.isSuccess()) {
         return firstRead;
      } else {
         DataResult<Pair<Either<F, S>, T>> secondRead = this.second.decode(ops, input).map(vo -> vo.mapFirst(Either::right));
         if (secondRead.isSuccess()) {
            return secondRead;
         } else if (firstRead.hasResultOrPartial()) {
            return firstRead;
         } else {
            return secondRead.hasResultOrPartial()
               ? secondRead
               : DataResult.error(
                  () -> "Failed to parse either. First: "
                     + firstRead.error().orElseThrow().message()
                     + "; Second: "
                     + secondRead.error().orElseThrow().message()
               );
         }
      }
   }

   public <T> DataResult<T> encode(Either<F, S> input, DynamicOps<T> ops, T prefix) {
      return input.map(value1 -> this.first.encode((F)value1, ops, prefix), value2 -> this.second.encode((S)value2, ops, prefix));
   }
}
