package com.mojang.realmsclient.client.worldupload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.zip.GZIPOutputStream;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

@OnlyIn(Dist.CLIENT)
public class RealmsUploadWorldPacker {
   private static final long SIZE_LIMIT = 5368709120L;
   private static final String WORLD_FOLDER_NAME = "world";
   private final BooleanSupplier isCanceled;
   private final Path directoryToPack;

   public static File pack(Path p_363835_, BooleanSupplier p_361418_) throws IOException {
      return new RealmsUploadWorldPacker(p_363835_, p_361418_).tarGzipArchive();
   }

   private RealmsUploadWorldPacker(Path p_366435_, BooleanSupplier p_361265_) {
      this.isCanceled = p_361265_;
      this.directoryToPack = p_366435_;
   }

   private File tarGzipArchive() throws IOException {
      TarArchiveOutputStream tararchiveoutputstream = null;

      File file2;
      try {
         File file1 = File.createTempFile("realms-upload-file", ".tar.gz");
         tararchiveoutputstream = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(file1)));
         tararchiveoutputstream.setLongFileMode(3);
         this.addFileToTarGz(tararchiveoutputstream, this.directoryToPack, "world", true);
         if (this.isCanceled.getAsBoolean()) {
            throw new RealmsUploadCanceledException();
         }

         tararchiveoutputstream.finish();
         this.verifyBelowSizeLimit(file1.length());
         file2 = file1;
      } finally {
         if (tararchiveoutputstream != null) {
            tararchiveoutputstream.close();
         }
      }

      return file2;
   }

   private void addFileToTarGz(TarArchiveOutputStream p_369594_, Path p_362919_, String p_363494_, boolean p_368866_) throws IOException {
      if (this.isCanceled.getAsBoolean()) {
         throw new RealmsUploadCanceledException();
      }

      this.verifyBelowSizeLimit(p_369594_.getBytesWritten());
      File file1 = p_362919_.toFile();
      String s = p_368866_ ? p_363494_ : p_363494_ + file1.getName();
      TarArchiveEntry tararchiveentry = new TarArchiveEntry(file1, s);
      p_369594_.putArchiveEntry(tararchiveentry);
      if (file1.isFile()) {
         try (InputStream inputstream = new FileInputStream(file1)) {
            inputstream.transferTo(p_369594_);
         }

         p_369594_.closeArchiveEntry();
      } else {
         p_369594_.closeArchiveEntry();
         File[] afile = file1.listFiles();
         if (afile != null) {
            for (File file2 : afile) {
               this.addFileToTarGz(p_369594_, file2.toPath(), s + "/", false);
            }
         }
      }
   }

   private void verifyBelowSizeLimit(long p_365035_) {
      if (p_365035_ > 5368709120L) {
         throw new RealmsUploadTooLargeException(5368709120L);
      }
   }
}
