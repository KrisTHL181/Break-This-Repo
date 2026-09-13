package com.mojang.datafixers;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class DataFixUtils {
   private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{
      0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9
   };

   private DataFixUtils() {
   }

   public static int smallestEncompassingPowerOfTwo(int input) {
      int result = input - 1;
      result |= result >> 1;
      result |= result >> 2;
      result |= result >> 4;
      result |= result >> 8;
      result |= result >> 16;
      return result + 1;
   }

   private static boolean isPowerOfTwo(int input) {
      return input != 0 && (input & input - 1) == 0;
   }

   public static int ceillog2(int input) {
      input = isPowerOfTwo(input) ? input : smallestEncompassingPowerOfTwo(input);
      return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int)(input * 125613361L >> 27) & 31];
   }

   public static <T> T make(Supplier<T> factory) {
      return factory.get();
   }

   public static <T> T make(T t, Consumer<T> consumer) {
      consumer.accept(t);
      return t;
   }

   public static <U> U orElse(Optional<? extends U> optional, U other) {
      return (U)(optional.isPresent() ? optional.get() : other);
   }

   public static <U> U orElseGet(Optional<? extends U> optional, Supplier<? extends U> other) {
      return (U)(optional.isPresent() ? optional.get() : other.get());
   }

   public static <U> Optional<U> or(Optional<? extends U> optional, Supplier<? extends Optional<? extends U>> other) {
      return optional.isPresent() ? optional.map(u -> (U)u) : other.get().map(u -> (U)u);
   }

   public static byte[] toArray(ByteBuffer input) {
      byte[] bytes;
      if (input.hasArray()) {
         bytes = input.array();
      } else {
         bytes = new byte[input.capacity()];
         input.get(bytes, 0, bytes.length);
      }

      return bytes;
   }

   public static int makeKey(int version) {
      return makeKey(version, 0);
   }

   public static int makeKey(int version, int subVersion) {
      return version * 10 + subVersion;
   }

   public static int getVersion(int key) {
      return key / 10;
   }

   public static int getSubVersion(int key) {
      return key % 10;
   }

   public static <T> UnaryOperator<T> consumerToFunction(Consumer<T> consumer) {
      return s -> {
         consumer.accept((T)s);
         return s;
      };
   }
}
