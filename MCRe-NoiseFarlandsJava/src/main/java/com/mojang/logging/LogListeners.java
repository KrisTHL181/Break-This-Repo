package com.mojang.logging;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.slf4j.event.Level;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogListeners {
    private static final Map<String, Target> TARGETS = new ConcurrentHashMap<>();

    public static Target getOrCreateTarget(final String target) {
        return TARGETS.computeIfAbsent(target, s -> new Target());
    }

    public static void addListener(final String target, final Listener listener) {
        getOrCreateTarget(target).addListener(listener);
    }

    public static class Target {
        private volatile List<Listener> listeners = List.of();

        private synchronized void addListener(final Listener listener) {
            final List<Listener> newListeners = new ArrayList<>(listeners.size() + 1);
            newListeners.addAll(listeners);
            newListeners.add(listener);
            listeners = newListeners;
        }

        public void post(final Layout<? extends Serializable> layout, final LogEvent event) {
            if (listeners.isEmpty()) {
                return;
            }
            final String message = layout.toSerializable(event).toString();
            final Level level = log4jToSlf4jLevel(event.getLevel());
            for (final Listener listener : listeners) {
                listener.accept(message, level);
            }
        }

        private static Level log4jToSlf4jLevel(final org.apache.logging.log4j.Level level) {
            if (level == org.apache.logging.log4j.Level.ERROR) {
                return Level.ERROR;
            } else if (level == org.apache.logging.log4j.Level.WARN) {
                return Level.WARN;
            } else if (level == org.apache.logging.log4j.Level.INFO) {
                return Level.INFO;
            } else if (level == org.apache.logging.log4j.Level.DEBUG) {
                return Level.DEBUG;
            } else if (level == org.apache.logging.log4j.Level.TRACE) {
                return Level.TRACE;
            }
            return Level.INFO;
        }
    }

    public interface Listener {
        void accept(String message, Level level);
    }
}
