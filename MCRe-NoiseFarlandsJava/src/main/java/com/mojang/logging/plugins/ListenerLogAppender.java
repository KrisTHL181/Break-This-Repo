package com.mojang.logging.plugins;

import com.mojang.logging.LogListeners;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

@Plugin(name = "Listener", category = "Core", elementType = "appender", printObject = true)
public class ListenerLogAppender extends AbstractAppender {
    private final LogListeners.Target output;

    public ListenerLogAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout, final boolean ignoreExceptions, final LogListeners.Target output) {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
        this.output = output;
    }

    @Override
    public void append(final LogEvent event) {
        output.post(getLayout(), event);
    }

    @PluginFactory
    @Nullable
    public static ListenerLogAppender createAppender(@PluginAttribute("name") @Nullable final String name, @PluginAttribute("ignoreExceptions") final String ignore, @PluginElement("Layout") @Nullable Layout<? extends Serializable> layout, @PluginElement("Filters") final Filter filter, @PluginAttribute("target") @Nullable final String target) {
        final boolean ignoreExceptions = Boolean.parseBoolean(ignore);

        if (name == null) {
            LOGGER.error("No name provided for ListenerLogAppender");
            return null;
        }

        final LogListeners.Target output = LogListeners.getOrCreateTarget(Objects.requireNonNullElse(target, name));

        if (layout == null) {
            layout = PatternLayout.newBuilder().build();
        }

        return new ListenerLogAppender(name, filter, layout, ignoreExceptions, output);
    }
}
