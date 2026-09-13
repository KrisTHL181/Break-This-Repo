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

   public static Path safeGetPath(final URI probeUri) throws IOException {
      try {
         return Paths.get(probeUri);
      } catch (FileSystemNotFoundException var3) {
      } catch (Throwable t) {
         LOGGER.warn("Unable to get path for: {}", probeUri, t);
      }

      try {
         FileSystems.newFileSystem(probeUri, Collections.emptyMap());
      } catch (FileSystemAlreadyExistsException var2) {
      }

      return Paths.get(probeUri);
   }
}
