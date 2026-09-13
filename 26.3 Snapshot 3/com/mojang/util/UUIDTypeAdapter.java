package com.mojang.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.UUID;

public class UUIDTypeAdapter extends TypeAdapter<UUID> {
   public void write(JsonWriter out, UUID value) throws IOException {
      if (value == null) {
         out.nullValue();
      } else {
         out.value(UndashedUuid.toString(value));
      }
   }

   public UUID read(JsonReader in) throws IOException {
      return UndashedUuid.fromStringLenient(in.nextString());
   }
}
