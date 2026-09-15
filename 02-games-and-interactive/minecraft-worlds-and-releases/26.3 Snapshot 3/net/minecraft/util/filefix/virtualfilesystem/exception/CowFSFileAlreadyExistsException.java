package net.minecraft.util.filefix.virtualfilesystem.exception;

import java.nio.file.FileAlreadyExistsException;

public class CowFSFileAlreadyExistsException extends FileAlreadyExistsException {
   public CowFSFileAlreadyExistsException(final String message) {
      super(message);
   }
}
