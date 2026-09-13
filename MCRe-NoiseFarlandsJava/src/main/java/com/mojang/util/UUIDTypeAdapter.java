package com.mojang.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.UUID;

public class UUIDTypeAdapter extends TypeAdapter<UUID> {
    @Override
    public void write(final JsonWriter out, final UUID value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(UndashedUuid.toString(value));
        }
    }

    @Override
    public UUID read(final JsonReader in) throws IOException {
        return UndashedUuid.fromStringLenient(in.nextString());
    }
}
