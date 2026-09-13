package net.minecraft.client.telemetry;

import java.util.function.Consumer;

@FunctionalInterface
public interface TelemetryEventSender {
   TelemetryEventSender DISABLED = (type, buildFunction) -> {};

   default TelemetryEventSender decorate(final Consumer<TelemetryPropertyMap.Builder> decorator) {
      return (type, buildFunction) -> this.send(type, properties -> {
         buildFunction.accept(properties);
         decorator.accept(properties);
      });
   }

   void send(TelemetryEventType type, Consumer<TelemetryPropertyMap.Builder> buildFunction);
}
