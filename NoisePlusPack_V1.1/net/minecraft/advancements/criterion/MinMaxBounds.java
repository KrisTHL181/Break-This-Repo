package net.minecraft.advancements.criterion;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public interface MinMaxBounds<T extends Number & Comparable<T>> {
   SimpleCommandExceptionType ERROR_EMPTY = new SimpleCommandExceptionType(Component.translatable("argument.range.empty"));
   SimpleCommandExceptionType ERROR_SWAPPED = new SimpleCommandExceptionType(Component.translatable("argument.range.swapped"));

   MinMaxBounds.Bounds<T> bounds();

   default Optional<T> min() {
      return this.bounds().min;
   }

   default Optional<T> max() {
      return this.bounds().max;
   }

   default boolean isAny() {
      return this.bounds().isAny();
   }

   record Bounds<T extends Number & Comparable<T>>(Optional<T> min, Optional<T> max) {
      public boolean isAny() {
         return this.min().isEmpty() && this.max().isEmpty();
      }

      public DataResult<MinMaxBounds.Bounds<T>> validateSwappedBoundsInCodec() {
         return this.areSwapped()
            ? DataResult.error(() -> "Swapped bounds in range: " + this.min() + " is higher than " + this.max())
            : DataResult.success(this);
      }

      public boolean areSwapped() {
         return this.min.isPresent() && this.max.isPresent() && this.min.get().compareTo(this.max.get()) > 0;
      }

      public Optional<T> asPoint() {
         Optional<T> optional = this.min();
         Optional<T> optional1 = this.max();
         return optional.equals(optional1) ? optional : Optional.empty();
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> any() {
         return new MinMaxBounds.Bounds<>(Optional.empty(), Optional.empty());
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> exactly(T p_458072_) {
         Optional<T> optional = Optional.of(p_458072_);
         return new MinMaxBounds.Bounds<>(optional, optional);
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> between(T p_458790_, T p_456327_) {
         return new MinMaxBounds.Bounds<>(Optional.of(p_458790_), Optional.of(p_456327_));
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> atLeast(T p_459587_) {
         return new MinMaxBounds.Bounds<>(Optional.of(p_459587_), Optional.empty());
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> atMost(T p_450771_) {
         return new MinMaxBounds.Bounds<>(Optional.empty(), Optional.of(p_450771_));
      }

      public <U extends Number & Comparable<U>> MinMaxBounds.Bounds<U> map(Function<T, U> p_455181_) {
         return new MinMaxBounds.Bounds<>(this.min.map(p_455181_), this.max.map(p_455181_));
      }

      static <T extends Number & Comparable<T>> Codec<MinMaxBounds.Bounds<T>> createCodec(Codec<T> p_454145_) {
         Codec<MinMaxBounds.Bounds<T>> codec = RecordCodecBuilder.create(
            p_459924_ -> p_459924_.group(
                  p_454145_.optionalFieldOf("min").forGetter(MinMaxBounds.Bounds::min), p_454145_.optionalFieldOf("max").forGetter(MinMaxBounds.Bounds::max)
               )
               .apply(p_459924_, MinMaxBounds.Bounds::new)
         );
         return Codec.either(codec, p_454145_)
            .xmap(p_452249_ -> (MinMaxBounds.Bounds)p_452249_.map(p_454880_ -> p_454880_, p_456326_ -> exactly((T)p_456326_)), p_451896_ -> {
               Optional<T> optional = p_451896_.asPoint();
               return optional.isPresent() ? Either.right(optional.get()) : Either.left(p_451896_);
            });
      }

      static <B extends ByteBuf, T extends Number & Comparable<T>> StreamCodec<B, MinMaxBounds.Bounds<T>> createStreamCodec(final StreamCodec<B, T> p_452697_) {
         return new StreamCodec<B, MinMaxBounds.Bounds<T>>() {
            private static final int MIN_FLAG = 1;
            private static final int MAX_FLAG = 2;

            public MinMaxBounds.Bounds<T> decode(B p_452127_) {
               byte b0 = p_452127_.readByte();
               Optional<T> optional = (b0 & 1) != 0 ? Optional.of(p_452697_.decode(p_452127_)) : Optional.empty();
               Optional<T> optional1 = (b0 & 2) != 0 ? Optional.of(p_452697_.decode(p_452127_)) : Optional.empty();
               return new MinMaxBounds.Bounds<>(optional, optional1);
            }

            public void encode(B p_453712_, MinMaxBounds.Bounds<T> p_458967_) {
               Optional<T> optional = p_458967_.min();
               Optional<T> optional1 = p_458967_.max();
               p_453712_.writeByte((optional.isPresent() ? 1 : 0) | (optional1.isPresent() ? 2 : 0));
               optional.ifPresent(p_453605_ -> p_452697_.encode(p_453712_, (T)p_453605_));
               optional1.ifPresent(p_457175_ -> p_452697_.encode(p_453712_, (T)p_457175_));
            }
         };
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> fromReader(
         StringReader p_456653_, Function<String, T> p_454401_, Supplier<DynamicCommandExceptionType> p_454037_
      ) throws CommandSyntaxException {
         if (!p_456653_.canRead()) {
            throw MinMaxBounds.ERROR_EMPTY.createWithContext(p_456653_);
         }

         int i = p_456653_.getCursor();

         try {
            Optional<T> optional = readNumber(p_456653_, p_454401_, p_454037_);
            Optional<T> optional1;
            if (p_456653_.canRead(2) && p_456653_.peek() == '.' && p_456653_.peek(1) == '.') {
               p_456653_.skip();
               p_456653_.skip();
               optional1 = readNumber(p_456653_, p_454401_, p_454037_);
            } else {
               optional1 = optional;
            }

            if (optional.isEmpty() && optional1.isEmpty()) {
               throw MinMaxBounds.ERROR_EMPTY.createWithContext(p_456653_);
            } else {
               return new MinMaxBounds.Bounds<>(optional, optional1);
            }
         } catch (CommandSyntaxException commandsyntaxexception) {
            p_456653_.setCursor(i);
            throw new CommandSyntaxException(commandsyntaxexception.getType(), commandsyntaxexception.getRawMessage(), commandsyntaxexception.getInput(), i);
         }
      }

      private static <T extends Number> Optional<T> readNumber(
         StringReader p_451248_, Function<String, T> p_456036_, Supplier<DynamicCommandExceptionType> p_451609_
      ) throws CommandSyntaxException {
         int i = p_451248_.getCursor();

         while (p_451248_.canRead() && isAllowedInputChar(p_451248_)) {
            p_451248_.skip();
         }

         String s = p_451248_.getString().substring(i, p_451248_.getCursor());
         if (s.isEmpty()) {
            return Optional.empty();
         }

         try {
            return Optional.of(p_456036_.apply(s));
         } catch (NumberFormatException numberformatexception) {
            throw p_451609_.get().createWithContext(p_451248_, s);
         }
      }

      private static boolean isAllowedInputChar(StringReader p_456090_) {
         char c0 = p_456090_.peek();
         if ((c0 < '0' || c0 > '9') && c0 != '-') {
            return c0 != '.' ? false : !p_456090_.canRead(2) || p_456090_.peek(1) != '.';
         } else {
            return true;
         }
      }
   }

   record Doubles(MinMaxBounds.Bounds<Double> bounds, MinMaxBounds.Bounds<Double> boundsSqr) implements MinMaxBounds<Double> {
      public static final MinMaxBounds.Doubles ANY = new MinMaxBounds.Doubles(MinMaxBounds.Bounds.any());
      public static final Codec<MinMaxBounds.Doubles> CODEC = MinMaxBounds.Bounds.createCodec(Codec.DOUBLE)
         .validate(MinMaxBounds.Bounds::validateSwappedBoundsInCodec)
         .xmap(MinMaxBounds.Doubles::new, MinMaxBounds.Doubles::bounds);
      public static final StreamCodec<ByteBuf, MinMaxBounds.Doubles> STREAM_CODEC = MinMaxBounds.Bounds.createStreamCodec(ByteBufCodecs.DOUBLE)
         .map(MinMaxBounds.Doubles::new, MinMaxBounds.Doubles::bounds);

      private Doubles(MinMaxBounds.Bounds<Double> p_458937_) {
         this(p_458937_, p_458937_.map(Mth::square));
      }

      public static MinMaxBounds.Doubles exactly(double p_450166_) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.exactly(p_450166_));
      }

      public static MinMaxBounds.Doubles between(double p_458521_, double p_458425_) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.between(p_458521_, p_458425_));
      }

      public static MinMaxBounds.Doubles atLeast(double p_460333_) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.atLeast(p_460333_));
      }

      public static MinMaxBounds.Doubles atMost(double p_459446_) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.atMost(p_459446_));
      }

      public boolean matches(double p_454694_) {
         return this.bounds.min.isPresent() && this.bounds.min.get() > p_454694_ ? false : this.bounds.max.isEmpty() || !(this.bounds.max.get() < p_454694_);
      }

      public boolean matchesSqr(double p_458226_) {
         return this.boundsSqr.min.isPresent() && this.boundsSqr.min.get() > p_458226_
            ? false
            : this.boundsSqr.max.isEmpty() || !(this.boundsSqr.max.get() < p_458226_);
      }

      public static MinMaxBounds.Doubles fromReader(StringReader p_455724_) throws CommandSyntaxException {
         int i = p_455724_.getCursor();
         MinMaxBounds.Bounds<Double> bounds = MinMaxBounds.Bounds.fromReader(
            p_455724_, Double::parseDouble, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidDouble
         );
         if (bounds.areSwapped()) {
            p_455724_.setCursor(i);
            throw ERROR_SWAPPED.createWithContext(p_455724_);
         } else {
            return new MinMaxBounds.Doubles(bounds);
         }
      }
   }

   record FloatDegrees(MinMaxBounds.Bounds<Float> bounds) implements MinMaxBounds<Float> {
      public static final MinMaxBounds.FloatDegrees ANY = new MinMaxBounds.FloatDegrees(MinMaxBounds.Bounds.any());
      public static final Codec<MinMaxBounds.FloatDegrees> CODEC = MinMaxBounds.Bounds.createCodec(Codec.FLOAT)
         .xmap(MinMaxBounds.FloatDegrees::new, MinMaxBounds.FloatDegrees::bounds);
      public static final StreamCodec<ByteBuf, MinMaxBounds.FloatDegrees> STREAM_CODEC = MinMaxBounds.Bounds.createStreamCodec(ByteBufCodecs.FLOAT)
         .map(MinMaxBounds.FloatDegrees::new, MinMaxBounds.FloatDegrees::bounds);

      public static MinMaxBounds.FloatDegrees fromReader(StringReader p_459024_) throws CommandSyntaxException {
         MinMaxBounds.Bounds<Float> bounds = MinMaxBounds.Bounds.fromReader(
            p_459024_, Float::parseFloat, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidFloat
         );
         return new MinMaxBounds.FloatDegrees(bounds);
      }
   }

   record Ints(MinMaxBounds.Bounds<Integer> bounds, MinMaxBounds.Bounds<Long> boundsSqr) implements MinMaxBounds<Integer> {
      public static final MinMaxBounds.Ints ANY = new MinMaxBounds.Ints(MinMaxBounds.Bounds.any());
      public static final Codec<MinMaxBounds.Ints> CODEC = MinMaxBounds.Bounds.createCodec(Codec.INT)
         .validate(MinMaxBounds.Bounds::validateSwappedBoundsInCodec)
         .xmap(MinMaxBounds.Ints::new, MinMaxBounds.Ints::bounds);
      public static final StreamCodec<ByteBuf, MinMaxBounds.Ints> STREAM_CODEC = MinMaxBounds.Bounds.createStreamCodec(ByteBufCodecs.INT)
         .map(MinMaxBounds.Ints::new, MinMaxBounds.Ints::bounds);

      private Ints(MinMaxBounds.Bounds<Integer> p_460512_) {
         this(p_460512_, p_460512_.map(p_460498_ -> Mth.square(p_460498_.longValue())));
      }

      public static MinMaxBounds.Ints exactly(int p_453998_) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.exactly(p_453998_));
      }

      public static MinMaxBounds.Ints between(int p_454802_, int p_455520_) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.between(p_454802_, p_455520_));
      }

      public static MinMaxBounds.Ints atLeast(int p_460618_) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.atLeast(p_460618_));
      }

      public static MinMaxBounds.Ints atMost(int p_451892_) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.atMost(p_451892_));
      }

      public boolean matches(int p_456097_) {
         return this.bounds.min.isPresent() && this.bounds.min.get() > p_456097_ ? false : this.bounds.max.isEmpty() || this.bounds.max.get() >= p_456097_;
      }

      public boolean matchesSqr(long p_454549_) {
         return this.boundsSqr.min.isPresent() && this.boundsSqr.min.get() > p_454549_
            ? false
            : this.boundsSqr.max.isEmpty() || this.boundsSqr.max.get() >= p_454549_;
      }

      public static MinMaxBounds.Ints fromReader(StringReader p_451450_) throws CommandSyntaxException {
         int i = p_451450_.getCursor();
         MinMaxBounds.Bounds<Integer> bounds = MinMaxBounds.Bounds.fromReader(
            p_451450_, Integer::parseInt, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidInt
         );
         if (bounds.areSwapped()) {
            p_451450_.setCursor(i);
            throw ERROR_SWAPPED.createWithContext(p_451450_);
         } else {
            return new MinMaxBounds.Ints(bounds);
         }
      }
   }
}
