package net.minecraft.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class DirectoryLock implements AutoCloseable {
   public static final String LOCK_FILE = "session.lock";
   private final FileChannel lockFile;
   private final FileLock lock;
   private static final ByteBuffer DUMMY;

   public static DirectoryLock create(Path p_13641_) throws IOException {
      Path path = p_13641_.resolve("session.lock");
      FileUtil.createDirectoriesSafe(p_13641_);
      FileChannel filechannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

      try {
         filechannel.write(DUMMY.duplicate());
         filechannel.force(true);
         FileLock filelock = filechannel.tryLock();
         if (filelock == null) {
            throw DirectoryLock.LockException.alreadyLocked(path);
         } else {
            return new DirectoryLock(filechannel, filelock);
         }
      } catch (IOException ioexception1) {
         try {
            filechannel.close();
         } catch (IOException ioexception) {
            ioexception1.addSuppressed(ioexception);
         }

         throw ioexception1;
      }
   }

   private DirectoryLock(FileChannel p_13637_, FileLock p_13638_) {
      this.lockFile = p_13637_;
      this.lock = p_13638_;
   }

   @Override
   public void close() throws IOException {
      try {
         if (this.lock.isValid()) {
            this.lock.release();
         }
      } finally {
         if (this.lockFile.isOpen()) {
            this.lockFile.close();
         }
      }
   }

   public boolean isValid() {
      return this.lock.isValid();
   }

   public static boolean isLocked(Path p_13643_) throws IOException {
      Path path = p_13643_.resolve("session.lock");

      try (
         FileChannel filechannel = FileChannel.open(path, StandardOpenOption.WRITE);
         FileLock filelock = filechannel.tryLock();
      ) {
         return filelock == null;
      } catch (AccessDeniedException accessdeniedexception) {
         return true;
      } catch (NoSuchFileException nosuchfileexception) {
         return false;
      }
   }

   static {
      byte[] abyte = "☃".getBytes(StandardCharsets.UTF_8);
      DUMMY = ByteBuffer.allocateDirect(abyte.length);
      DUMMY.put(abyte);
      DUMMY.flip();
   }

   public static class LockException extends IOException {
      private LockException(Path p_13646_, String p_13647_) {
         super(p_13646_.toAbsolutePath() + ": " + p_13647_);
      }

      public static DirectoryLock.LockException alreadyLocked(Path p_13649_) {
         return new DirectoryLock.LockException(p_13649_, "already locked (possibly by other Minecraft instance?)");
      }
   }
}
