package com.mojang.authlib.yggdrasil;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.authlib.minecraft.TelemetryEvent;

import javax.annotation.Nullable;

public class YggdrassilTelemetryEvent implements TelemetryEvent {
    private final YggdrassilTelemetrySession service;
    private final String type;

    @Nullable
    private JsonObject data = new JsonObject();

    YggdrassilTelemetryEvent(final YggdrassilTelemetrySession service, final String type) {
        this.service = service;
        this.type = type;
    }

    private JsonObject data() {
        if (data == null) {
            throw new IllegalStateException("Event already sent");
        }
        return data;
    }

    @Override
    public void addProperty(final String id, final String value) {
        data().addProperty(id, value);
    }

    @Override
    public void addProperty(final String id, final int value) {
        data().addProperty(id, value);
    }

    @Override
    public void addProperty(final String id, final long value) {
        data().addProperty(id, value);
    }

    @Override
    public void addProperty(final String id, final boolean value) {
        data().addProperty(id, value);
    }

    @Override
    public void addNullProperty(final String id) {
        data().add(id, JsonNull.INSTANCE);
    }

    @Override
    public void send() {
        service.sendEvent(type, data);
        data = null;
    }
}
