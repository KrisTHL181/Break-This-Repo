package net.minecraft.util.filefix.virtualfilesystem.exception;

import java.nio.file.FileSystemException;

public class CowFSFileSystemException extends FileSystemException {
   public CowFSFileSystemException(final String message) {
      super(message);
   }
}
