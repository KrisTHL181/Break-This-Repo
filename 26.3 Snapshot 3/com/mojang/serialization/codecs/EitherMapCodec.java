package com.mojang.serialization.codecs;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.Objects;
import java.util.stream.Stream;

public final class EitherMapCodec<F, S> extends MapCodec<Either<F, S>> {
   private final MapCodec<F> first;
   private final MapCodec<S> second;

   public EitherMapCodec(MapCodec<F> first, MapCodec<S> second) {
      this.first = first;
      this.second = second;
   }

   @Override
   public <T> DataResult<Either<F, S>> decode(DynamicOps<T> ops, MapLike<T> input) {
      DataResult<Either<F, S>> firstRead = this.first.decode(ops, input).map(Either::left);
      if (firstRead.isSuccess()) {
         return firstRead;
      }

      DataResult<Either<F, S>> secondRead = this.second.decode(ops, input).map(Either::right);
      return secondRead.isSuccess() ? secondRead : firstRead.apply2((f, s) -> s, secondRead);
   }

   public <T> RecordBuilder<T> encode(Either<F, S> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
      return input.map(value1 -> this.first.encode((F)value1, ops, prefix), value2 -> this.second.encode((S)value2, ops, prefix));
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         EitherMapCodec<?, ?> eitherCodec = (EitherMapCodec<?, ?>)o;
         return Objects.equals(this.first, eitherCodec.first) && Objects.equals(this.second, eitherCodec.second);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.first, this.second);
   }

   @Override
   public String toString() {
      return "EitherMapCodec[" + this.first + ", " + this.second + "]";
   }

   @Override
   public <T> Stream<T> keys(DynamicOps<T> ops) {
      return Stream.concat(this.first.keys(ops), this.second.keys(ops));
   }
}
