package net.minecraft.util.eventlog;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class EventLogDirectory {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int COMPRESS_BUFFER_SIZE = 4096;
   private static final String COMPRESSED_EXTENSION = ".gz";
   private final Path root;
   private final String extension;

   private EventLogDirectory(final Path root, final String extension) {
      this.root = root;
      this.extension = extension;
   }

   public static EventLogDirectory open(final Path root, final String extension) throws IOException {
      Files.createDirectories(root);
      return new EventLogDirectory(root, extension);
   }

   public EventLogDirectory.FileList listFiles() throws IOException {
      try (Stream<Path> list = Files.list(this.root)) {
         return new EventLogDirectory.FileList(list.filter(x$0 -> Files.isRegularFile(x$0)).map(this::parseFile).filter(Objects::nonNull).toList());
      }
   }

   private EventLogDirectory.@Nullable File parseFile(final Path path) {
      String fileName = path.getFileName().toString();
      int extensionIndex = fileName.indexOf(46);
      if (extensionIndex == -1) {
         return null;
      }

      EventLogDirectory.FileId id = EventLogDirectory.FileId.parse(fileName.substring(0, extensionIndex));
      if (id != null) {
         String extension = fileName.substring(extensionIndex);
         if (extension.equals(this.extension)) {
            return new EventLogDirectory.RawFile(path, id);
         }

         if (extension.equals(this.extension + ".gz")) {
            return new EventLogDirectory.CompressedFile(path, id);
         }
      }

      return null;
   }

   private static void tryCompress(final Path raw, final Path compressed) throws IOException {
      if (Files.exists(compressed)) {
         throw new IOException("Compressed target file already exists: " + compressed);
      }

      try (FileChannel channel = FileChannel.open(raw, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
         FileLock lock = channel.tryLock();
         if (lock == null) {
            throw new IOException("Raw log file is already locked, cannot compress: " + raw);
         }

         writeCompressed(channel, compressed);
         channel.truncate(0L);
      }

      Files.delete(raw);
   }

   private static void writeCompressed(final ReadableByteChannel channel, final Path target) throws IOException {
      try (OutputStream output = new GZIPOutputStream(Files.newOutputStream(target))) {
         byte[] bytes = new byte[4096];
         ByteBuffer buffer = ByteBuffer.wrap(bytes);

         while (channel.read(buffer) >= 0) {
            buffer.flip();
            output.write(bytes, 0, buffer.limit());
            buffer.clear();
         }
      }
   }

   public EventLogDirectory.RawFile createNewFile(final LocalDate date) throws IOException {
      int index = 1;
      Set<EventLogDirectory.FileId> files = this.listFiles().ids();

      EventLogDirectory.FileId id;
      do {
         id = new EventLogDirectory.FileId(date, index++);
      } while (files.contains(id));

      EventLogDirectory.RawFile file = new EventLogDirectory.RawFile(this.root.resolve(id.toFileName(this.extension)), id);
      Files.createFile(file.path());
      return file;
   }

   public record CompressedFile(Path path, EventLogDirectory.FileId id) implements EventLogDirectory.File {
      @Override
      public @Nullable Reader openReader() throws IOException {
         return !Files.exists(this.path)
            ? null
            : new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(this.path)), StandardCharsets.UTF_8));
      }

      @Override
      public EventLogDirectory.CompressedFile compress() {
         return this;
      }
   }

   public interface File {
      Path path();

      EventLogDirectory.FileId id();

      @Nullable Reader openReader() throws IOException;

      EventLogDirectory.CompressedFile compress() throws IOException;
   }

   public record FileId(LocalDate date, int index) {
      private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

      public static EventLogDirectory.@Nullable FileId parse(final String name) {
         int separator = name.indexOf("-");
         if (separator == -1) {
            return null;
         }

         String date = name.substring(0, separator);
         String index = name.substring(separator + 1);

         try {
            return new EventLogDirectory.FileId(LocalDate.parse(date, DATE_FORMATTER), Integer.parseInt(index));
         } catch (NumberFormatException | DateTimeParseException e) {
            return null;
         }
      }

      @Override
      public String toString() {
         return DATE_FORMATTER.format(this.date) + "-" + this.index;
      }

      public String toFileName(final String extension) {
         return this + extension;
      }
   }

   public static class FileList implements Iterable<EventLogDirectory.File> {
      private final List<EventLogDirectory.File> files;

      private FileList(final List<EventLogDirectory.File> files) {
         this.files = new ArrayList<>(files);
      }

      public EventLogDirectory.FileList prune(final LocalDate date, final int expiryDays) {
         this.files.removeIf(file -> {
            EventLogDirectory.FileId id = file.id();
            LocalDate expiresAt = id.date().plusDays(expiryDays);
            if (!date.isBefore(expiresAt)) {
               try {
                  Files.delete(file.path());
                  return true;
               } catch (IOException e) {
                  EventLogDirectory.LOGGER.warn("Failed to delete expired event log file: {}", file.path(), e);
               }
            }

            return false;
         });
         return this;
      }

      public EventLogDirectory.FileList compressAll() {
         ListIterator<EventLogDirectory.File> iterator = this.files.listIterator();

         while (iterator.hasNext()) {
            EventLogDirectory.File file = iterator.next();

            try {
               iterator.set(file.compress());
            } catch (IOException e) {
               EventLogDirectory.LOGGER.warn("Failed to compress event log file: {}", file.path(), e);
            }
         }

         return this;
      }

      @Override
      public Iterator<EventLogDirectory.File> iterator() {
         return this.files.iterator();
      }

      public Stream<EventLogDirectory.File> stream() {
         return this.files.stream();
      }

      public Set<EventLogDirectory.FileId> ids() {
         return this.files.stream().map(EventLogDirectory.File::id).collect(Collectors.toSet());
      }
   }

   public record RawFile(Path path, EventLogDirectory.FileId id) implements EventLogDirectory.File {
      public FileChannel openChannel() throws IOException {
         return FileChannel.open(this.path, StandardOpenOption.WRITE, StandardOpenOption.READ);
      }

      @Override
      public @Nullable Reader openReader() throws IOException {
         return Files.exists(this.path) ? Files.newBufferedReader(this.path) : null;
      }

      @Override
      public EventLogDirectory.CompressedFile compress() throws IOException {
         Path compressedPath = this.path.resolveSibling(this.path.getFileName().toString() + ".gz");
         EventLogDirectory.tryCompress(this.path, compressedPath);
         return new EventLogDirectory.CompressedFile(compressedPath, this.id);
      }
   }
}
