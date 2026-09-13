package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.util.DelegateDataOutput;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class NbtIo {
   private static final OpenOption[] SYNC_OUTPUT_OPTIONS = new OpenOption[]{
      StandardOpenOption.SYNC, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
   };

   public static CompoundTag readCompressed(final Path file, final NbtAccounter accounter) throws IOException {
      try (
         InputStream rawInput = Files.newInputStream(file);
         InputStream input = new FastBufferedInputStream(rawInput);
      ) {
         return readCompressed(input, accounter);
      }
   }

   private static DataInputStream createDecompressorStream(final InputStream in) throws IOException {
      return new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(in)));
   }

   private static DataOutputStream createCompressorStream(final OutputStream out) throws IOException {
      return new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(out)));
   }

   public static CompoundTag readCompressed(final InputStream in, final NbtAccounter accounter) throws IOException {
      try (DataInputStream dis = createDecompressorStream(in)) {
         return read(dis, accounter);
      }
   }

   public static void parseCompressed(final Path file, final StreamTagVisitor output, final NbtAccounter accounter) throws IOException {
      try (
         InputStream rawInput = Files.newInputStream(file);
         InputStream input = new FastBufferedInputStream(rawInput);
      ) {
         parseCompressed(input, output, accounter);
      }
   }

   public static void parseCompressed(final InputStream in, final StreamTagVisitor output, final NbtAccounter accounter) throws IOException {
      try (DataInputStream dis = createDecompressorStream(in)) {
         parse(dis, output, accounter);
      }
   }

   public static void writeCompressed(final CompoundTag tag, final Path file) throws IOException {
      try (
         OutputStream out = Files.newOutputStream(file, SYNC_OUTPUT_OPTIONS);
         OutputStream bufferedOut = new BufferedOutputStream(out);
      ) {
         writeCompressed(tag, bufferedOut);
      }
   }

   public static void writeCompressed(final CompoundTag tag, final OutputStream out) throws IOException {
      try (DataOutputStream dos = createCompressorStream(out)) {
         write(tag, dos);
      }
   }

   public static void write(final CompoundTag tag, final Path file) throws IOException {
      try (
         OutputStream out = Files.newOutputStream(file, SYNC_OUTPUT_OPTIONS);
         OutputStream bufferedOut = new BufferedOutputStream(out);
         DataOutputStream dos = new DataOutputStream(bufferedOut);
      ) {
         write(tag, dos);
      }
   }

   public static @Nullable CompoundTag read(final Path file) throws IOException {
      if (!Files.exists(file)) {
         return null;
      }

      try (
         InputStream in = Files.newInputStream(file);
         DataInputStream dis = new DataInputStream(in);
      ) {
         return read(dis, NbtAccounter.unlimitedHeap());
      }
   }

   public static CompoundTag read(final DataInput input) throws IOException {
      return read(input, NbtAccounter.unlimitedHeap());
   }

   public static CompoundTag read(final DataInput input, final NbtAccounter accounter) throws IOException {
      if (readUnnamedTag(input, accounter) instanceof CompoundTag compoundTag) {
         return compoundTag;
      } else {
         throw new IOException("Root tag must be a named compound tag");
      }
   }

   public static void write(final CompoundTag tag, final DataOutput output) throws IOException {
      writeUnnamedTagWithFallback(tag, output);
   }

   public static void parse(final DataInput input, final StreamTagVisitor output, final NbtAccounter accounter) throws IOException {
      TagType<?> type = TagTypes.getType(input.readByte());
      if (type == EndTag.TYPE) {
         if (output.visitRootEntry(EndTag.TYPE) == StreamTagVisitor.ValueResult.CONTINUE) {
            output.visitEnd();
         }
      } else {
         switch (output.visitRootEntry(type)) {
            case HALT:
            default:
               break;
            case BREAK:
               StringTag.skipString(input);
               type.skip(input, accounter);
               break;
            case CONTINUE:
               StringTag.skipString(input);
               type.parse(input, output, accounter);
         }
      }
   }

   public static Tag readAnyTag(final DataInput input, final NbtAccounter accounter) throws IOException {
      byte type = input.readByte();
      return type == 0 ? EndTag.INSTANCE : readTagSafe(input, accounter, type);
   }

   public static void writeAnyTag(final Tag tag, final DataOutput output) throws IOException {
      output.writeByte(tag.getId());
      if (tag.getId() != 0) {
         tag.write(output);
      }
   }

   public static void writeUnnamedTag(final Tag tag, final DataOutput output) throws IOException {
      output.writeByte(tag.getId());
      if (tag.getId() != 0) {
         output.writeUTF("");
         tag.write(output);
      }
   }

   public static void writeUnnamedTagWithFallback(final Tag tag, final DataOutput output) throws IOException {
      writeUnnamedTag(tag, new NbtIo.StringFallbackDataOutput(output));
   }

   @VisibleForTesting
   public static Tag readUnnamedTag(final DataInput input, final NbtAccounter accounter) throws IOException {
      byte type = input.readByte();
      if (type == 0) {
         return EndTag.INSTANCE;
      }

      StringTag.skipString(input);
      return readTagSafe(input, accounter, type);
   }

   private static Tag readTagSafe(final DataInput input, final NbtAccounter accounter, final byte type) {
      try {
         return TagTypes.getType(type).load(input, accounter);
      } catch (IOException e) {
         CrashReport report = CrashReport.forThrowable(e, "Loading NBT data");
         CrashReportCategory category = report.addCategory("NBT Tag");
         category.setDetail("Tag type", type);
         throw new ReportedNbtException(report);
      }
   }

   public static class StringFallbackDataOutput extends DelegateDataOutput {
      public StringFallbackDataOutput(final DataOutput parent) {
         super(parent);
      }

      @Override
      public void writeUTF(final String s) throws IOException {
         try {
            super.writeUTF(s);
         } catch (UTFDataFormatException exception) {
            Util.logAndPauseIfInIde("Failed to write NBT String", exception);
            super.writeUTF("");
         }
      }
   }
}
