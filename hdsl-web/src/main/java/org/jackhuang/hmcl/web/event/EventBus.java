/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.web.event;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/// The in-process publish/subscribe bus every Phase 2 event flows through.
///
/// A topic is an arbitrary string; the web layer uses the names of the
/// WebSocket protocol's subscription groups: `instances`, `instance:<id>` and
/// `tasks`. Publishing is a direct, synchronous call on the publishing thread —
/// the payloads are tiny JSON objects and the listeners are the WebSocket
/// gateways' send calls, so a message queue would add latency, not throughput.
///
/// Listeners must not throw: a failing listener is logged and the rest still
/// get the event.
@NotNullByDefault
public final class EventBus {

    /// A subscriber. Called on the publishing thread.
    @FunctionalInterface
    public interface Listener {
        void onEvent(String topic, JsonObject payload);
    }

    /// Detaches a listener registered through [#subscribe].
    @FunctionalInterface
    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    private final Map<String, CopyOnWriteArrayList<Entry>> topics = new ConcurrentHashMap<>();

    /// Registers a listener for one topic.
    ///
    /// @param topic    the topic to hear about
    /// @param listener the listener
    /// @return the subscription; closing it detaches the listener
    public Subscription subscribe(String topic, Listener listener) {
        Entry entry = new Entry(listener);
        topics.computeIfAbsent(topic, ignored -> new CopyOnWriteArrayList<>()).add(entry);
        return () -> {
            var listeners = topics.get(topic);
            if (listeners != null) {
                listeners.remove(entry);
            }
        };
    }

    /// The topic a subscriber asks for when it wants every event.
    public static final String ALL_TOPIC = "*";

    /// Publishes one event to one topic.
    ///
    /// Listeners on `*` receive every event, on top of the topic's own.
    ///
    /// @param topic   the topic
    /// @param payload the event payload
    public void publish(String topic, JsonObject payload) {
        var listeners = topics.get(topic);
        if (listeners != null) {
            deliver(listeners, topic, payload);
        }
        if (!ALL_TOPIC.equals(topic)) {
            var all = topics.get(ALL_TOPIC);
            if (all != null) {
                deliver(all, topic, payload);
            }
        }
    }

    private static void deliver(CopyOnWriteArrayList<Entry> listeners, String topic, JsonObject payload) {
        for (Entry entry : listeners) {
            try {
                entry.listener().onEvent(topic, payload);
            } catch (RuntimeException e) {
                org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                        "Event listener failed on topic " + topic, e);
            }
        }
    }

    /// Publishes one event to several topics.
    ///
    /// @param topicList the topics
    /// @param payload   the event payload
    public void publish(List<String> topicList, JsonObject payload) {
        for (String topic : topicList) {
            publish(topic, payload);
        }
    }

    private record Entry(Listener listener) {
    }
}
