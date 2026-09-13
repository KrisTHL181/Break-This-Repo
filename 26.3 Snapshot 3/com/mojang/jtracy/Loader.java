package com.mojang.jtracy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

class Loader {
   private final String name;

   Loader() {
      String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
      String osArch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
      String prefix = "";
      String name = "jtracy-jni";
      String suffix = "";

      this.name = prefix + "jtracy-jni" + switch (osArch) {
         case "amd64", "x86_64", "x86-64" -> {
            if (osName.contains("win")) {
               yield "-windows.dll";
            } else if (!osName.contains("mac") && !osName.contains("darwin")) {
               if (!osName.contains("linux") && !osName.contains("unix")) {
                  throw new UnsatisfiedLinkError("Unsupported OS name: " + osName + " / " + osArch);
               }

               prefix = "lib";
               yield "-linux.so";
            } else {
               prefix = "lib";
               yield "-macos.dylib";
            }
         }
         case "aarch64" -> {
            if (!osName.contains("mac") && !osName.contains("darwin")) {
               throw new UnsatisfiedLinkError("Unsupported OS name: " + osName + " / " + osArch);
            }

            prefix = "lib";
            yield "-macos-arm64.dylib";
         }
         default -> throw new UnsatisfiedLinkError("Unsupported OS arch: " + osName + " / " + osArch);
      };
   }

   private Path createUnpackRoot() {
      Path path = Path.of(System.getProperty("java.io.tmpdir")).resolve("jtracy-" + UUID.randomUUID());

      try {
         Files.createDirectory(path);
      } catch (IOException var3) {
      }

      return path;
   }

   public void load() {
      Path root = this.createUnpackRoot();

      try {
         Path path = this.unpackLibrary(root);
         System.load(path.toAbsolutePath().toString());
      } finally {
         try {
            Files.walkFileTree(root, Set.of(), 1, new SimpleFileVisitor<Path>() {
               public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                  Files.delete(file);
                  return FileVisitResult.CONTINUE;
               }
            });
         } catch (IOException var11) {
         }

         try {
            Files.deleteIfExists(root);
         } catch (IOException var10) {
         }
      }
   }

   private Path unpackLibrary(Path root) {
      try (InputStream input = Loader.class.getClassLoader().getResourceAsStream(this.name)) {
         if (input == null) {
            throw new UnsatisfiedLinkError("Could not find jtracy natives at " + this.name);
         }

         Path path = Files.createTempFile(root, this.name, null);
         Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
         return path;
      } catch (IOException e) {
         throw new LinkageError("Can't unpack jtracy natives found at " + this.name + " to " + root, e);
      }
   }
}
