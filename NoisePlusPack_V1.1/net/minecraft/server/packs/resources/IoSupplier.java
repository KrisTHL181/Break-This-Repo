package net.minecraft.server.packs.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@FunctionalInterface
public interface IoSupplier<T> {
   static IoSupplier<InputStream> create(Path p_248941_) {
      return () -> Files.newInputStream(p_248941_);
   }

   static IoSupplier<InputStream> create(ZipFile p_249624_, ZipEntry p_248688_) {
      return () -> p_249624_.getInputStream(p_248688_);
   }

   T get() throws IOException;
}
