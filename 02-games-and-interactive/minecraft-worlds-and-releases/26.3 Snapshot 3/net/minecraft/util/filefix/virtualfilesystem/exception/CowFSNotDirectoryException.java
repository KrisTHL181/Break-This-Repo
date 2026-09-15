package net.minecraft.util.filefix.virtualfilesystem.exception;

import java.nio.file.NotDirectoryException;

public class CowFSNotDirectoryException extends NotDirectoryException {
   public CowFSNotDirectoryException(final String message) {
      super(message);
   }
}
