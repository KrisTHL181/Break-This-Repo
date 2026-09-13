package net.minecraft.server;

import com.google.common.collect.Lists;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class ChainedJsonException extends IOException {
   private final List<ChainedJsonException.Entry> entries = Lists.newArrayList();
   private final String message;

   public ChainedJsonException(final String message) {
      this.entries.add(new ChainedJsonException.Entry());
      this.message = message;
   }

   public ChainedJsonException(final String message, final Throwable cause) {
      super(cause);
      this.entries.add(new ChainedJsonException.Entry());
      this.message = message;
   }

   public void prependJsonKey(final String key) {
      this.entries.get(0).addJsonKey(key);
   }

   public void setFilenameAndFlush(final String filename) {
      this.entries.get(0).filename = filename;
      this.entries.add(0, new ChainedJsonException.Entry());
   }

   @Override
   public String getMessage() {
      return "Invalid " + this.entries.get(this.entries.size() - 1) + ": " + this.message;
   }

   public static ChainedJsonException forException(final Exception e) {
      if (e instanceof ChainedJsonException chainedJsonException) {
         return chainedJsonException;
      } else {
         String message = e.getMessage();
         if (e instanceof FileNotFoundException) {
            message = "File not found";
         }

         return new ChainedJsonException(message, e);
      }
   }

   public static class Entry {
      private @Nullable String filename;
      private final List<String> jsonKeys = Lists.newArrayList();

      private Entry() {
      }

      private void addJsonKey(final String name) {
         this.jsonKeys.add(0, name);
      }

      public @Nullable String getFilename() {
         return this.filename;
      }

      public String getJsonKeys() {
         return StringUtils.join(this.jsonKeys, "->");
      }

      @Override
      public String toString() {
         if (this.filename != null) {
            return this.jsonKeys.isEmpty() ? this.filename : this.filename + " " + this.getJsonKeys();
         } else {
            return this.jsonKeys.isEmpty() ? "(Unknown file)" : "(Unknown file) " + this.getJsonKeys();
         }
      }
   }
}
