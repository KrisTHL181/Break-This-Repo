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
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int COMPRESS_BUFFER_SIZE = 4096;
   private static final String COMPRESSED_EXTENSION = ".gz";
   private final Path root;
   private final String extension;

   private EventLogDirectory(Path p_261546_, String p_261467_) {
      this.root = p_261546_;
      this.extension = p_261467_;
   }

   public static EventLogDirectory open(Path p_261743_, String p_261659_) throws IOException {
      Files.createDirectories(p_261743_);
      return new EventLogDirectory(p_261743_, p_261659_);
   }

   public EventLogDirectory.FileList listFiles() throws IOException {
      try (Stream<Path> stream = Files.list(this.root)) {
         return new EventLogDirectory.FileList(
            stream.filter(p_262170_ -> Files.isRegularFile(p_262170_)).map(this::parseFile).filter(Objects::nonNull).toList()
         );
      }
   }

   private EventLogDirectory.@Nullable File parseFile(Path p_261985_) {
      String s = p_261985_.getFileName().toString();
      int i = s.indexOf(46);
      if (i == -1) {
         return null;
      }

      EventLogDirectory.FileId eventlogdirectory$fileid = EventLogDirectory.FileId.parse(s.substring(0, i));
      if (eventlogdirectory$fileid != null) {
         String s1 = s.substring(i);
         if (s1.equals(this.extension)) {
            return new EventLogDirectory.RawFile(p_261985_, eventlogdirectory$fileid);
         }

         if (s1.equals(this.extension + ".gz")) {
            return new EventLogDirectory.CompressedFile(p_261985_, eventlogdirectory$fileid);
         }
      }

      return null;
   }

   static void tryCompress(Path p_261741_, Path p_262101_) throws IOException {
      if (Files.exists(p_262101_)) {
         throw new IOException("Compressed target file already exists: " + p_262101_);
      }

      try (FileChannel filechannel = FileChannel.open(p_261741_, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
         FileLock filelock = filechannel.tryLock();
         if (filelock == null) {
            throw new IOException("Raw log file is already locked, cannot compress: " + p_261741_);
         }

         writeCompressed(filechannel, p_262101_);
         filechannel.truncate(0L);
      }

      Files.delete(p_261741_);
   }

   private static void writeCompressed(ReadableByteChannel p_262066_, Path p_262054_) throws IOException {
      try (OutputStream outputstream = new GZIPOutputStream(Files.newOutputStream(p_262054_))) {
         byte[] abyte = new byte[4096];
         ByteBuffer bytebuffer = ByteBuffer.wrap(abyte);

         while (p_262066_.read(bytebuffer) >= 0) {
            bytebuffer.flip();
            outputstream.write(abyte, 0, bytebuffer.limit());
            bytebuffer.clear();
         }
      }
   }

   public EventLogDirectory.RawFile createNewFile(LocalDate p_261865_) throws IOException {
      int i = 1;
      Set<EventLogDirectory.FileId> set = this.listFiles().ids();

      EventLogDirectory.FileId eventlogdirectory$fileid;
      do {
         eventlogdirectory$fileid = new EventLogDirectory.FileId(p_261865_, i++);
      } while (set.contains(eventlogdirectory$fileid));

      EventLogDirectory.RawFile eventlogdirectory$rawfile = new EventLogDirectory.RawFile(
         this.root.resolve(eventlogdirectory$fileid.toFileName(this.extension)), eventlogdirectory$fileid
      );
      Files.createFile(eventlogdirectory$rawfile.path());
      return eventlogdirectory$rawfile;
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

      public static EventLogDirectory.@Nullable FileId parse(String p_261762_) {
         int i = p_261762_.indexOf("-");
         if (i == -1) {
            return null;
         }

         String s = p_261762_.substring(0, i);
         String s1 = p_261762_.substring(i + 1);

         try {
            return new EventLogDirectory.FileId(LocalDate.parse(s, DATE_FORMATTER), Integer.parseInt(s1));
         } catch (DateTimeParseException | NumberFormatException numberformatexception) {
            return null;
         }
      }

      @Override
      public String toString() {
         return DATE_FORMATTER.format(this.date) + "-" + this.index;
      }

      public String toFileName(String p_261982_) {
         return this + p_261982_;
      }
   }

   public static class FileList implements Iterable<EventLogDirectory.File> {
      private final List<EventLogDirectory.File> files;

      FileList(List<EventLogDirectory.File> p_261941_) {
         this.files = new ArrayList<>(p_261941_);
      }

      public EventLogDirectory.FileList prune(LocalDate p_261825_, int p_261918_) {
         this.files.removeIf(p_261494_ -> {
            EventLogDirectory.FileId eventlogdirectory$fileid = p_261494_.id();
            LocalDate localdate = eventlogdirectory$fileid.date().plusDays(p_261918_);
            if (!p_261825_.isBefore(localdate)) {
               try {
                  Files.delete(p_261494_.path());
                  return true;
               } catch (IOException ioexception) {
                  EventLogDirectory.LOGGER.warn("Failed to delete expired event log file: {}", p_261494_.path(), ioexception);
               }
            }

            return false;
         });
         return this;
      }

      public EventLogDirectory.FileList compressAll() {
         ListIterator<EventLogDirectory.File> listiterator = this.files.listIterator();

         while (listiterator.hasNext()) {
            EventLogDirectory.File eventlogdirectory$file = listiterator.next();

            try {
               listiterator.set(eventlogdirectory$file.compress());
            } catch (IOException ioexception) {
               EventLogDirectory.LOGGER.warn("Failed to compress event log file: {}", eventlogdirectory$file.path(), ioexception);
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
         Path path = this.path.resolveSibling(this.path.getFileName().toString() + ".gz");
         EventLogDirectory.tryCompress(this.path, path);
         return new EventLogDirectory.CompressedFile(path, this.id);
      }
   }
}
