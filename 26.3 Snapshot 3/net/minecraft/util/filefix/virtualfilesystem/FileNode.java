package net.minecraft.util.filefix.virtualfilesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class FileNode extends Node {
   private Path storagePath;
   boolean isCopy;

   FileNode(final CopyOnWriteFSPath path, final Path storagePath, final boolean isCopy) {
      super(path);
      this.storagePath = storagePath;
      this.isCopy = isCopy;
   }

   public void ensureCopy() throws IOException {
      if (!this.isCopy) {
         Path tempFile = this.path.getFileSystem().createTemporaryFilePath();
         Files.copy(this.storagePath, tempFile, StandardCopyOption.COPY_ATTRIBUTES);
         this.storagePath = tempFile;
         this.isCopy = true;
      }
   }

   public void deleteCopy() throws IOException {
      if (this.isCopy) {
         Files.deleteIfExists(this.storagePath);
      }
   }

   public Path storagePath() {
      return this.storagePath;
   }
}
