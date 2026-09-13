package net.minecraft.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class StrictJsonParser {
   public static JsonElement parse(final Reader reader) throws JsonIOException, JsonSyntaxException {
      try {
         JsonReader jsonReader = new JsonReader(reader);
         jsonReader.setStrictness(Strictness.STRICT);
         JsonElement element = JsonParser.parseReader(jsonReader);
         if (!element.isJsonNull() && jsonReader.peek() != JsonToken.END_DOCUMENT) {
            throw new JsonSyntaxException("Did not consume the entire document.");
         } else {
            return element;
         }
      } catch (MalformedJsonException | NumberFormatException e) {
         throw new JsonSyntaxException(e);
      } catch (IOException e) {
         throw new JsonIOException(e);
      }
   }

   public static JsonElement parse(final String json) throws JsonSyntaxException {
      return parse(new StringReader(json));
   }
}
