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

    public WorldSessionTelemetryManager(
        final TelemetryEventSender eventSender,
        final boolean newWorld,
        final @Nullable Duration worldLoadDuration,
        final @Nullable String minigameName,
        final UUID sessionId
    ) {
        this.worldLoadEvent = new WorldLoadEvent(minigameName);
        this.performanceMetricsEvent = new PerformanceMetricsEvent();
        this.worldLoadTimesEvent = new WorldLoadTimesEvent(newWorld, worldLoadDuration);
        this.eventSender = eventSender.decorate(properties -> {
            this.worldLoadEvent.addProperties(properties);
            properties.put(TelemetryProperty.WORLD_SESSION_ID, this.worldSessionId);
            properties.put(TelemetryProperty.SERVER_SESSION_ID, sessionId);
        });
    }

    public void tick() {
        this.performanceMetricsEvent.tick(this.eventSender);
    }

    public void onPlayerInfoReceived(final GameType type, final boolean hardcore) {
        this.worldLoadEvent.setGameMode(type, hardcore);
        this.worldUnloadEvent.onPlayerInfoReceived();
        this.worldSessionStart();
    }

    public void onServerBrandReceived(final String serverBrand) {
        this.worldLoadEvent.setServerBrand(serverBrand);
        this.worldSessionStart();
    }

    public void setTime(final long gameTime) {
        this.worldUnloadEvent.setTime(gameTime);
    }

    public void worldSessionStart() {
        if (this.worldLoadEvent.send(this.eventSender, false)) {
            this.worldLoadTimesEvent.send(this.eventSender);
            this.performanceMetricsEvent.start();
        }
    }

    public void onDisconnect() {
        this.worldLoadEvent.send(this.eventSender, true);
        this.performanceMetricsEvent.stop();
        if (this.worldLoadEvent.wasSent()) {
            this.worldUnloadEvent.send(this.eventSender);
        }
    }

    public void onAdvancementDone(final Level level, final AdvancementHolder holder) {
        Identifier advancementId = holder.id();
        if (holder.value().sendsTelemetryEvent() && "minecraft".equals(advancementId.getNamespace())) {
            long gameTime = level.getGameTime();
            this.eventSender.send(TelemetryEventType.ADVANCEMENT_MADE, properties -> {
                properties.put(TelemetryProperty.ADVANCEMENT_ID, advancementId.toString());
                properties.put(TelemetryProperty.ADVANCEMENT_GAME_TIME, gameTime);
            });
        }
    }
}