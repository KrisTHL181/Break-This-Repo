package net.minecraft.client.telemetry;

import java.time.Duration;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.telemetry.events.PerformanceMetricsEvent;
import net.minecraft.client.telemetry.events.WorldLoadEvent;
import net.minecraft.client.telemetry.events.WorldLoadTimesEvent;
import net.minecraft.client.telemetry.events.WorldUnloadEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class WorldSessionTelemetryManager {
   private final UUID worldSessionId = UUID.randomUUID();
   private final TelemetryEventSender eventSender;
   private final WorldLoadEvent worldLoadEvent;
   private final WorldUnloadEvent worldUnloadEvent = new WorldUnloadEvent();
   private final PerformanceMetricsEvent performanceMetricsEvent;
   private final WorldLoadTimesEvent worldLoadTimesEvent;

   public WorldSessionTelemetryManager(TelemetryEventSender p_286529_, boolean p_286429_, @Nullable Duration p_286727_, @Nullable String p_286633_) {
      this.worldLoadEvent = new WorldLoadEvent(p_286633_);
      this.performanceMetricsEvent = new PerformanceMetricsEvent();
      this.worldLoadTimesEvent = new WorldLoadTimesEvent(p_286429_, p_286727_);
      this.eventSender = p_286529_.decorate(p_261981_ -> {
         this.worldLoadEvent.addProperties(p_261981_);
         p_261981_.put(TelemetryProperty.WORLD_SESSION_ID, this.worldSessionId);
      });
   }

   public void tick() {
      this.performanceMetricsEvent.tick(this.eventSender);
   }

   public void onPlayerInfoReceived(GameType p_261768_, boolean p_261669_) {
      this.worldLoadEvent.setGameMode(p_261768_, p_261669_);
      this.worldUnloadEvent.onPlayerInfoReceived();
      this.worldSessionStart();
   }

   public void onServerBrandReceived(String p_261520_) {
      this.worldLoadEvent.setServerBrand(p_261520_);
      this.worldSessionStart();
   }

   public void setTime(long p_261878_) {
      this.worldUnloadEvent.setTime(p_261878_);
   }

   public void worldSessionStart() {
      if (this.worldLoadEvent.send(this.eventSender)) {
         this.worldLoadTimesEvent.send(this.eventSender);
         this.performanceMetricsEvent.start();
      }
   }

   public void onDisconnect() {
      this.worldLoadEvent.send(this.eventSender);
      this.performanceMetricsEvent.stop();
      this.worldUnloadEvent.send(this.eventSender);
   }

   public void onAdvancementDone(Level p_286825_, AdvancementHolder p_298119_) {
      Identifier identifier = p_298119_.id();
      if (p_298119_.value().sendsTelemetryEvent() && "minecraft".equals(identifier.getNamespace())) {
         long i = p_286825_.getGameTime();
         this.eventSender.send(TelemetryEventType.ADVANCEMENT_MADE, p_448472_ -> {
            p_448472_.put(TelemetryProperty.ADVANCEMENT_ID, identifier.toString());
            p_448472_.put(TelemetryProperty.ADVANCEMENT_GAME_TIME, i);
         });
      }
   }
}
