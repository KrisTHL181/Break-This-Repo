package net.minecraft;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class Optionull {
   @Deprecated
   public static <T> T orElse(@Nullable T p_366600_, T p_368186_) {
      return Objects.requireNonNullElse(p_366600_, p_368186_);
   }

   public static <T, R> @Nullable R map(@Nullable T p_270441_, Function<T, R> p_270332_) {
      return p_270441_ == null ? null : p_270332_.apply(p_270441_);
   }

   public static <T, R> R mapOrDefault(@Nullable T p_270215_, Function<T, R> p_270557_, R p_270839_) {
      return p_270215_ == null ? p_270839_ : p_270557_.apply(p_270215_);
   }

   public static <T, R> R mapOrElse(@Nullable T p_270820_, Function<T, R> p_270536_, Supplier<R> p_270756_) {
      return p_270820_ == null ? p_270756_.get() : p_270536_.apply(p_270820_);
   }

   public static <T> @Nullable T first(Collection<T> p_270346_) {
      Iterator<T> iterator = p_270346_.iterator();
      return iterator.hasNext() ? iterator.next() : null;
   }

   public static <T> T firstOrDefault(Collection<T> p_270625_, T p_270960_) {
      Iterator<T> iterator = p_270625_.iterator();
      return iterator.hasNext() ? iterator.next() : p_270960_;
   }

   public static <T> T firstOrElse(Collection<T> p_270529_, Supplier<T> p_270239_) {
      Iterator<T> iterator = p_270529_.iterator();
      return iterator.hasNext() ? iterator.next() : p_270239_.get();
   }

   public static <T> boolean isNullOrEmpty(T @Nullable [] p_270794_) {
      return p_270794_ == null || p_270794_.length == 0;
   }

   public static boolean isNullOrEmpty(boolean @Nullable [] p_270403_) {
      return p_270403_ == null || p_270403_.length == 0;
   }

   public static boolean isNullOrEmpty(byte @Nullable [] p_270775_) {
      return p_270775_ == null || p_270775_.length == 0;
   }

   public static boolean isNullOrEmpty(char @Nullable [] p_270512_) {
      return p_270512_ == null || p_270512_.length == 0;
   }

   public static boolean isNullOrEmpty(short @Nullable [] p_270712_) {
      return p_270712_ == null || p_270712_.length == 0;
   }

   public static boolean isNullOrEmpty(int @Nullable [] p_270127_) {
      return p_270127_ == null || p_270127_.length == 0;
   }

   public static boolean isNullOrEmpty(long @Nullable [] p_270148_) {
      return p_270148_ == null || p_270148_.length == 0;
   }

   public static boolean isNullOrEmpty(float @Nullable [] p_270428_) {
      return p_270428_ == null || p_270428_.length == 0;
   }

   public static boolean isNullOrEmpty(double @Nullable [] p_270373_) {
      return p_270373_ == null || p_270373_.length == 0;
   }
}
