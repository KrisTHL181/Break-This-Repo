package net.minecraft.advancements.predicates;

import com.google.common.collect.Range;
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

   static <V extends Number & Comparable<V>, B extends MinMaxBounds<V>> Function<B, DataResult<B>> validateContainedInRange(final MinMaxBounds<V> allowed) {
      Range<V> allowedRange = allowed.bounds().asRange();
      return target -> {
         Range<V> selfAsRange = target.bounds().asRange();
         return !allowedRange.encloses(selfAsRange)
            ? DataResult.error(() -> "Range must be within " + allowedRange + ", but was " + selfAsRange)
            : DataResult.success(target);
      };
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

      public Range<T> asRange() {
         if (this.min.isPresent()) {
            return this.max.isPresent() ? Range.closed(this.min.get(), this.max.get()) : Range.atLeast(this.min.get());
         } else {
            return this.max.isPresent() ? Range.atMost(this.max.get()) : Range.all();
         }
      }

      public Optional<T> asPoint() {
         Optional<T> min = this.min();
         Optional<T> max = this.max();
         return min.equals(max) ? min : Optional.empty();
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> any() {
         return new MinMaxBounds.Bounds<>(Optional.empty(), Optional.empty());
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> exactly(final T value) {
         Optional<T> wrapped = Optional.of(value);
         return new MinMaxBounds.Bounds<>(wrapped, wrapped);
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> between(final T min, final T max) {
         return new MinMaxBounds.Bounds<>(Optional.of(min), Optional.of(max));
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> atLeast(final T value) {
         return new MinMaxBounds.Bounds<>(Optional.of(value), Optional.empty());
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> atMost(final T value) {
         return new MinMaxBounds.Bounds<>(Optional.empty(), Optional.of(value));
      }

      public <U extends Number & Comparable<U>> MinMaxBounds.Bounds<U> map(final Function<T, U> mapper) {
         return new MinMaxBounds.Bounds<>(this.min.map(mapper), this.max.map(mapper));
      }

      public static <T extends Number & Comparable<T>> Codec<MinMaxBounds.Bounds<T>> createCodec(final Codec<T> numberCodec) {
         Codec<MinMaxBounds.Bounds<T>> rangeCodec = RecordCodecBuilder.create(
            i -> i.group(
                  numberCodec.optionalFieldOf("min").forGetter(MinMaxBounds.Bounds::min),
                  numberCodec.optionalFieldOf("max").forGetter(MinMaxBounds.Bounds::max)
               )
               .apply(i, MinMaxBounds.Bounds::new)
         );
         return Codec.either(rangeCodec, numberCodec).xmap(either -> (MinMaxBounds.Bounds)either.map(v -> v, x$0 -> exactly((T)x$0)), bounds -> {
            Optional<T> point = bounds.asPoint();
            return point.isPresent() ? Either.right(point.get()) : Either.left(bounds);
         });
      }

      public static <B extends ByteBuf, T extends Number & Comparable<T>> StreamCodec<B, MinMaxBounds.Bounds<T>> createStreamCodec(
         final StreamCodec<B, T> numberCodec
      ) {
         return new StreamCodec<B, MinMaxBounds.Bounds<T>>() {
            private static final int MIN_FLAG = 1;
            private static final int MAX_FLAG = 2;

            public MinMaxBounds.Bounds<T> decode(final B input) {
               byte flags = input.readByte();
               Optional<T> min = (flags & 1) != 0 ? Optional.of(numberCodec.decode(input)) : Optional.empty();
               Optional<T> max = (flags & 2) != 0 ? Optional.of(numberCodec.decode(input)) : Optional.empty();
               return new MinMaxBounds.Bounds<>(min, max);
            }

            public void encode(final B output, final MinMaxBounds.Bounds<T> value) {
               Optional<T> min = value.min();
               Optional<T> max = value.max();
               output.writeByte((min.isPresent() ? 1 : 0) | (max.isPresent() ? 2 : 0));
               min.ifPresent(v -> numberCodec.encode(output, (T)v));
               max.ifPresent(v -> numberCodec.encode(output, (T)v));
            }
         };
      }

      public static <T extends Number & Comparable<T>> MinMaxBounds.Bounds<T> fromReader(
         final StringReader reader, final Function<String, T> converter, final Supplier<DynamicCommandExceptionType> parseExc
      ) throws CommandSyntaxException {
         if (!reader.canRead()) {
            throw MinMaxBounds.ERROR_EMPTY.createWithContext(reader);
         }

         int start = reader.getCursor();

         try {
            Optional<T> min = readNumber(reader, converter, parseExc);
            Optional<T> max;
            if (reader.canRead(2) && reader.peek() == '.' && reader.peek(1) == '.') {
               reader.skip();
               reader.skip();
               max = readNumber(reader, converter, parseExc);
            } else {
               max = min;
            }

            if (min.isEmpty() && max.isEmpty()) {
               throw MinMaxBounds.ERROR_EMPTY.createWithContext(reader);
            } else {
               return new MinMaxBounds.Bounds<>(min, max);
            }
         } catch (CommandSyntaxException e) {
            reader.setCursor(start);
            throw new CommandSyntaxException(e.getType(), e.getRawMessage(), e.getInput(), start);
         }
      }

      private static <T extends Number> Optional<T> readNumber(
         final StringReader reader, final Function<String, T> converter, final Supplier<DynamicCommandExceptionType> parseExc
      ) throws CommandSyntaxException {
         int start = reader.getCursor();

         while (reader.canRead() && isAllowedInputChar(reader)) {
            reader.skip();
         }

         String number = reader.getString().substring(start, reader.getCursor());
         if (number.isEmpty()) {
            return Optional.empty();
         }

         try {
            return Optional.of(converter.apply(number));
         } catch (NumberFormatException ex) {
            throw parseExc.get().createWithContext(reader, number);
         }
      }

      private static boolean isAllowedInputChar(final StringReader reader) {
         char c = reader.peek();
         if ((c < '0' || c > '9') && c != '-') {
            return c != '.' ? false : !reader.canRead(2) || reader.peek(1) != '.';
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

      private Doubles(final MinMaxBounds.Bounds<Double> bounds) {
         this(bounds, bounds.map(Mth::square));
      }

      public static MinMaxBounds.Doubles exactly(final double value) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.exactly(value));
      }

      public static MinMaxBounds.Doubles between(final double min, final double max) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.between(min, max));
      }

      public static MinMaxBounds.Doubles atLeast(final double value) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.atLeast(value));
      }

      public static MinMaxBounds.Doubles atMost(final double value) {
         return new MinMaxBounds.Doubles(MinMaxBounds.Bounds.atMost(value));
      }

      public boolean matches(final double value) {
         return this.bounds.min.isPresent() && this.bounds.min.get() > value ? false : this.bounds.max.isEmpty() || !(this.bounds.max.get() < value);
      }

      public boolean matchesSqr(final double valueSqr) {
         return this.boundsSqr.min.isPresent() && this.boundsSqr.min.get() > valueSqr
            ? false
            : this.boundsSqr.max.isEmpty() || !(this.boundsSqr.max.get() < valueSqr);
      }

      public static MinMaxBounds.Doubles fromReader(final StringReader reader) throws CommandSyntaxException {
         int start = reader.getCursor();
         MinMaxBounds.Bounds<Double> bounds = MinMaxBounds.Bounds.fromReader(
            reader, Double::parseDouble, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidDouble
         );
         if (bounds.areSwapped()) {
            reader.setCursor(start);
            throw ERROR_SWAPPED.createWithContext(reader);
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

      public static MinMaxBounds.FloatDegrees fromReader(final StringReader reader) throws CommandSyntaxException {
         MinMaxBounds.Bounds<Float> bounds = MinMaxBounds.Bounds.fromReader(
            reader, Float::parseFloat, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidFloat
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

      private Ints(final MinMaxBounds.Bounds<Integer> bounds) {
         this(bounds, bounds.map(i -> Mth.square(i.longValue())));
      }

      public static MinMaxBounds.Ints exactly(final int value) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.exactly(value));
      }

      public static MinMaxBounds.Ints between(final int min, final int max) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.between(min, max));
      }

      public static MinMaxBounds.Ints atLeast(final int value) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.atLeast(value));
      }

      public static MinMaxBounds.Ints atMost(final int value) {
         return new MinMaxBounds.Ints(MinMaxBounds.Bounds.atMost(value));
      }

      public boolean matches(final int value) {
         return this.bounds.min.isPresent() && this.bounds.min.get() > value ? false : this.bounds.max.isEmpty() || this.bounds.max.get() >= value;
      }

      public boolean matchesSqr(final long valueSqr) {
         return this.boundsSqr.min.isPresent() && this.boundsSqr.min.get() > valueSqr
            ? false
            : this.boundsSqr.max.isEmpty() || this.boundsSqr.max.get() >= valueSqr;
      }

      public static MinMaxBounds.Ints fromReader(final StringReader reader) throws CommandSyntaxException {
         int start = reader.getCursor();
         MinMaxBounds.Bounds<Integer> bounds = MinMaxBounds.Bounds.fromReader(
            reader, Integer::parseInt, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidInt
         );
         if (bounds.areSwapped()) {
            reader.setCursor(start);
            throw ERROR_SWAPPED.createWithContext(reader);
         } else {
            return new MinMaxBounds.Ints(bounds);
         }
      }
   }
}
