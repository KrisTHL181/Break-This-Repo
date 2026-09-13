package net.minecraft.util.filefix;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.FileUtil;
import org.slf4j.Logger;

public class FileFixUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String NAMESPACE_PATTERN = "([a-z0-9_.-]+)";

   public static void moveFile(final Path baseDirectory, final String from, final String to) throws IOException {
      Path fromAbsolute = baseDirectory.resolve(from);
      if (Files.exists(fromAbsolute)) {
         Path toAbsolute = baseDirectory.resolve(to);
         if (Files.exists(toAbsolute)) {
            LOGGER.warn("Target already exists, skipping move from {} to {}", from, to);
         } else {
            FileUtil.createDirectoriesSafe(toAbsolute.getParent());
            Files.move(fromAbsolute, toAbsolute, StandardCopyOption.COPY_ATTRIBUTES);
         }
      }
   }

   public static void deleteFileOrEmptyDirectory(final Path baseDirectory, final String file) throws IOException {
      Path toDelete = baseDirectory.resolve(file);
      if (Files.exists(toDelete)) {
         if (Files.isDirectory(toDelete)) {
            try {
               try (Stream<Path> paths = Files.list(toDelete)) {
                  List<Path> files = paths.toList();
                  if (files.size() == 1 && files.getFirst().getFileName().toString().equals(".DS_Store")) {
                     LOGGER.debug("Attempting to delete DS_Store at '{}'", toDelete);
                     if (!Files.deleteIfExists(files.getFirst())) {
                        LOGGER.warn("Failed to delete file '{}' at '{}'", files.getFirst(), toDelete);
                     }
                  }
               }

               List var12;
               try (Stream<Path> paths = Files.list(toDelete)) {
                  var12 = paths.toList();
               }

               if (!var12.isEmpty()) {
                  LOGGER.warn("Failed to delete directory '{}', as it's not empty. Content: {}", toDelete, var12);
                  return;
               }
            } catch (IOException e) {
               LOGGER.warn("Failed to delete directory '{}' because {}", toDelete, e.toString());
               return;
            }
         }

         Files.deleteIfExists(toDelete);
      }
   }
}
