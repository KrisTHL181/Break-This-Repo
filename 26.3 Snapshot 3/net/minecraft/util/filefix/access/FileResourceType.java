package net.minecraft.util.filefix.access;

import java.nio.file.Path;

public class FileResourceType<T extends AutoCloseable> {
   private final FileResourceType.AccessFactory<T> factory;

   public FileResourceType(final FileResourceType.AccessFactory<T> factory) {
      this.factory = factory;
   }

   public T create(final Path path, final int dataVersion) {
      return this.factory.create(path, dataVersion);
   }

   @FunctionalInterface
   public interface AccessFactory<T> {
      T create(Path path, int dataVersion);
   }
}
