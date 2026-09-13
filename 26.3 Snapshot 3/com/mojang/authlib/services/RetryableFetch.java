package com.mojang.authlib.services;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryableFetch<T> {
   private static final Logger LOGGER = LoggerFactory.getLogger(RetryableFetch.class);
   public static final int REFRESH_INTERVAL_HOURS = 24;
   public static final int BASE_FAILURE_INTERVAL_MINUTES = 5;
   private static final int MAX_BACKOFF_EXPONENT = 6;

   public static <T> Supplier<T> fetch(
      final Supplier<URL> urlSupplier,
      final ScheduledExecutorService executor,
      final Function<URL, Optional<T>> fetch,
      T defaultValue,
      final int refreshIntervalInHours,
      final int retryIntervalInMinutes
   ) {
      final CompletableFuture<T> ready = new CompletableFuture<>();
      final AtomicReference<T> value = new AtomicReference<>();
      executor.execute(new Runnable() {
         private final AtomicInteger failureCount = new AtomicInteger();

         @Override
         public void run() {
            try {
               URL url = urlSupplier.get();
               if (url != null) {
                  Optional<T> opt = fetch.apply(url);
                  if (opt.isPresent()) {
                     value.set(opt.get());
                     this.failureCount.set(0);
                  }
               }
            } catch (Exception e) {
               RetryableFetch.LOGGER.warn("Failed to connect", e);
            }

            ready.complete(null);
            this.reschedule();
         }

         private void reschedule() {
            if (value.get() == null) {
               int backoffExponent = Math.min(this.failureCount.getAndIncrement(), 6);
               int delayMinutes = retryIntervalInMinutes * (1 << backoffExponent);
               executor.schedule(this, delayMinutes, TimeUnit.MINUTES);
            } else {
               executor.schedule(this, refreshIntervalInHours, TimeUnit.HOURS);
            }
         }
      });
      return () -> {
         ready.join();
         return Objects.requireNonNullElse(value.get(), defaultValue);
      };
   }
}
