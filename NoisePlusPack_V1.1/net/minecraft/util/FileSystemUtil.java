package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.slf4j.Logger;

public class FileSystemUtil {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static Path safeGetPath(URI p_396304_) throws IOException {
      try {
         return Paths.get(p_396304_);
      } catch (FileSystemNotFoundException var3) {
      } catch (Throwable throwable) {
         LOGGER.warn("Unable to get path for: {}", p_396304_, throwable);
      }

      try {
         FileSystems.newFileSystem(p_396304_, Collections.emptyMap());
      } catch (FileSystemAlreadyExistsException var2) {
      }

      return Paths.get(p_396304_);
   }
}
