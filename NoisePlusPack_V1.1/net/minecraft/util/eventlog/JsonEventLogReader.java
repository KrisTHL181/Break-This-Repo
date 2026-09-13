package net.minecraft.util.eventlog;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import org.jspecify.annotations.Nullable;

public interface JsonEventLogReader<T> extends Closeable {
   static <T> JsonEventLogReader<T> create(final Codec<T> p_261600_, Reader p_261836_) {
      final JsonReader jsonreader = new JsonReader(p_261836_);
      jsonreader.setStrictness(Strictness.LENIENT);
      return new JsonEventLogReader<T>() {
         @Override
         public @Nullable T next() throws IOException {
            try {
               if (!jsonreader.hasNext()) {
                  return null;
               }

               JsonElement jsonelement = JsonParser.parseReader(jsonreader);
               return (T)p_261600_.parse(JsonOps.INSTANCE, jsonelement).getOrThrow(IOException::new);
            } catch (JsonParseException jsonparseexception) {
               throw new IOException(jsonparseexception);
            } catch (EOFException eofexception) {
               return null;
            }
         }

         @Override
         public void close() throws IOException {
            jsonreader.close();
         }
      };
   }

   @Nullable T next() throws IOException;
}
