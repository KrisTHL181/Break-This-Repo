package net.minecraft.client.telemetry;

import com.google.common.base.Suppliers;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.UserApiService;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ClientTelemetryManager implements AutoCloseable {
    private static final AtomicInteger THREAD_COUNT = new AtomicInteger(1);
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread result = new Thread(r, "Telemetry-Sender-#" + THREAD_COUNT.getAndIncrement());
        result.setDaemon(true);
        return result;
    });
    private final Minecraft minecraft;
    private final UserApiService userApiService;
    private final TelemetryPropertyMap deviceSessionProperties;
    private final Path logDirectory;
    private final CompletableFuture<Optional<TelemetryLogManager>> logManager;
    private final Supplier<TelemetryEventSender> outsideSessionSender = Suppliers.memoize(this::createEventSender);

    public ClientTelemetryManager(final Minecraft minecraft, final UserApiService userApiService, final User user) {
        this.minecraft = minecraft;
        this.userApiService = userApiService;
        TelemetryPropertyMap.Builder properties = TelemetryPropertyMap.builder();
        user.getXuid().ifPresent(id -> properties.put(TelemetryProperty.USER_ID, id));
        user.getClientId().ifPresent(id -> properties.put(TelemetryProperty.CLIENT_ID, id));
        properties.put(TelemetryProperty.MINECRAFT_SESSION_ID, UUID.randomUUID());
        properties.put(TelemetryProperty.GAME_VERSION, SharedConstants.getCurrentVersion().id());
        properties.put(TelemetryProperty.OPERATING_SYSTEM, Util.getPlatform().telemetryName());
        properties.put(TelemetryProperty.PLATFORM, System.getProperty("os.name"));
        properties.put(TelemetryProperty.CLIENT_MODDED, Minecraft.checkModStatus().shouldReportAsModified());
        properties.putIfNotNull(TelemetryProperty.LAUNCHER_NAME, Minecraft.getLauncherBrand());
        this.deviceSessionProperties = properties.build();
        this.logDirectory = minecraft.gameDirectory.toPath().resolve("logs/telemetry");
        this.logManager = TelemetryLogManager.open(this.logDirectory);
    }

    public WorldSessionTelemetryManager createWorldSessionManager(
        final boolean newWorld, final @Nullable Duration worldLoadDuration, final @Nullable String minigameName, final UUID sessionId
    ) {
        return new WorldSessionTelemetryManager(this.createEventSender(), newWorld, worldLoadDuration, minigameName, sessionId);
    }

    public TelemetryEventSender getOutsideSessionSender() {
        return this.outsideSessionSender.get();
    }

    private TelemetryEventSender createEventSender() {
        if (!this.minecraft.allowsTelemetry()) {
            return TelemetryEventSender.DISABLED;
        }

        TelemetrySession telemetrySession = this.userApiService.newTelemetrySession(EXECUTOR);
        if (!telemetrySession.isEnabled()) {
            return TelemetryEventSender.DISABLED;
        }

        CompletableFuture<Optional<TelemetryEventLogger>> loggerFuture = this.logManager
            .thenCompose(manager -> manager.map(TelemetryLogManager::openLogger).orElseGet(() -> CompletableFuture.completedFuture(Optional.empty())));
        return (type, buildFunction) -> {
            if (!type.isOptIn() || Minecraft.getInstance().telemetryOptInExtra()) {
                TelemetryPropertyMap.Builder properties = TelemetryPropertyMap.builder();
                properties.putAll(this.deviceSessionProperties);
                properties.put(TelemetryProperty.EVENT_TIMESTAMP_UTC, Instant.now());
                properties.put(TelemetryProperty.OPT_IN, type.isOptIn());
                buildFunction.accept(properties);
                TelemetryEventInstance event = new TelemetryEventInstance(type, properties.build());
                loggerFuture.thenAccept(logger -> {
                    if (!logger.isEmpty()) {
                        logger.get().log(event);
                        if (!SharedConstants.IS_RUNNING_IN_IDE || !SharedConstants.DEBUG_DONT_SEND_TELEMETRY_TO_BACKEND) {
                            event.export(telemetrySession).send();
                        }
                    }
                });
            }
        };
    }

    public Path getLogDirectory() {
        return this.logDirectory;
    }

    @Override
    public void close() {
        this.logManager.thenAccept(manager -> manager.ifPresent(TelemetryLogManager::close));
    }
}