package com.mojang.logging;

import java.lang.StackWalker.Option;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.event.Level;

public class LogUtils {
   public static final String FATAL_MARKER_ID = "FATAL";
   public static final Marker FATAL_MARKER = MarkerFactory.getMarker("FATAL");
   private static final StackWalker STACK_WALKER = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);

   public static boolean isLoggerActive() {
      return LogManager.getContext() instanceof LifeCycle lifeCycle ? !lifeCycle.isStopped() : true;
   }

   public static void configureRootLoggingLevel(Level level) {
      LoggerContext ctx = (LoggerContext)LogManager.getContext(false);
      Configuration config = ctx.getConfiguration();
      LoggerConfig loggerConfig = config.getLoggerConfig("");
      loggerConfig.setLevel(convertLevel(level));
      ctx.updateLoggers();
   }

   private static org.apache.logging.log4j.Level convertLevel(Level level) {
      return switch (level) {
         case INFO -> org.apache.logging.log4j.Level.INFO;
         case WARN -> org.apache.logging.log4j.Level.WARN;
         case DEBUG -> org.apache.logging.log4j.Level.DEBUG;
         case ERROR -> org.apache.logging.log4j.Level.ERROR;
         case TRACE -> org.apache.logging.log4j.Level.TRACE;
         default -> throw new IncompatibleClassChangeError();
      };
   }

   public static Object defer(final Supplier<Object> result) {
      class ToString {
         @Override
         public String toString() {
            return result.get().toString();
         }
      }

      return new ToString();
   }

   public static Logger getLogger() {
      return LoggerFactory.getLogger(STACK_WALKER.getCallerClass());
   }
}
