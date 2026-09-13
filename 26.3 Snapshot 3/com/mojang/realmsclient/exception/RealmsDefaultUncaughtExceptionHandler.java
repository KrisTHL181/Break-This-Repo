package com.mojang.realmsclient.exception;

import java.lang.Thread.UncaughtExceptionHandler;
import org.slf4j.Logger;

public class RealmsDefaultUncaughtExceptionHandler implements UncaughtExceptionHandler {
   private final Logger logger;

   public RealmsDefaultUncaughtExceptionHandler(final Logger logger) {
      this.logger = logger;
   }

   @Override
   public void uncaughtException(final Thread t, final Throwable e) {
      this.logger.error("Caught previously unhandled exception", e);
   }
}
