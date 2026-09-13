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

   public static CompoundTag readCompressed(Path p_310303_, NbtAccounter p_311830_) throws IOException {
      try (
         InputStream inputstream = Files.newInputStream(p_310303_);
         InputStream inputstream1 = new FastBufferedInputStream(inputstream);
      ) {
         return readCompressed(inputstream1, p_311830_);
      }
   }

   private static DataInputStream createDecompressorStream(InputStream p_202494_) throws IOException {
      return new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(p_202494_)));
   }

   private static DataOutputStream createCompressorStream(OutputStream p_310411_) throws IOException {
      return new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(p_310411_)));
   }

   public static CompoundTag readCompressed(InputStream p_313037_, NbtAccounter p_312435_) throws IOException {
      try (DataInputStream datainputstream = createDecompressorStream(p_313037_)) {
         return read(datainputstream, p_312435_);
      }
   }

   public static void parseCompressed(Path p_310443_, StreamTagVisitor p_202489_, NbtAccounter p_301727_) throws IOException {
      try (
         InputStream inputstream = Files.newInputStream(p_310443_);
         InputStream inputstream1 = new FastBufferedInputStream(inputstream);
      ) {
         parseCompressed(inputstream1, p_202489_, p_301727_);
      }
   }

   public static void parseCompressed(InputStream p_202491_, StreamTagVisitor p_202492_, NbtAccounter p_301762_) throws IOException {
      try (DataInputStream datainputstream = createDecompressorStream(p_202491_)) {
         parse(datainputstream, p_202492_, p_301762_);
      }
   }

   public static void writeCompressed(CompoundTag p_128945_, Path p_310344_) throws IOException {
      try (
         OutputStream outputstream = Files.newOutputStream(p_310344_, SYNC_OUTPUT_OPTIONS);
         OutputStream outputstream1 = new BufferedOutputStream(outputstream);
      ) {
         writeCompressed(p_128945_, outputstream1);
      }
   }

   public static void writeCompressed(CompoundTag p_128948_, OutputStream p_128949_) throws IOException {
      try (DataOutputStream dataoutputstream = createCompressorStream(p_128949_)) {
         write(p_128948_, dataoutputstream);
      }
   }

   public static void write(CompoundTag p_128956_, Path p_311890_) throws IOException {
      try (
         OutputStream outputstream = Files.newOutputStream(p_311890_, SYNC_OUTPUT_OPTIONS);
         OutputStream outputstream1 = new BufferedOutputStream(outputstream);
         DataOutputStream dataoutputstream = new DataOutputStream(outputstream1);
      ) {
         write(p_128956_, dataoutputstream);
      }
   }

   public static @Nullable CompoundTag read(Path p_310670_) throws IOException {
      if (!Files.exists(p_310670_)) {
         return null;
      }

      try (
         InputStream inputstream = Files.newInputStream(p_310670_);
         DataInputStream datainputstream = new DataInputStream(inputstream);
      ) {
         return read(datainputstream, NbtAccounter.unlimitedHeap());
      }
   }

   public static CompoundTag read(DataInput p_128929_) throws IOException {
      return read(p_128929_, NbtAccounter.unlimitedHeap());
   }

   public static CompoundTag read(DataInput p_128935_, NbtAccounter p_128936_) throws IOException {
      Tag tag = readUnnamedTag(p_128935_, p_128936_);
      if (tag instanceof CompoundTag) {
         return (CompoundTag)tag;
      } else {
         throw new IOException("Root tag must be a named compound tag");
      }
   }

   public static void write(CompoundTag p_128942_, DataOutput p_128943_) throws IOException {
      writeUnnamedTagWithFallback(p_128942_, p_128943_);
   }

   public static void parse(DataInput p_197510_, StreamTagVisitor p_197511_, NbtAccounter p_301755_) throws IOException {
      TagType<?> tagtype = TagTypes.getType(p_197510_.readByte());
      if (tagtype == EndTag.TYPE) {
         if (p_197511_.visitRootEntry(EndTag.TYPE) == StreamTagVisitor.ValueResult.CONTINUE) {
            p_197511_.visitEnd();
         }
      } else {
         switch (p_197511_.visitRootEntry(tagtype)) {
            case HALT:
            default:
               break;
            case BREAK:
               StringTag.skipString(p_197510_);
               tagtype.skip(p_197510_, p_301755_);
               break;
            case CONTINUE:
               StringTag.skipString(p_197510_);
               tagtype.parse(p_197510_, p_197511_, p_301755_);
         }
      }
   }

   public static Tag readAnyTag(DataInput p_301023_, NbtAccounter p_299704_) throws IOException {
      byte b0 = p_301023_.readByte();
      return b0 == 0 ? EndTag.INSTANCE : readTagSafe(p_301023_, p_299704_, b0);
   }

   public static void writeAnyTag(Tag p_300328_, DataOutput p_297970_) throws IOException {
      p_297970_.writeByte(p_300328_.getId());
      if (p_300328_.getId() != 0) {
         p_300328_.write(p_297970_);
      }
   }

   public static void writeUnnamedTag(Tag p_128951_, DataOutput p_128952_) throws IOException {
      p_128952_.writeByte(p_128951_.getId());
      if (p_128951_.getId() != 0) {
         p_128952_.writeUTF("");
         p_128951_.write(p_128952_);
      }
   }

   public static void writeUnnamedTagWithFallback(Tag p_310490_, DataOutput p_311501_) throws IOException {
      writeUnnamedTag(p_310490_, new NbtIo.StringFallbackDataOutput(p_311501_));
   }

   @VisibleForTesting
   public static Tag readUnnamedTag(DataInput p_128931_, NbtAccounter p_128933_) throws IOException {
      byte b0 = p_128931_.readByte();
      if (b0 == 0) {
         return EndTag.INSTANCE;
      }

      StringTag.skipString(p_128931_);
      return readTagSafe(p_128931_, p_128933_, b0);
   }

   private static Tag readTagSafe(DataInput p_299672_, NbtAccounter p_299171_, byte p_300451_) {
      try {
         return TagTypes.getType(p_300451_).load(p_299672_, p_299171_);
      } catch (IOException ioexception) {
         CrashReport crashreport = CrashReport.forThrowable(ioexception, "Loading NBT data");
         CrashReportCategory crashreportcategory = crashreport.addCategory("NBT Tag");
         crashreportcategory.setDetail("Tag type", p_300451_);
         throw new ReportedNbtException(crashreport);
      }
   }

   public static class StringFallbackDataOutput extends DelegateDataOutput {
      public StringFallbackDataOutput(DataOutput p_311190_) {
         super(p_311190_);
      }

      @Override
      public void writeUTF(String p_311566_) throws IOException {
         try {
            super.writeUTF(p_311566_);
         } catch (UTFDataFormatException utfdataformatexception) {
            Util.logAndPauseIfInIde("Failed to write NBT String", utfdataformatexception);
            super.writeUTF("");
         }
      }
   }
}
