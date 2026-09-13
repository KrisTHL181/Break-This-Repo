package net.minecraft.client.telemetry.events;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GameLoadTimesEvent {
   public static final GameLoadTimesEvent INSTANCE = new GameLoadTimesEvent(Ticker.systemTicker());
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Ticker timeSource;
   private final Map<TelemetryProperty<GameLoadTimesEvent.Measurement>, Stopwatch> measurements = new HashMap<>();
   private OptionalLong bootstrapTime = OptionalLong.empty();

   protected GameLoadTimesEvent(Ticker p_286506_) {
      this.timeSource = p_286506_;
   }

   public synchronized void beginStep(TelemetryProperty<GameLoadTimesEvent.Measurement> p_286394_) {
      this.beginStep(p_286394_, p_286494_ -> Stopwatch.createStarted(this.timeSource));
   }

   public synchronized void beginStep(TelemetryProperty<GameLoadTimesEvent.Measurement> p_286396_, Stopwatch p_286822_) {
      this.beginStep(p_286396_, p_286421_ -> p_286822_);
   }

   private synchronized void beginStep(
      TelemetryProperty<GameLoadTimesEvent.Measurement> p_286311_, Function<TelemetryProperty<GameLoadTimesEvent.Measurement>, Stopwatch> p_286454_
   ) {
      this.measurements.computeIfAbsent(p_286311_, p_286454_);
   }

   public synchronized void endStep(TelemetryProperty<GameLoadTimesEvent.Measurement> p_286634_) {
      Stopwatch stopwatch = this.measurements.get(p_286634_);
      if (stopwatch == null) {
         LOGGER.warn("Attempted to end step for {} before starting it", p_286634_.id());
      } else if (stopwatch.isRunning()) {
         stopwatch.stop();
      }
   }

   public void send(TelemetryEventSender p_286524_) {
      p_286524_.send(
         TelemetryEventType.GAME_LOAD_TIMES,
         p_286285_ -> {
            synchronized (this) {
               this.measurements
                  .forEach(
                     (p_286804_, p_286275_) -> {
                        if (!p_286275_.isRunning()) {
                           long i = p_286275_.elapsed(TimeUnit.MILLISECONDS);
                           p_286285_.put((TelemetryProperty<GameLoadTimesEvent.Measurement>)p_286804_, new GameLoadTimesEvent.Measurement((int)i));
                        } else {
                           LOGGER.warn(
                              "Measurement {} was discarded since it was still ongoing when the event {} was sent.",
                              p_286804_.id(),
                              TelemetryEventType.GAME_LOAD_TIMES.id()
                           );
                        }
                     }
                  );
               this.bootstrapTime
                  .ifPresent(p_286872_ -> p_286285_.put(TelemetryProperty.LOAD_TIME_BOOTSTRAP_MS, new GameLoadTimesEvent.Measurement((int)p_286872_)));
               this.measurements.clear();
            }
         }
      );
   }

   public synchronized void setBootstrapTime(long p_286847_) {
      this.bootstrapTime = OptionalLong.of(p_286847_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Measurement(int millis) {
      public static final Codec<GameLoadTimesEvent.Measurement> CODEC = Codec.INT.xmap(GameLoadTimesEvent.Measurement::new, p_286736_ -> p_286736_.millis);
   }
}
