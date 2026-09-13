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
   public static JsonElement parse(Reader p_407190_) throws JsonIOException, JsonSyntaxException {
      try {
         JsonReader jsonreader = new JsonReader(p_407190_);
         jsonreader.setStrictness(Strictness.STRICT);
         JsonElement jsonelement = JsonParser.parseReader(jsonreader);
         if (!jsonelement.isJsonNull() && jsonreader.peek() != JsonToken.END_DOCUMENT) {
            throw new JsonSyntaxException("Did not consume the entire document.");
         } else {
            return jsonelement;
         }
      } catch (NumberFormatException | MalformedJsonException malformedjsonexception) {
         throw new JsonSyntaxException(malformedjsonexception);
      } catch (IOException ioexception) {
         throw new JsonIOException(ioexception);
      }
   }

   public static JsonElement parse(String p_407752_) throws JsonSyntaxException {
      return parse(new StringReader(p_407752_));
   }
}
