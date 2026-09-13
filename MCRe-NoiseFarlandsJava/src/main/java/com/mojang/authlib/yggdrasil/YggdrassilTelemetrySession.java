package com.mojang.authlib.yggdrasil;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.mojang.authlib.Environment;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.TelemetryEvent;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.yggdrasil.request.TelemetryEventsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Instant;
import java.util.concurrent.Executor;

public class YggdrassilTelemetrySession implements TelemetrySession {
    private static final Logger LOGGER = LoggerFactory.getLogger(YggdrassilTelemetrySession.class);

    private static final String SOURCE = "minecraft.java";

    private final MinecraftClient minecraftClient;
    private final URL routeEvents;
    private final Executor ioExecutor;

    @VisibleForTesting
    YggdrassilTelemetrySession(final MinecraftClient minecraftClient, final Environment environment, final Executor ioExecutor) {
        this.minecraftClient = minecraftClient;
        routeEvents = HttpAuthenticationService.constantURL(environment.servicesHost() + "/events");
        this.ioExecutor = ioExecutor;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public TelemetryEvent createNewEvent(final String type) {
        return new YggdrassilTelemetryEvent(this, type);
    }

    void sendEvent(final String type, final JsonObject data) {
        final Instant sendTime = Instant.now();
        final TelemetryEventsRequest.Event request = new TelemetryEventsRequest.Event(SOURCE, type, sendTime, data);

        ioExecutor.execute(() -> {
            try {
                final TelemetryEventsRequest envelope = new TelemetryEventsRequest(ImmutableList.of(request));
                minecraftClient.post(routeEvents, envelope, Void.class);
            } catch (final MinecraftClientException e) {
                // Telemetry is optional, so don't send too much warnings
                LOGGER.debug("Failed to send telemetry event {}", request.name(), e);
            }
        });
    }
}
