package com.mojang.realmsclient.client.worldupload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public class RealmsUploadWorldPacker {
   private static final long SIZE_LIMIT = 5368709120L;
   private static final String WORLD_FOLDER_NAME = "world";
   private final BooleanSupplier isCanceled;
   private final Path directoryToPack;

   public static File pack(final Path directoryToPack, final BooleanSupplier isCanceled) throws IOException {
      return new RealmsUploadWorldPacker(directoryToPack, isCanceled).tarGzipArchive();
   }

   private RealmsUploadWorldPacker(final Path directoryToPack, final BooleanSupplier isCanceled) {
      this.isCanceled = isCanceled;
      this.directoryToPack = directoryToPack;
   }

   private File tarGzipArchive() throws IOException {
      TarArchiveOutputStream tar = null;

      try {
         File file = File.createTempFile("realms-upload-file", ".tar.gz");
         tar = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
         tar.setLongFileMode(3);
         this.addFileToTarGz(tar, this.directoryToPack, "world", true);
         if (this.isCanceled.getAsBoolean()) {
            throw new RealmsUploadCanceledException();
         }

         tar.finish();
         this.verifyBelowSizeLimit(file.length());
         return file;
      } finally {
         if (tar != null) {
            tar.close();
         }
      }
   }

   private void addFileToTarGz(final TarArchiveOutputStream out, final Path path, final String base, final boolean root) throws IOException {
      if (this.isCanceled.getAsBoolean()) {
         throw new RealmsUploadCanceledException();
      }

      this.verifyBelowSizeLimit(out.getBytesWritten());
      File file = path.toFile();
      String entryName = root ? base : base + file.getName();
      TarArchiveEntry entry = new TarArchiveEntry(file, entryName);
      out.putArchiveEntry(entry);
      if (file.isFile()) {
         try (InputStream is = new FileInputStream(file)) {
            is.transferTo(out);
         }

         out.closeArchiveEntry();
      } else {
         out.closeArchiveEntry();
         File[] children = file.listFiles();
         if (children != null) {
            for (File child : children) {
               this.addFileToTarGz(out, child.toPath(), entryName + "/", false);
            }
         }
      }
   }

   private void verifyBelowSizeLimit(final long sizeInByte) {
      if (sizeInByte > 5368709120L) {
         throw new RealmsUploadTooLargeException(5368709120L);
      }
   }
}
