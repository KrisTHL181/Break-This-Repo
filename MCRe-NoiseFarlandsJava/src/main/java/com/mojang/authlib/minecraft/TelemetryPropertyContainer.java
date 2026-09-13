package com.mojang.authlib.minecraft;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public interface TelemetryPropertyContainer {
    void addProperty(String id, String value);

    void addProperty(String id, int value);

    void addProperty(String id, long value);

    void addProperty(String id, boolean value);

    void addNullProperty(String id);

    static TelemetryPropertyContainer forJsonObject(final JsonObject object) {
        return new TelemetryPropertyContainer() {
            @Override
            public void addProperty(final String id, final String value) {
                object.addProperty(id, value);
            }

            @Override
            public void addProperty(final String id, final int value) {
                object.addProperty(id, value);
            }

            @Override
            public void addProperty(final String id, final long value) {
                object.addProperty(id, value);
            }

            @Override
            public void addProperty(final String id, final boolean value) {
                object.addProperty(id, value);
            }

            @Override
            public void addNullProperty(final String id) {
                object.add(id, JsonNull.INSTANCE);
            }
        };
    }
}
