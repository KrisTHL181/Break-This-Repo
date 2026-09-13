package net.minecraft.util;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import org.jspecify.annotations.Nullable;

public abstract class DummyFileAttributes implements BasicFileAttributes {
   public static final BasicFileAttributes DIRECTORY = new DummyFileAttributes() {
      @Override
      public boolean isRegularFile() {
         return false;
      }

      @Override
      public boolean isDirectory() {
         return true;
      }
   };
   public static final BasicFileAttributes FILE = new DummyFileAttributes() {
      @Override
      public boolean isRegularFile() {
         return true;
      }

      @Override
      public boolean isDirectory() {
         return false;
      }
   };
   private static final FileTime EPOCH = FileTime.fromMillis(0L);

   @Override
   public FileTime lastModifiedTime() {
      return EPOCH;
   }

   @Override
   public FileTime lastAccessTime() {
      return EPOCH;
   }

   @Override
   public FileTime creationTime() {
      return EPOCH;
   }

   @Override
   public boolean isSymbolicLink() {
      return false;
   }

   @Override
   public boolean isOther() {
      return false;
   }

   @Override
   public long size() {
      return 0L;
   }

   @Override
   public @Nullable Object fileKey() {
      return null;
   }
}
