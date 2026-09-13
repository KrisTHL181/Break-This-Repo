package com.mojang.util;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class InstantTypeAdapter extends TypeAdapter<Instant> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Override
    public void write(final JsonWriter out, final Instant value) throws IOException {
        out.value(FORMATTER.format(value));
    }

    @Override
    public Instant read(final JsonReader in) throws IOException {
        try {
            return Instant.from(FORMATTER.parse(in.nextString()));
        } catch (final DateTimeParseException e) {
            throw new JsonParseException("Malformed ISO instant format");
        }
    }
}
