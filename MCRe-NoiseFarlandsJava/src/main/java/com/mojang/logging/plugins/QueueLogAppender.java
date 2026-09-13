package com.mojang.logging.plugins;

import com.mojang.logging.LogQueues;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

@Plugin(name = "Queue", category = "Core", elementType = "appender", printObject = true)
public class QueueLogAppender extends AbstractAppender {
    private static final int MAX_CAPACITY = 250;

    private final BlockingQueue<String> queue;

    public QueueLogAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout, final boolean ignoreExceptions, final BlockingQueue<String> queue) {
        super(name, filter, layout, ignoreExceptions);
        this.queue = queue;
    }

    @Override
    public void append(final LogEvent event) {
        if (queue.size() >= MAX_CAPACITY) {
            queue.clear();
        }
        queue.add(getLayout().toSerializable(event).toString());
    }

    @PluginFactory
    public static QueueLogAppender createAppender(@PluginAttribute("name") final String name, @PluginAttribute("ignoreExceptions") final String ignore, @PluginElement("Layout") Layout<? extends Serializable> layout, @PluginElement("Filters") final Filter filter, @PluginAttribute("target") final String target) {
        final boolean ignoreExceptions = Boolean.parseBoolean(ignore);

        if (name == null) {
            LOGGER.error("No name provided for QueueLogAppender");
            return null;
        }

        final BlockingQueue<String> queue = LogQueues.getOrCreateQueue(Objects.requireNonNullElse(target, name));

        if (layout == null) {
            layout = PatternLayout.newBuilder().build();
        }

        return new QueueLogAppender(name, filter, layout, ignoreExceptions, queue);
    }
}
