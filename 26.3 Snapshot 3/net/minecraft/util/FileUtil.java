package net.minecraft.util;

import com.mojang.serialization.DataResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.SharedConstants;
import org.apache.commons.io.FilenameUtils;

public class FileUtil {
   private static final Pattern COPY_COUNTER_PATTERN = Pattern.compile("(<name>.*) \\((<count>\\d*)\\)", 66);
   private static final int MAX_FILE_NAME = 255;
   private static final Pattern RESERVED_WINDOWS_FILENAMES = Pattern.compile(".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?", 2);
   private static final Pattern STRICT_PATH_SEGMENT_CHECK = Pattern.compile("[-._a-z0-9]+");

   public static String sanitizeName(String baseName) {
      for (char replacer : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
         baseName = baseName.replace(replacer, '_');
      }

      return baseName.replaceAll("[./\"]", "_");
   }

   public static String findAvailableName(final Path baseDir, String baseName, final String suffix) throws IOException {
      baseName = sanitizeName(baseName);
      if (!isPathPartPortable(baseName)) {
         baseName = "_" + baseName + "_";
      }

      Matcher matcher = COPY_COUNTER_PATTERN.matcher(baseName);
      int count = 0;
      if (matcher.matches()) {
         baseName = matcher.group("name");
         count = Integer.parseInt(matcher.group("count"));
      }

      if (baseName.length() > 255 - suffix.length()) {
         baseName = baseName.substring(0, 255 - suffix.length());
      }

      while (true) {
         String nameToTest = baseName;
         if (count != 0) {
            String countSuffix = " (" + count + ")";
            int length = 255 - countSuffix.length();
            if (nameToTest.length() > length) {
               nameToTest = nameToTest.substring(0, length);
            }

            nameToTest = nameToTest + countSuffix;
         }

         nameToTest = nameToTest + suffix;
         Path fullPath = baseDir.resolve(nameToTest);

         try {
            Path created = Files.createDirectory(fullPath);
            Files.deleteIfExists(created);
            return baseDir.relativize(created).toString();
         } catch (FileAlreadyExistsException e) {
            count++;
         }
      }
   }

   public static boolean isPathPortable(final Path path) {
      for (Path part : path) {
         if (!isPathPartPortable(part.toString())) {
            return false;
         }
      }

      return true;
   }

   public static boolean isPathPartPortable(final String name) {
      return !RESERVED_WINDOWS_FILENAMES.matcher(name).matches();
   }

   public static String getFullResourcePath(final String filename) {
      return FilenameUtils.getFullPath(filename).replace(File.separator, "/");
   }

   public static String normalizeResourcePath(final String filename) {
      return FilenameUtils.normalize(filename).replace(File.separator, "/");
   }

   public static DataResult<List<String>> decomposePath(final String path) {
      int segmentEnd = path.indexOf(47);
      if (segmentEnd == -1) {
         return switch (path) {
            case "", ".", ".." -> DataResult.error(() -> "Invalid path '" + path + "'");
            default -> !containsAllowedCharactersOnly(path) ? DataResult.error(() -> "Invalid path '" + path + "'") : DataResult.success(List.of(path));
         };
      } else {
         List<String> result = new ArrayList<>();
         int segmentStart = 0;
         boolean lastSegment = false;

         while (true) {
            String segment = path.substring(segmentStart, segmentEnd);
            switch (segment) {
               case "":
               case ".":
               case "..":
                  return DataResult.error(() -> "Invalid segment '" + segment + "' in path '" + path + "'");
            }

            if (!containsAllowedCharactersOnly(segment)) {
               return DataResult.error(() -> "Invalid segment '" + segment + "' in path '" + path + "'");
            }

            result.add(segment);
            if (lastSegment) {
               return DataResult.success(result);
            }

            segmentStart = segmentEnd + 1;
            segmentEnd = path.indexOf(47, segmentStart);
            if (segmentEnd == -1) {
               segmentEnd = path.length();
               lastSegment = true;
            }
         }
      }
   }

   public static Path resolvePath(final Path root, final List<String> segments) {
      int size = segments.size();

      return switch (size) {
         case 0 -> root;
         case 1 -> root.resolve(segments.get(0));
         default -> {
            String[] rest = new String[size - 1];

            for (int i = 1; i < size; i++) {
               rest[i - 1] = segments.get(i);
            }

            yield root.resolve(root.getFileSystem().getPath(segments.get(0), rest));
         }
      };
   }

   private static boolean containsAllowedCharactersOnly(final String segment) {
      return STRICT_PATH_SEGMENT_CHECK.matcher(segment).matches();
   }

   public static boolean isValidPathSegment(final String segment) {
      return !segment.equals("..") && !segment.equals(".") && containsAllowedCharactersOnly(segment);
   }

   public static void validatePath(final String... path) {
      if (path.length == 0) {
         throw new IllegalArgumentException("Path must have at least one element");
      }

      for (String segment : path) {
         if (!isValidPathSegment(segment)) {
            throw new IllegalArgumentException("Illegal segment " + segment + " in path " + Arrays.toString(path));
         }
      }
   }

   public static void createDirectoriesSafe(final Path dir) throws IOException {
      Files.createDirectories(Files.exists(dir) ? dir.toRealPath() : dir);
   }

   public static boolean isEmptyPath(final Path path) {
      return path.getNameCount() == 1 && path.getFileName().toString().isEmpty();
   }
}
