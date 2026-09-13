package com.mojang.serialization.codecs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class OptionalFieldCodec<A> extends MapCodec<Optional<A>> {
   private final String name;
   private final Codec<A> elementCodec;
   private final boolean lenient;

   public OptionalFieldCodec(String name, Codec<A> elementCodec, boolean lenient) {
      this.name = name;
      this.elementCodec = elementCodec;
      this.lenient = lenient;
   }

   @Override
   public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input) {
      T value = input.get(this.name);
      if (value == null) {
         return DataResult.success(Optional.empty());
      }

      DataResult<A> parsed = this.elementCodec.parse(ops, value);
      return parsed.isError() && this.lenient ? DataResult.success(Optional.empty()) : parsed.map(Optional::of).setPartial(parsed.resultOrPartial());
   }

   public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
      return input.isPresent() ? prefix.add(this.name, this.elementCodec.encodeStart(ops, input.get())) : prefix;
   }

   @Override
   public <T> Stream<T> keys(DynamicOps<T> ops) {
      return Stream.of(ops.createString(this.name));
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         OptionalFieldCodec<?> that = (OptionalFieldCodec<?>)o;
         return Objects.equals(this.name, that.name) && Objects.equals(this.elementCodec, that.elementCodec) && this.lenient == that.lenient;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.name, this.elementCodec, this.lenient);
   }

   @Override
   public String toString() {
      return "OptionalFieldCodec[" + this.name + ": " + this.elementCodec + "]";
   }
}
