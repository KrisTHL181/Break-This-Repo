package net.minecraft.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;

public class FileZipper implements Closeable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Path outputFile;
   private final Path tempFile;
   private final FileSystem fs;

   public FileZipper(final Path outputFile) {
      this.outputFile = outputFile;
      this.tempFile = outputFile.resolveSibling(outputFile.getFileName().toString() + "_tmp");

      try {
         this.fs = Util.ZIP_FILE_SYSTEM_PROVIDER.newFileSystem(this.tempFile, ImmutableMap.of("create", "true"));
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   public void add(final Path destinationRelativePath, final String content) {
      try {
         Path root = this.fs.getPath(File.separator);
         Path path = root.resolve(destinationRelativePath.toString());
         Files.createDirectories(path.getParent());
         Files.write(path, content.getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   public void add(final Path destinationRelativePath, final File file) {
      try {
         Path root = this.fs.getPath(File.separator);
         Path path = root.resolve(destinationRelativePath.toString());
         Files.createDirectories(path.getParent());
         Files.copy(file.toPath(), path);
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   public void add(final Path path) {
      try {
         Path root = this.fs.getPath(File.separator);
         if (Files.isRegularFile(path)) {
            Path targetFile = root.resolve(path.getParent().relativize(path).toString());
            Files.copy(targetFile, path);
         } else {
            try (Stream<Path> sourceFiles = Files.find(path, Integer.MAX_VALUE, (p, a) -> a.isRegularFile())) {
               for (Path sourceFile : sourceFiles.collect(Collectors.toList())) {
                  Path targetFile = root.resolve(path.relativize(sourceFile).toString());
                  Files.createDirectories(targetFile.getParent());
                  Files.copy(sourceFile, targetFile);
               }
            }
         }
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   @Override
   public void close() {
      try {
         this.fs.close();
         Files.move(this.tempFile, this.outputFile);
         LOGGER.info("Compressed to {}", this.outputFile);
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }
}
