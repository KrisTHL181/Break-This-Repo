package com.mojang.util;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

public class ByteBufferTypeAdapter extends TypeAdapter<ByteBuffer> {
    @Override
    public void write(final JsonWriter out, final ByteBuffer value) throws IOException {
        out.value(Base64.getEncoder().encodeToString(value.array()));
    }

    @Override
    public ByteBuffer read(final JsonReader in) throws IOException {
        try {
            return ByteBuffer.wrap(Base64.getDecoder().decode(in.nextString()));
        } catch (final IllegalArgumentException e) {
            throw new JsonParseException("Malformed base64 string", e);
        }
    }
}
