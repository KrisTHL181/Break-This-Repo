package net.minecraft.client.telemetry;

import java.util.function.Consumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface TelemetryEventSender {
   TelemetryEventSender DISABLED = (p_261883_, p_261730_) -> {};

   default TelemetryEventSender decorate(Consumer<TelemetryPropertyMap.Builder> p_261897_) {
      return (p_261694_, p_261504_) -> this.send(p_261694_, p_261539_ -> {
         p_261504_.accept(p_261539_);
         p_261897_.accept(p_261539_);
      });
   }

   void send(TelemetryEventType var1, Consumer<TelemetryPropertyMap.Builder> var2);
}
