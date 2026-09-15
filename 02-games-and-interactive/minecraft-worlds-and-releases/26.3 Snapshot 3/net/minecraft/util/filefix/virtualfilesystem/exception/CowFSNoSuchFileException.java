package net.minecraft.util.filefix.virtualfilesystem.exception;

import java.nio.file.NoSuchFileException;

public class CowFSNoSuchFileException extends NoSuchFileException {
   public CowFSNoSuchFileException(final String message) {
      super(message);
   }
}
