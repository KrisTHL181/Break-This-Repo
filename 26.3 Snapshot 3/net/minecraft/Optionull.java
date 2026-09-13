package net.minecraft;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class Optionull {
   @Deprecated
   public static <T> T orElse(final @Nullable T t, final T defaultValue) {
      return Objects.requireNonNullElse(t, defaultValue);
   }

   public static <T, R> @Nullable R map(final @Nullable T t, final Function<T, R> map) {
      return t == null ? null : map.apply(t);
   }

   public static <T, R> R mapOrDefault(final @Nullable T t, final Function<T, R> map, final R defaultValue) {
      return t == null ? defaultValue : map.apply(t);
   }

   public static <T, R> R mapOrElse(final @Nullable T t, final Function<T, R> map, final Supplier<R> elseSupplier) {
      return t == null ? elseSupplier.get() : map.apply(t);
   }

   public static <T> @Nullable T first(final Collection<T> collection) {
      Iterator<T> iterator = collection.iterator();
      return iterator.hasNext() ? iterator.next() : null;
   }

   public static <T> T firstOrDefault(final Collection<T> collection, final T defaultValue) {
      Iterator<T> iterator = collection.iterator();
      return iterator.hasNext() ? iterator.next() : defaultValue;
   }

   public static <T> T firstOrElse(final Collection<T> collection, final Supplier<T> elseSupplier) {
      Iterator<T> iterator = collection.iterator();
      return iterator.hasNext() ? iterator.next() : elseSupplier.get();
   }

   public static <T> boolean isNullOrEmpty(final T @Nullable [] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(final boolean @Nullable [] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(final byte @Nullable [] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(final char @Nullable [] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(final short @Nullable [] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(final int @Nullable [] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(final long @Nullable [] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(final float @Nullable [] t) {
      return t == null || t.length == 0;
   }

   public static boolean isNullOrEmpty(final double @Nullable [] t) {
      return t == null || t.length == 0;
   }
}
