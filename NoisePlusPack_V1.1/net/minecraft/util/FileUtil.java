package net.minecraft.util;

import com.mojang.serialization.DataResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

   public static String sanitizeName(String p_453893_) {
      for (char c0 : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
         p_453893_ = p_453893_.replace(c0, '_');
      }

      return p_453893_.replaceAll("[./\"]", "_");
   }

   public static String findAvailableName(Path p_459453_, String p_452613_, String p_455848_) throws IOException {
      p_452613_ = sanitizeName(p_452613_);
      if (!isPathPartPortable(p_452613_)) {
         p_452613_ = "_" + p_452613_ + "_";
      }

      Matcher matcher = COPY_COUNTER_PATTERN.matcher(p_452613_);
      int i = 0;
      if (matcher.matches()) {
         p_452613_ = matcher.group("name");
         i = Integer.parseInt(matcher.group("count"));
      }

      if (p_452613_.length() > 255 - p_455848_.length()) {
         p_452613_ = p_452613_.substring(0, 255 - p_455848_.length());
      }

      while (true) {
         String s = p_452613_;
         if (i != 0) {
            String s1 = " (" + i + ")";
            int j = 255 - s1.length();
            if (p_452613_.length() > j) {
               s = p_452613_.substring(0, j);
            }

            s = s + s1;
         }

         s = s + p_455848_;
         Path path = p_459453_.resolve(s);

         try {
            Path path1 = Files.createDirectory(path);
            Files.deleteIfExists(path1);
            return p_459453_.relativize(path1).toString();
         } catch (FileAlreadyExistsException filealreadyexistsexception) {
            i++;
         }
      }
   }

   public static boolean isPathNormalized(Path p_452834_) {
      Path path = p_452834_.normalize();
      return path.equals(p_452834_);
   }

   public static boolean isPathPortable(Path p_452174_) {
      for (Path path : p_452174_) {
         if (!isPathPartPortable(path.toString())) {
            return false;
         }
      }

      return true;
   }

   public static boolean isPathPartPortable(String p_460763_) {
      return !RESERVED_WINDOWS_FILENAMES.matcher(p_460763_).matches();
   }

   public static Path createPathToResource(Path p_455653_, String p_456639_, String p_452686_) {
      String s = p_456639_ + p_452686_;
      Path path = Paths.get(s);
      if (path.endsWith(p_452686_)) {
         throw new InvalidPathException(s, "empty resource name");
      } else {
         return p_455653_.resolve(path);
      }
   }

   public static String getFullResourcePath(String p_456803_) {
      return FilenameUtils.getFullPath(p_456803_).replace(File.separator, "/");
   }

   public static String normalizeResourcePath(String p_450259_) {
      return FilenameUtils.normalize(p_450259_).replace(File.separator, "/");
   }

   public static DataResult<List<String>> decomposePath(String p_450680_) {
      int i = p_450680_.indexOf(47);
      if (i == -1) {
         return switch (p_450680_) {
            case "", ".", ".." -> DataResult.error(() -> "Invalid path '" + p_450680_ + "'");
            default -> !containsAllowedCharactersOnly(p_450680_)
               ? DataResult.error(() -> "Invalid path '" + p_450680_ + "'")
               : DataResult.success(List.of(p_450680_));
         };
      } else {
         List<String> list = new ArrayList<>();
         int j = 0;
         boolean flag = false;

         while (true) {
            String s = p_450680_.substring(j, i);
            switch (s) {
               case "":
               case ".":
               case "..":
                  return DataResult.error(() -> "Invalid segment '" + s + "' in path '" + p_450680_ + "'");
            }

            if (!containsAllowedCharactersOnly(s)) {
               return DataResult.error(() -> "Invalid segment '" + s + "' in path '" + p_450680_ + "'");
            }

            list.add(s);
            if (flag) {
               return DataResult.success(list);
            }

            j = i + 1;
            i = p_450680_.indexOf(47, j);
            if (i == -1) {
               i = p_450680_.length();
               flag = true;
            }
         }
      }
   }

   public static Path resolvePath(Path p_460552_, List<String> p_452490_) {
      int i = p_452490_.size();

      return switch (i) {
         case 0 -> p_460552_;
         case 1 -> p_460552_.resolve(p_452490_.get(0));
         default -> {
            String[] astring = new String[i - 1];

            for (int j = 1; j < i; j++) {
               astring[j - 1] = p_452490_.get(j);
            }

            yield p_460552_.resolve(p_460552_.getFileSystem().getPath(p_452490_.get(0), astring));
         }
      };
   }

   private static boolean containsAllowedCharactersOnly(String p_456664_) {
      return STRICT_PATH_SEGMENT_CHECK.matcher(p_456664_).matches();
   }

   public static boolean isValidPathSegment(String p_456937_) {
      return !p_456937_.equals("..") && !p_456937_.equals(".") && containsAllowedCharactersOnly(p_456937_);
   }

   public static void validatePath(String... p_453784_) {
      if (p_453784_.length == 0) {
         throw new IllegalArgumentException("Path must have at least one element");
      }

      for (String s : p_453784_) {
         if (!isValidPathSegment(s)) {
            throw new IllegalArgumentException("Illegal segment " + s + " in path " + Arrays.toString(p_453784_));
         }
      }
   }

   public static void createDirectoriesSafe(Path p_454579_) throws IOException {
      Files.createDirectories(Files.exists(p_454579_) ? p_454579_.toRealPath() : p_454579_);
   }
}
