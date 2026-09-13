package com.mojang.datafixers.types;

import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import java.util.function.Function;

public final class Func<A, B> extends Type<Function<A, B>> {
   protected final Type<A> first;
   protected final Type<B> second;

   public Func(Type<A> first, Type<B> second) {
      this.first = first;
      this.second = second;
   }

   @Override
   public TypeTemplate buildTemplate() {
      throw new UnsupportedOperationException("No template for function types.");
   }

   @Override
   protected Codec<Function<A, B>> buildCodec() {
      return Codec.of(Encoder.error("Cannot save a function"), Decoder.error("Cannot read a function"));
   }

   @Override
   public String toString() {
      return "(" + this.first + " -> " + this.second + ")";
   }

   @Override
   public boolean equals(Object obj, boolean ignoreRecursionPoints, boolean checkIndex) {
      return !(obj instanceof Func<?, ?> that)
         ? false
         : this.first.equals(that.first, ignoreRecursionPoints, checkIndex) && this.second.equals(that.second, ignoreRecursionPoints, checkIndex);
   }

   @Override
   public int hashCode() {
      int result = this.first.hashCode();
      return 31 * result + this.second.hashCode();
   }

   public Type<A> first() {
      return this.first;
   }

   public Type<B> second() {
      return this.second;
   }
}
