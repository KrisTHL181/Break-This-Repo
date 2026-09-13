package com.mojang.logging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.slf4j.event.Level;

public class LogListeners {
   private static final Map<String, LogListeners.Target> TARGETS = new ConcurrentHashMap<>();

   public static LogListeners.Target getOrCreateTarget(String target) {
      return TARGETS.computeIfAbsent(target, s -> new LogListeners.Target());
   }

   public static void addListener(String target, LogListeners.Listener listener) {
      getOrCreateTarget(target).addListener(listener);
   }

   public interface Listener {
      void accept(String var1, Level var2);
   }

   public static class Target {
      private volatile List<LogListeners.Listener> listeners = List.of();

      private synchronized void addListener(LogListeners.Listener listener) {
         List<LogListeners.Listener> newListeners = new ArrayList<>(this.listeners.size() + 1);
         newListeners.addAll(this.listeners);
         newListeners.add(listener);
         this.listeners = newListeners;
      }

      public void post(Layout<? extends Serializable> layout, LogEvent event) {
         if (!this.listeners.isEmpty()) {
            String message = layout.toSerializable(event).toString();
            Level level = log4jToSlf4jLevel(event.getLevel());

            for (LogListeners.Listener listener : this.listeners) {
               listener.accept(message, level);
            }
         }
      }

      private static Level log4jToSlf4jLevel(org.apache.logging.log4j.Level level) {
         if (level == org.apache.logging.log4j.Level.ERROR) {
            return Level.ERROR;
         } else if (level == org.apache.logging.log4j.Level.WARN) {
            return Level.WARN;
         } else if (level == org.apache.logging.log4j.Level.INFO) {
            return Level.INFO;
         } else if (level == org.apache.logging.log4j.Level.DEBUG) {
            return Level.DEBUG;
         } else {
            return level == org.apache.logging.log4j.Level.TRACE ? Level.TRACE : Level.INFO;
         }
      }
   }
}
