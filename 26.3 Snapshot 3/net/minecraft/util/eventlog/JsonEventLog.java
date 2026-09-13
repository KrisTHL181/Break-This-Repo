package net.minecraft.util.eventlog;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;

public class JsonEventLog<T> implements Closeable {
   private static final Gson GSON = new Gson();
   private final Codec<T> codec;
   private final FileChannel channel;
   private final AtomicInteger referenceCount = new AtomicInteger(1);

   public JsonEventLog(final Codec<T> codec, final FileChannel channel) {
      this.codec = codec;
      this.channel = channel;
   }

   public static <T> JsonEventLog<T> open(final Codec<T> codec, final Path path) throws IOException {
      FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE);
      return new JsonEventLog<>(codec, channel);
   }

   public void write(final T event) throws IOException {
      JsonElement json = (JsonElement)this.codec.encodeStart(JsonOps.INSTANCE, event).getOrThrow(IOException::new);
      this.channel.position(this.channel.size());
      Writer writer = Channels.newWriter(this.channel, StandardCharsets.UTF_8);
      GSON.toJson(json, GSON.newJsonWriter(writer));
      writer.write(10);
      writer.flush();
   }

   public JsonEventLogReader<T> openReader() throws IOException {
      if (this.referenceCount.get() <= 0) {
         throw new IOException("Event log has already been closed");
      }

      this.referenceCount.incrementAndGet();
      final JsonEventLogReader<T> reader = JsonEventLogReader.create(this.codec, Channels.newReader(this.channel, StandardCharsets.UTF_8));
      return new JsonEventLogReader<T>() {
         private volatile long position;

         @Override
         public @Nullable T next() throws IOException {
            try {
               JsonEventLog.this.channel.position(this.position);
               return reader.next();
            } finally {
               this.position = JsonEventLog.this.channel.position();
            }
         }

         @Override
         public void close() throws IOException {
            JsonEventLog.this.releaseReference();
         }
      };
   }

   @Override
   public void close() throws IOException {
      this.releaseReference();
   }

   private void releaseReference() throws IOException {
      if (this.referenceCount.decrementAndGet() <= 0) {
         this.channel.close();
      }
   }
}
