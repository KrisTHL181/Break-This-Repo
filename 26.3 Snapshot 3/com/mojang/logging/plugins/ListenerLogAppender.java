package com.mojang.logging.plugins;

import com.mojang.logging.LogListeners;
import java.io.Serializable;
import java.util.Objects;
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

@Plugin(name = "Listener", category = "Core", elementType = "appender", printObject = true)
public class ListenerLogAppender extends AbstractAppender {
   private final LogListeners.Target output;

   public ListenerLogAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, LogListeners.Target output) {
      super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
      this.output = output;
   }

   public void append(LogEvent event) {
      this.output.post(this.getLayout(), event);
   }

   @PluginFactory
   public static @Nullable ListenerLogAppender createAppender(
      @PluginAttribute("name") @Nullable String name,
      @PluginAttribute("ignoreExceptions") String ignore,
      @PluginElement("Layout") @Nullable Layout<? extends Serializable> layout,
      @PluginElement("Filters") Filter filter,
      @PluginAttribute("target") @Nullable String target
   ) {
      boolean ignoreExceptions = Boolean.parseBoolean(ignore);
      if (name == null) {
         LOGGER.error("No name provided for ListenerLogAppender");
         return null;
      }

      LogListeners.Target output = LogListeners.getOrCreateTarget(Objects.requireNonNullElse(target, name));
      if (layout == null) {
         layout = PatternLayout.newBuilder().build();
      }

      return new ListenerLogAppender(name, filter, layout, ignoreExceptions, output);
   }
}
