package com.mojang.logging.plugins;

import com.mojang.logging.LogQueues;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name = "Queue", category = "Core", elementType = "appender", printObject = true)
public class QueueLogAppender extends AbstractAppender {
   private static final int MAX_CAPACITY = 250;
   private final BlockingQueue<String> queue;

   public QueueLogAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, BlockingQueue<String> queue) {
      super(name, filter, layout, ignoreExceptions);
      this.queue = queue;
   }

   public void append(LogEvent event) {
      if (this.queue.size() >= 250) {
         this.queue.clear();
      }

      this.queue.add(this.getLayout().toSerializable(event).toString());
   }

   @PluginFactory
   public static QueueLogAppender createAppender(
      @PluginAttribute("name") String name,
      @PluginAttribute("ignoreExceptions") String ignore,
      @PluginElement("Layout") Layout<? extends Serializable> layout,
      @PluginElement("Filters") Filter filter,
      @PluginAttribute("target") String target
   ) {
      boolean ignoreExceptions = Boolean.parseBoolean(ignore);
      if (name == null) {
         LOGGER.error("No name provided for QueueLogAppender");
         return null;
      }

      BlockingQueue<String> queue = LogQueues.getOrCreateQueue(Objects.requireNonNullElse(target, name));
      if (layout == null) {
         layout = PatternLayout.newBuilder().build();
      }

      return new QueueLogAppender(name, filter, layout, ignoreExceptions, queue);
   }
}
